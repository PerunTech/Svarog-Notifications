import { defineConfig, transformWithOxc } from 'vite';
import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { devServer } from './build/dev-server.mjs';

// The shell fetches /<name>/<file> and reads the routes off window[<name>], so both come from package.json.
const pkg = JSON.parse(fs.readFileSync(path.resolve(import.meta.dirname, 'package.json'), 'utf8'));
const self = { name: pkg.name, file: path.basename(pkg.main) };

const frontend = path.resolve(import.meta.dirname, 'frontend') + path.sep;

// The sources are .js with JSX in them. Classic runtime, so React comes from perun-core.
const jsxInJs = () => ({
  name: `${self.name}:jsx-in-js`,
  enforce: 'pre',
  transform(code, id) {
    if (!id.startsWith(frontend) || !id.endsWith('.js')) return null;
    return transformWithOxc(code, id, { lang: 'jsx', jsx: { runtime: 'classic' } });
  },
});

// react-paginate requires 'react'. It gets perun-core's: a second copy would draw elements perun-core's React
// cannot render once it moves past the bundled one. An ES module, since a require of perun-core would stay a
// require in the browser; it names every export of the React perun-core is built with.
const REACT = '\0react-from-perun-core';
const reactExports = Object.keys(createRequire(import.meta.resolve('perun-core/package.json'))('react'));
const reactFromPerunCore = () => ({
  name: `${self.name}:react-from-perun-core`,
  enforce: 'pre',
  resolveId: (id) => (id === 'react' ? REACT : null),
  load: (id) => (id === REACT
    ? `import { React } from 'perun-core';\nexport default React;\nexport const { ${reactExports.join(', ')} } = React;`
    : null),
});

// Relative to the repository, so the map's sources stay under /<name>/ and do not meet another module's
// frontend/index.js in DevTools.
const sourcePath = (source, map) =>
  path.relative(import.meta.dirname, path.resolve(path.dirname(map), source)).split(path.sep).join('/');

export default defineConfig(({ mode }) => ({
  publicDir: false,
  plugins: [jsxInJs(), reactFromPerunCore(), devServer(self)],
  build: {
    outDir: 'backend/www',
    // backend/www also holds index.html and config.js.
    emptyOutDir: false,
    target: 'es2020',
    sourcemap: true,
    minify: mode === 'production',
    lib: {
      // The development entry also registers the plugin with perun-core's plugin manager.
      entry: mode === 'production' ? 'frontend/index.js' : 'frontend/client.js',
      name: self.name,
      formats: ['umd'],
      fileName: () => self.file,
    },
    rollupOptions: {
      external: ['perun-core'],
      output: {
        globals: { 'perun-core': 'perun-core' },
        sourcemapPathTransform: sourcePath,
      },
    },
  },
}));
