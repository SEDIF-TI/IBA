/**
 * Configuración de ESLint (formato plano de ESLint 9).
 * Área: Núcleo compartido (frontend). Reglas: las recomendadas de JavaScript y TypeScript, las
 * reglas de los hooks de React (dependencias de useEffect y orden de llamada) y la de recarga en
 * caliente de Vite. El estándar del equipo pide cero errores y cero advertencias.
 */
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'node_modules'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      // Los contextos exportan su Provider junto con su hook de acceso; es el patrón del proyecto.
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true, allowExportNames: ['useAuth', 'useNotificacion'] },
      ],
      'no-console': ['error', { allow: ['warn', 'error'] }],
    },
  },
);
