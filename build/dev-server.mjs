import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { build } from 'vite';

// `pnpm dev`: perun-core's page and bundle, this module's watch build where the shell fetches plugins from, and
// everything else proxied to the backend named in backend/www/config.js.

const root = path.resolve(import.meta.dirname, '..');
const www = path.join(root, 'backend/www');
const require = createRequire(import.meta.url);

const manifestNear = (dir) => {
  let current = dir;
  for (;;) {
    const candidate = path.join(current, 'package.json');
    if (fs.existsSync(candidate)) return candidate;
    const parent = path.dirname(current);
    if (parent === current) return null;
    current = parent;
  }
};

// A sibling checkout's build wins over the installed package: the installed one lacks the chunks it lazy-loads,
// and a local rebuild then needs no reinstall.
const vendorBundle = (name, from, sibling) => {
  const local = sibling && path.resolve(root, '..', sibling);

  let installed = null;
  try {
    const manifest = require.resolve(`${name}/package.json`, from ? { paths: [from] } : undefined);
    installed = path.join(path.dirname(manifest), require(manifest).main);
  } catch {
    installed = null;
  }

  const localManifest = local && manifestNear(local);
  const file = installed
    ? path.basename(installed)
    : localManifest && path.basename(require(localManifest).main);

  if (local && file && fs.existsSync(path.join(local, file))) {
    return { name, dir: local, file, source: 'checkout' };
  }

  if (installed) {
    return { name, dir: path.dirname(installed), file, source: 'installed' };
  }

  throw new Error(
    `vite: ${name} is neither installed nor built in a sibling checkout. ` +
    `Run \`pnpm install\`, or build it in ../${sibling.split('/')[0]}.`
  );
};

// Live assignments only, so a commented-out host above the live one is not taken for it.
const serverLine = () => /^([ \t]*)window\.server\s*=\s*['"]([^'"]+)['"]/gm;

const readApiUrl = () => {
  const source = fs.readFileSync(path.join(www, 'config.js'), 'utf8');
  const found = [...source.matchAll(serverLine())];
  return found.length ? new URL(found[found.length - 1][2]) : null;
};

const TYPES = {
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.html': 'text/html; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
};

// no-store: the shell loads plugin scripts without a version in the URL, so a cached copy would hide a rebuild.
const serveDir = (prefix, dir) => (req, res, next) => {
  if (req.method !== 'GET' && req.method !== 'HEAD') return next();
  const { pathname } = new URL(req.url, 'http://localhost');
  if (!pathname.startsWith(prefix)) return next();

  let relative;
  try {
    relative = decodeURIComponent(pathname.slice(prefix.length));
  } catch {
    return next();
  }
  const file = path.join(dir, relative);
  if (file !== dir && !file.startsWith(dir + path.sep)) return next();

  fs.stat(file, (error, stat) => {
    if (error || !stat.isFile()) return next();
    res.setHeader('Content-Type', TYPES[path.extname(file)] ?? 'application/octet-stream');
    res.setHeader('Content-Length', stat.size);
    res.setHeader('Cache-Control', 'no-store');
    if (req.method === 'HEAD') return res.end();
    fs.createReadStream(file).pipe(res);
  });
};

/** @param {{ name: string, file: string }} self  The module's context path and the file served under it. */
export function devServer(self) {
  // Resolved when the server starts: `vite build` reads this config on CI, where there is no config.js.
  const resolve = () => {
    const perunCore = vendorBundle('perun-core', null, 'perun-core/www');
    const vendors = [perunCore];
    const owned = [...vendors, self].map(v => `/${v.name}/`);
    return { perunCore, vendors, owned, api: readApiUrl() };
  };
  let site;
  // Vite's client, and the env.mjs it imports from under /node_modules/ with pnpm.
  const viteOwned = ['/@', '/node_modules/'];

  return {
    name: `${self.name}:dev-server`,
    apply: 'serve',

    config: () => {
      site = resolve();
      const { api, owned } = site;
      return {
        appType: 'custom',
        publicDir: false,
        server: {
          port: 8080,
          // Everything not served here is the backend's, including the assets whose location only it knows.
          proxy: api ? {
            '^/': {
              target: api.origin,
              changeOrigin: true,
              secure: false,
              bypass: (req) => {
                const { pathname } = new URL(req.url, 'http://localhost');
                return [...owned, ...viteOwned].some(prefix => pathname.startsWith(prefix))
                  ? req.url
                  : undefined;
              },
            },
          } : undefined,
        },
      };
    },

    async configureServer(server) {
      const { perunCore, vendors, api } = site;
      const watcher = await build({
        configFile: server.config.configFile,
        mode: 'development',
        build: { watch: {} },
      });

      let ready;
      const built = new Promise(resolve => { ready = resolve; });
      watcher.on('event', (event) => {
        if (event.code === 'BUNDLE_END') {
          event.result.close();
          server.ws.send({ type: 'full-reload', path: '*' });
          ready();
        } else if (event.code === 'ERROR') {
          ready();
        }
      });
      server.httpServer?.once('close', () => watcher.close());
      await built;

      if (api) {
        // Same origin in dev, through the proxy.
        server.middlewares.use((req, res, next) => {
          if (req.method !== 'GET' || new URL(req.url, 'http://localhost').pathname !== '/config.js') {
            return next();
          }
          const source = fs.readFileSync(path.join(www, 'config.js'), 'utf8');
          res.setHeader('Content-Type', TYPES['.js']);
          res.setHeader('Cache-Control', 'no-store');
          res.end(source.replace(serverLine(), (_match, indent) => `${indent}window.server = '${api.pathname}'`));
        });

        // The shell loads its plugins itself; the page's own script tag for this bundle would evaluate it twice.
        server.middlewares.use(async (req, res, next) => {
          const { pathname } = new URL(req.url, 'http://localhost');
          if (req.method !== 'GET' || (pathname !== '/' && pathname !== '/index.html')) return next();
          const html = fs.readFileSync(path.join(www, 'index.html'), 'utf8').replace(
            `<script src="${self.file}"></script>`,
            `<script src="/${perunCore.name}/${perunCore.file}"></script>`
          );
          res.setHeader('Content-Type', TYPES['.html']);
          res.setHeader('Cache-Control', 'no-store');
          res.end(await server.transformIndexHtml(req.url, html));
        });
      }

      for (const v of vendors) server.middlewares.use(serveDir(`/${v.name}/`, v.dir));
      // Where the shell fetches this plugin; otherwise it would run the deployed copy.
      server.middlewares.use(serveDir(`/${self.name}/`, www));
      if (!api) server.middlewares.use(serveDir('/', www));
    },
  };
}
