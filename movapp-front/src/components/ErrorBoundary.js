import React from 'react';
import { useFocusTrap } from '../hooks/useFocusTrap';
import './ErrorBoundary.css';

function ErrorFallback({ error, onReset, onReload }) {
  const cardRef = useFocusTrap(true);
  const detalhe = error?.message;

  return (
    <div className="errboundary-wrapper" role="alert" aria-live="assertive">
      <div
        ref={cardRef}
        className="errboundary-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="errboundary-titulo"
      >
        <div className="errboundary-icone" aria-hidden="true">⚠️</div>
        <h2 id="errboundary-titulo">Algo deu errado</h2>
        <p>
          Ocorreu um erro inesperado e a tela não pôde ser exibida.
          Você pode tentar recarregar ou voltar à tela anterior.
        </p>
        {detalhe && process.env.NODE_ENV !== 'production' && (
          <pre className="errboundary-detalhe">{detalhe}</pre>
        )}
        <div className="errboundary-acoes">
          <button className="btn-secundario" onClick={onReset}>
            Tentar novamente
          </button>
          <button className="btn-primario" onClick={onReload}>
            Recarregar página
          </button>
        </div>
      </div>
    </div>
  );
}

class ErrorBoundary extends React.Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    if (process.env.NODE_ENV !== 'production') {
      console.error('[ErrorBoundary]', error, info?.componentStack);
    }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <ErrorFallback
        error={this.state.error}
        onReset={this.handleReset}
        onReload={this.handleReload}
      />
    );
  }
}

export default ErrorBoundary;
