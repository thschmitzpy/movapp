const ICONES = {
  success: '✓',
  error:   '!',
  warning: '⚠',
  info:    'i',
};

const ROTULOS = {
  success: 'Sucesso',
  error:   'Erro',
  warning: 'Atenção',
  info:    'Aviso',
};

function Toast({ tipo = 'info', mensagem, onFechar }) {
  return (
    <div className={`toast toast-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      <span className="toast-icone" aria-hidden="true">{ICONES[tipo]}</span>
      <div className="toast-corpo">
        <strong className="toast-titulo">{ROTULOS[tipo]}</strong>
        <span className="toast-mensagem">{mensagem}</span>
      </div>
      <button
        type="button"
        className="toast-fechar"
        onClick={onFechar}
        aria-label="Fechar notificação"
      >
        ×
      </button>
    </div>
  );
}

export default Toast;
