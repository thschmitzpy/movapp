 import { render, screen } from '@testing-library/react';
  import Login from './components/Login';

  test('renderiza a tela de login', () => {
    render(<Login onLogin={() => {}} />);
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
  });
