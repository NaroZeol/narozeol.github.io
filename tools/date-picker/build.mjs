import { build } from 'esbuild';
import postcss from 'postcss';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const directory = path.dirname(fileURLToPath(import.meta.url));
const output = path.resolve(directory, '../../assets/vendor/date-picker');
await mkdir(output, { recursive: true });
const result = await build({
  absWorkingDir: directory,
  entryPoints: ['date-picker.jsx'],
  outdir: output,
  bundle: true,
  format: 'esm',
  minify: true,
  define: { 'process.env.NODE_ENV': '"production"' },
  target: ['es2020'],
  legalComments: 'eof',
  metafile: true,
});

// Isolate Arco's base selectors and CSS variables from Minimal Mistakes.
const cssFile = path.join(output, 'date-picker.css');
const stylesheet = postcss.parse(await readFile(cssFile, 'utf8'));
stylesheet.walkRules(rule => {
  if (rule.parent.type === 'atrule' && /keyframes$/.test(rule.parent.name)) return;
  rule.selectors = rule.selectors.map(selector => /^body\b/.test(selector)
    ? selector.replace(/^body/, '.thought-date-scope')
    : `.thought-date-scope ${selector}`);
});
await writeFile(cssFile, stylesheet.toString() + '\n' + await readFile(path.join(directory, 'theme.css'), 'utf8'));

// Keep the notices for every package actually included in the browser bundle.
const packages = new Set(Object.keys(result.metafile.inputs).filter(p => p.startsWith('node_modules/')).map(p => {
  const parts = p.slice('node_modules/'.length).split('/');
  return parts[0].startsWith('@') ? parts.slice(0, 2).join('/') : parts[0];
}));
let notices = 'Generated from tools/date-picker/package-lock.json. Rebuild: npm ci && npm run build\n';
for (const name of [...packages].sort()) {
  const pkgPath = path.join(directory, 'node_modules', name);
  const pkg = JSON.parse(await readFile(path.join(pkgPath, 'package.json'), 'utf8'));
  let license = '';
  for (const file of ['LICENSE', 'LICENSE.md', 'LICENSE.txt', 'LICENCE', 'license', 'license.md']) {
    try { license = await readFile(path.join(pkgPath, file), 'utf8'); break; } catch {}
  }
  if (!license) throw new Error(`Missing license for ${name}`);
  notices += `\n--- ${name}@${pkg.version} (${pkg.license}) ---\n${license.replace(/\r\n/g, '\n').trimEnd()}\n`;
}
await writeFile(path.join(output, 'LICENSES.txt'), notices.trimEnd() + '\n');
