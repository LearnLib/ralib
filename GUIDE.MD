# Running the experiments with Docker

This guide covers building the Docker image and running the three experiment
scripts. See [README.md](README.md) for background on the benchmarks and
repository layout.

## Prerequisites

Install Git and Docker, and start the Docker daemon. Run the commands below
from the root of this repository in a Bash-compatible shell.

The image provides JDK 21, Maven 3.9.16, Python, and the LaTeX tools required
for plotting. You do not need to install these dependencies on the host.
The initial build needs internet access to download the image and dependencies.

## Build the image

Initialize the RALib submodule, then build:

```bash
git submodule update --init --recursive
docker build -t ralib-benchmark .
```

The submodule uses a GitHub SSH URL, so your Git setup must have access to it.
The Docker build copies `ralib/` and runs `build.sh`, including RALib's tests.
It also includes `scripts/`, `experiments/`, and `configs/`, and creates an
empty `results/` directory. Existing host results are excluded from the image.

Rebuild the image after changing RALib, scripts, experiments, or configurations.

## Choose a script and save results

Create a host directory for the output:

```bash
mkdir -p results
```

Use this command structure:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark SCRIPT [SCRIPT_ARGUMENTS...]
```

Replace `SCRIPT` with one of the script names below. Docker options belong
before `ralib-benchmark`; script options belong after the script name.
Arguments are forwarded unchanged to the selected script.

The mount connects the host's `results/` directory to `/benchmark/results`
inside the container. `--rm` removes the container after it exits; files in
the mounted directory remain available on the host.

To list the supported scripts without starting an experiment:

```bash
docker run --rm ralib-benchmark
```

## Synthetic models

Run the synthetic benchmarks for all three learners:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-synthetic-models-experiments.sh
```

This script runs `slstar`, `sllambda`, and `sllambdaeq`, with one iteration per
benchmark and learner. It currently accepts no command-line options.

Logs and learned models are stored under `results/synthetic/`. The summary
is copied to `results/synthetic-models.csv` when the script exits.

## DTLS

Run the DTLS benchmarks and generate plots:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-dtls-experiments.sh
```

This script also runs all three learners with one iteration per benchmark
and learner, and currently accepts no command-line options.

Logs and learned models are stored under `results/dtls/`. Generated plots
are copied into the host's `results/` directory when the script exits:

- `plot-dtls-resets.pdf`
- `plot-dtls-resets-withstar.pdf`
- `plot-dtls-counterexamples.pdf`
- `plot-dtls-counterexamples-withstar.pdf`

## Palindromes

Run palindrome sizes 1 through 2 for all learners:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh --max-n 2
```

For a small run using one learner and one size:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh -n 1 -a slstar
```

The palindrome script supports these options:

| Option | Meaning |
| --- | --- |
| `-a ALG`, `--alg ALG` | Select `slstar`, `sllambda`, or `sllambdaeq`; defaults to all three. |
| `-n N` | Run only size `N`. |
| `-mn N`, `--max-n N` | Run sizes 1 through `N`; defaults to 2. For each learner, stop increasing the size after a failed run. |
| `-m MEM`, `--mem MEM` | Pass a memory setting such as `10g` through `MAVEN_OPTS`. |
| `-o DIR`, `--out DIR` | Choose the log directory inside the container. |
| `-h`, `--help` | Print usage and exit. |

Use `-n` for a single size or `--max-n` for a range. For example, to request
size 10 with `sllambda` and the script's `10g` memory setting:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh -n 10 -a sllambda -m 10g
```

Make enough memory available to Docker for the JVM and other container
processes. The script's `-m` option is separate from Docker's `--memory` limit.

By default, logs are stored under `results/palindrome/`, and the summary is
copied to `results/palindrome.csv` on script exit. Successful learning runs
include the learned model in the log.

If you set `--out`, use a path beneath `/benchmark/results` so the logs are
saved on the host. For example:

```bash
docker run --rm \
  --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh \
  -n 1 -a slstar --out /benchmark/results/custom-palindrome
```

This saves logs to `results/custom-palindrome/`; the summary still goes to
`results/palindrome.csv`.

Display the script's built-in help with:

```bash
docker run --rm ralib-benchmark run-palindrome-experiments.sh --help
```

## Keep separate runs

Scripts can overwrite earlier results. Use a fresh host directory for each
run you want to preserve, while keeping the container mount target unchanged:

```bash
mkdir -p results/run-01
docker run --rm \
  --mount type=bind,source="$(pwd)/results/run-01",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh --max-n 2
```

The logs and summary for this run will be under `results/run-01/`.
Avoid running containers concurrently against the same output directory.

The container runs as root by default, so on Linux its output files may be
owned by root. Logs and models are written during execution; summary CSVs
and PDFs are copied from the container's project root when the script exits.
Check the logs to confirm individual experiments succeeded, since the shell
scripts can continue after an experiment fails.
