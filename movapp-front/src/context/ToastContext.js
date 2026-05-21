import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { _registerToastDispatcher } from '../services/toast';
import Toast from '../components/Toast';
import '../components/Toast.css';

const ToastContext = createContext(null);

const DURACAO_PADRAO = 4500;
const JANELA_DEDUP_MS = 2500;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);
  const ultimosRef = useRef(new Map());

  const remover = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const push = useCallback((item) => {
    if (!item?.mensagem) return;
    const chave = `${item.tipo}:${item.mensagem}`;
    const agora = Date.now();
    const ultimo = ultimosRef.current.get(chave);
    if (ultimo && agora - ultimo < JANELA_DEDUP_MS) return;
    ultimosRef.current.set(chave, agora);

    const id = ++idRef.current;
    const duracao = item.duracao ?? DURACAO_PADRAO;
    setToasts(prev => [...prev, { id, tipo: item.tipo || 'info', mensagem: item.mensagem }]);

    if (duracao > 0) {
      setTimeout(() => remover(id), duracao);
    }
  }, [remover]);

  useEffect(() => {
    _registerToastDispatcher(push);
    return () => _registerToastDispatcher(() => {});
  }, [push]);

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toast-container" aria-live="polite" aria-atomic="false">
        {toasts.map(t => (
          <Toast key={t.id} tipo={t.tipo} mensagem={t.mensagem} onFechar={() => remover(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast precisa estar dentro de <ToastProvider>');
  return ctx;
}
