import { useEffect, useRef } from 'react';

const SELETOR_FOCAVEL = [
  'button:not(:disabled)',
  '[href]',
  'input:not(:disabled)',
  'select:not(:disabled)',
  'textarea:not(:disabled)',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

// Mantém o foco do teclado dentro do container enquanto `ativo` for true.
// Move o foco para o primeiro elemento focável ao ativar e restaura
// o foco anterior quando desativa. `Esc` chama `onEscape` se fornecido.
export function useFocusTrap(ativo, onEscape) {
  const containerRef = useRef(null);
  const onEscapeRef = useRef(onEscape);

  useEffect(() => { onEscapeRef.current = onEscape; }, [onEscape]);

  useEffect(() => {
    if (!ativo) return;

    const container = containerRef.current;
    if (!container) return;

    const focoAnterior = document.activeElement;

    const focaveis = () => Array.from(container.querySelectorAll(SELETOR_FOCAVEL));
    const lista = focaveis();
    if (lista.length > 0) {
      lista[0].focus();
    } else {
      // Garante foco no container para que Esc/Tab cheguem ao listener.
      container.setAttribute('tabindex', '-1');
      container.focus();
    }

    function handleKeyDown(e) {
      if (e.key === 'Escape') {
        if (onEscapeRef.current) {
          e.preventDefault();
          onEscapeRef.current();
        }
        return;
      }
      if (e.key !== 'Tab') return;

      const lista = focaveis();
      if (lista.length === 0) {
        e.preventDefault();
        return;
      }
      const primeiro = lista[0];
      const ultimo = lista[lista.length - 1];
      const ativo = document.activeElement;

      if (e.shiftKey && (ativo === primeiro || !container.contains(ativo))) {
        e.preventDefault();
        ultimo.focus();
      } else if (!e.shiftKey && (ativo === ultimo || !container.contains(ativo))) {
        e.preventDefault();
        primeiro.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      if (focoAnterior && typeof focoAnterior.focus === 'function') {
        focoAnterior.focus();
      }
    };
  }, [ativo]);

  return containerRef;
}
