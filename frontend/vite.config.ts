import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],

  resolve: {
    alias: {
      // Importaciones absolutas desde src, para no encadenar ../../..
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    port: 5173,
    // Necesario dentro de un contenedor: sin esto solo escucha en localhost
    // y el puerto publicado no llega a ningun lado.
    host: true,
    proxy: {
      // En desarrollo el frontend corre en 5173 y el backend en 8080. El proxy
      // hace que ambos se vean como el mismo origen y no haga falta CORS.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: false,
    rollupOptions: {
      output: {
        // Se separan las bibliotecas grandes para que el navegador las guarde
        // una vez y no vuelva a bajarlas en cada despliegue.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom'],
          // Material UI y sus iconos en archivos separados: cada uno cambia con distinta frecuencia
          // y asi ninguno pasa de 500 kB.
          mui: ['@mui/material'],
          'mui-iconos': ['@mui/icons-material'],
        },
      },
    },
  },
});
