import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://maxulysse.github.io',
  // Set per version by scripts/build-versions.mjs.
  base: process.env.SITE_BASE ?? '/nf-arts/',
  outDir: process.env.SITE_OUT ?? './dist',
  output: 'static',
});
