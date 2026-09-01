import { CssBaseline, ThemeProvider } from '@mui/material';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import App from './App';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import theme from './theme';
import './index.css';

const contenedor = document.getElementById('root');
if (!contenedor) {
  throw new Error('No se encontro el elemento raiz de la aplicacion');
}

createRoot(contenedor).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      {/* Normaliza los estilos del navegador antes de aplicar los propios. */}
      <CssBaseline />
      <BrowserRouter>
        <NotificationProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </NotificationProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
);
