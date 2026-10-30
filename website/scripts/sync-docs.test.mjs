import assert from 'node:assert/strict';
import { test } from 'node:test';
import { convert } from './sync-docs.mjs';

test('title becomes front matter', () => {
  assert.equal(convert('# Styles\n\nText.\n'), '---\ntitle: "Styles"\n---\n\nText.\n');
});

test('links and images are rewritten for the site', () => {
  const out = convert(
    '# T\n\nSee [gallery](gallery.md), [x](configuration.md#in-a-pipeline), ' +
      '[c](../CONTRIBUTING.md), [w](https://en.wikipedia.org/wiki/Hilbert_curve).\n' +
      '![](images/a.png) <img src="images/b.svg">\n',
  );
  assert.match(out, /\[gallery\]\(\/nf-arts\/#gallery\)/);
  assert.match(out, /\[x\]\(\/nf-arts\/configuration\/#in-a-pipeline\)/);
  assert.match(out, /\[c\]\(https:\/\/github\.com\/maxulysse\/nf-arts\/blob\/main\/CONTRIBUTING\.md\)/);
  assert.match(out, /\[w\]\(https:\/\/en\.wikipedia\.org\/wiki\/Hilbert_curve\)/);
  assert.match(out, /!\[\]\(\/nf-arts\/images\/a\.png\)/);
  assert.match(out, /src="\/nf-arts\/images\/b\.svg"/);
});

test('a page without a title fails', () => {
  assert.throws(() => convert('No title\n'));
});
