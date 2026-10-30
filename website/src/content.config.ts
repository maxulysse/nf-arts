import { defineCollection, z } from 'astro:content';
import { glob } from 'astro/loaders';

// Pages copied from ../docs by scripts/sync-docs.mjs.
export const collections = {
  docs: defineCollection({
    loader: glob({ pattern: '*.md', base: './src/content/docs' }),
    schema: z.object({ title: z.string() }),
  }),
};
