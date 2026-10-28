# Styles

In every style, color is the process, cached tasks are faded and failed tasks are thinner, smaller, hollow or leafless. The session ID seeds the random choices, so a run always gives the same image.

| Style | What it draws |
| --- | --- |
| `radial` | Each process is a ring, each task an arc on it. Angle is time, clockwise from 12 o'clock. Thickness is peak memory. |
| `hilbert` | Tasks run end to end along a [Hilbert curve](https://en.wikipedia.org/wiki/Hilbert_curve) in start order, each taking a share proportional to its duration. Thickness is peak memory. More tasks make a finer curve; the seed picks one of its 8 orientations. |
| `ridgeline` | One ridge per process, top to bottom, time left to right. Ridge height is how many of the process's tasks were running, relative to its busiest moment. |
| `phyllotaxis` | Tasks are sunflower seeds on a golden-angle spiral, in start order. Dot size is peak memory. |
| `flowfield` | Each task is a brush stroke drifting through a seeded flow field, starting at its start order (x) in its process's row (y). Length is duration, width is peak memory. |
| `mondrian` | One block per process, split into one block per task, area proportional to duration. Opacity is peak memory; failed tasks are hollow. Defaults to the `mondrian` palette. |
| `tree` | The run is the trunk, each process a branch in start order from the bottom. Branch length is how long the process was active. Each task is a twig with a leaf: twig length is duration, leaf size is peak memory. Failed tasks have no leaf. |
| `ginkgo` | The run is one ginkgo leaf. Each task is a vein fanning out from the stem and forking near its tip. Angle is start time, left to right, length is duration, width is peak memory. |

## On real runs

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

More megatests in the [gallery](gallery.md).
