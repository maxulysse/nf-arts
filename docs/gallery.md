# Gallery

Every image here comes from a real run, rendered with `render`: the nf-core/rnavar test profile from its local history, and nf-core megatests from their trace files on `s3://nf-core-awsmegatests`.

## Every style

| Style | rnavar test, 43 tasks | rnavar megatest, 553 tasks | sarek megatest, 2314 tasks |
| --- | :---: | :---: | :---: |
| radial | ![](images/test-rnavar-radial.png) | ![](images/megatest-rnavar-radial.png) | ![](images/megatest-sarek-radial.png) |
| hilbert | ![](images/test-rnavar-hilbert.png) | ![](images/megatest-rnavar-hilbert.png) | ![](images/megatest-sarek-hilbert.png) |
| ridgeline | ![](images/test-rnavar-ridgeline.png) | ![](images/megatest-rnavar-ridgeline.png) | ![](images/megatest-sarek-ridgeline.png) |
| phyllotaxis | ![](images/test-rnavar-phyllotaxis.png) | ![](images/megatest-rnavar-phyllotaxis.png) | ![](images/megatest-sarek-phyllotaxis.png) |
| flowfield | ![](images/test-rnavar-flowfield.png) | ![](images/megatest-rnavar-flowfield.png) | ![](images/megatest-sarek-flowfield.png) |
| mondrian | ![](images/test-rnavar-mondrian.png) | ![](images/megatest-rnavar-mondrian.png) | ![](images/megatest-sarek-mondrian.png) |
| tree | ![](images/test-rnavar-tree.png) | ![](images/megatest-rnavar-tree.png) | ![](images/megatest-sarek-tree.png) |
| ginkgo | ![](images/test-rnavar-ginkgo.png) | ![](images/megatest-rnavar-ginkgo.png) | ![](images/megatest-sarek-ginkgo.png) |

The rnavar images use the `nfcore` palette, sarek `magma`, and mondrian its own `mondrian` palette.

## More megatests

| Pipeline | Radial | Hilbert |
| --- | :---: | :---: |
| rnaseq, 332 tasks, `viridis` | ![](images/megatest-rnaseq-radial.png) | ![](images/megatest-rnaseq-hilbert.png) |
| mag, 3207 tasks, `ocean` | ![](images/megatest-mag-radial.png) | ![](images/megatest-mag-hilbert.png) |
| raredisease, 990 tasks, `sunset` | ![](images/megatest-raredisease-radial.png) | ![](images/megatest-raredisease-hilbert.png) |
| viralrecon, 3239 tasks, `mono` | ![](images/megatest-viralrecon-radial.png) | ![](images/megatest-viralrecon-hilbert.png) |
| taxprofiler, 239 tasks, `rainbow` | ![](images/megatest-taxprofiler-radial.png) | ![](images/megatest-taxprofiler-hilbert.png) |

## Custom colors

The rnavar test profile with `colors = ['#ff006e', '#8338ec', '#3a86ff']` and `background = '#ffffff'`, as SVG:

<img src="images/test-rnavar-custom.svg" width="300" alt="radial art of the rnavar test profile with custom colors">
