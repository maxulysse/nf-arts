// Copies ../docs into the Starlight site, so docs/ stays the only source.
// Each page's first "# Title" becomes front matter; .md links and image paths are rewritten for the site.
import { cpSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const site = join(dirname(fileURLToPath(import.meta.url)), '..');
const docs = join(site, '..', 'docs');
const pages = join(site, 'src', 'content', 'docs');
const base = '/nf-arts/';
const repo = 'https://github.com/maxulysse/nf-arts/blob/main/';

// docs/README.md is the GitHub index and docs/gallery.md the GitHub gallery; the site's home page (index.mdx) has both.
const skip = new Set(['README.md', 'gallery.md']);

export function convert(text) {
  const match = text.match(/^# (.+)\n+/);
  if (!match) throw new Error('page has no "# Title" on its first line');
  const body = text
    .slice(match[0].length)
    .replace(/\]\(\.\.\/([^)]+)\)/g, `](${repo}$1)`)
    .replace(/\]\(gallery\.md\)/g, `](${base}#gallery)`)
    .replace(/\]\(([\w-]+)\.md(#[^)]*)?\)/g, (_, page, anchor = '') => `](${base}${page}/${anchor})`)
    .replace(/(\]\(|src=")images\//g, `$1${base}images/`);
  return `---\ntitle: ${JSON.stringify(match[1])}\n---\n\n${body}`;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  for (const file of readdirSync(pages)) {
    if (file.endsWith('.md')) rmSync(join(pages, file));
  }
  for (const file of readdirSync(docs)) {
    if (!file.endsWith('.md') || skip.has(file)) continue;
    writeFileSync(join(pages, file), convert(readFileSync(join(docs, file), 'utf-8')));
  }
  cpSync(join(docs, 'images'), join(site, 'public', 'images'), { recursive: true });
  mkdirSync(join(site, 'src', 'assets'), { recursive: true });
  for (const logo of ['logo.svg', 'logo-wordmark.svg', 'logo-wordmark-dark.svg']) {
    cpSync(join(docs, 'images', logo), join(site, 'src', 'assets', logo));
  }
}
