import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist', 'src/routeTree.gen.js']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // The source UI library uses forwardRef, which the React 19 compiler rule misreads after transpilation.
      'react-hooks/refs': 'off',
      'react-hooks/purity': 'off',
    },
  },
  {
    files: ['src/components/lab/StatCard.js', 'src/components/ui/carousel.js'],
    rules: {
      'react-hooks/set-state-in-effect': 'off',
    },
  },
])
