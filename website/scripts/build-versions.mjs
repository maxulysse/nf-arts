// Builds the site once per release tag and once for the working tree ("dev"), each from that version's docs/.
// Output: dist/<version>/ for each version, and dist/index.html redirecting to the latest release.
import { execFileSync, execSync } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const site = join(dirname(fileURLToPath(import.meta.url)), '..');
const repo = join(site, '..');
const root = '/nf-arts/';

const releases = execFileSync('git', ['tag', '--list', '--sort=-version:refname'], { cwd: repo, encoding: 'utf-8' })
  .split('\n')
  .filter((tag) => /^\d+\.\d+\.\d+$/.test(tag));
const latest = releases[0] ?? 'dev';
const versions = ['dev', ...releases];

rmSync(join(site, 'dist'), { recursive: true, force: true });
const scratch = mkdtempSync(join(tmpdir(), 'nf-arts-docs-'));

for (const version of versions) {
  let docs = join(repo, 'docs');
  if (version !== 'dev') {
    // Tags match the semver pattern above, so they are safe in the shell command.
    execSync(`git archive ${version} docs | tar -x -C ${scratch} && mv ${scratch}/docs ${scratch}/${version}`, { cwd: repo });
    docs = join(scratch, version);
  }
  const env = {
    ...process.env,
    DOCS_DIR: docs,
    SITE_BASE: `${root}${version}/`,
    SITE_OUT: join(site, 'dist', version),
    PUBLIC_VERSION: version,
    PUBLIC_VERSIONS: versions.join(','),
    PUBLIC_LATEST: latest,
  };
  console.log(`\nBuilding ${version} from ${docs}`);
  execFileSync('node', ['scripts/sync-docs.mjs'], { cwd: site, env, stdio: 'inherit' });
  execFileSync('npx', ['astro', 'build'], { cwd: site, env, stdio: 'inherit' });
}

rmSync(scratch, { recursive: true, force: true });
writeFileSync(
  join(site, 'dist', 'index.html'),
  `<!doctype html>\n<meta http-equiv="refresh" content="0; url=${root}${latest}/">\n<link rel="canonical" href="${root}${latest}/">\n<p><a href="${root}${latest}/">nf-arts docs</a></p>\n`,
);
console.log(`\nBuilt ${versions.join(', ')}; ${root} redirects to ${latest}.`);
