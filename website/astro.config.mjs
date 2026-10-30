import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://maxulysse.github.io',
  base: '/nf-arts/',
  integrations: [
    starlight({
      title: 'nf-arts',
      description: 'A Nextflow plugin that turns each run into generative art.',
      logo: {
        light: './src/assets/logo-wordmark.svg',
        dark: './src/assets/logo-wordmark-dark.svg',
        replacesTitle: true,
      },
      favicon: '/images/logo.svg',
      social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/maxulysse/nf-arts' }],
      editLink: { baseUrl: 'https://github.com/maxulysse/nf-arts/edit/main/docs/' },
      customCss: ['./src/styles/custom.css'],
      sidebar: [
        { label: 'Home', link: '/' },
        { label: 'Configuration', slug: 'configuration' },
        { label: 'Styles', slug: 'styles' },
        { label: 'Paint a finished run', slug: 'render' },
        { label: 'What goes into the art', slug: 'data' },
        { label: 'Gallery', link: '/#gallery' },
        { label: 'Contributing', link: 'https://github.com/maxulysse/nf-arts/blob/main/CONTRIBUTING.md' },
      ],
    }),
  ],
});
