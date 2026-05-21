let dispatcher = null;

export function _registerToastDispatcher(fn) {
  dispatcher = fn;
}

function emit(tipo, mensagem, opcoes) {
  if (!dispatcher || !mensagem) return;
  dispatcher({ tipo, mensagem, ...opcoes });
}

export const toast = {
  success: (msg, opts) => emit('success', msg, opts),
  error:   (msg, opts) => emit('error', msg, opts),
  warning: (msg, opts) => emit('warning', msg, opts),
  info:    (msg, opts) => emit('info', msg, opts),
};
