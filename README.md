# SLLambdaEq Artefact

This project contains the scripts for running the experiments
presented in the paper "Efficient Active Learning of Register
Automata with Equalities". The experiments themselves are run
in [RALib](https://github.com/LearnLib/ralib). The project is
organized as follows:

1. **benchmarks** are located in ```experiments/```. These
    consist of benchmark problems collected from papers on
    RALib, from the [automata wiki](https://automata.cs.ru.nl),
    located in ```experiments/synthetic```, and hypotheses
    of increasing complexity of the DTLS server of Mbed TLS
    2.26.0, located in ```experiments/dtls```.
2. **configurations** of RALib are located in ```configs/```.
    The different configurations are used in different 
    series of experiments and specify aspects of the 
    learning experiments (how counterexamples are found,
    how counterexamples are preprocessed, max. runtimes, 
    teachers/theories for types, etc.).
3. **scripts** for running experiments and generating plots
    and tables are located in ```scripts/```.
4. **results** from running experiments will be stored in 
    ```results/```. Results include logs and models from 
    individual learning experiments. Some scripts will 
    use the results to compute plots and tables. 
    Templates and generated documents ce be found 
    in ```scripts/plots```.
5. **RALib** is a git sub module in ```ralib/```.


## RALib Installation

To install RALib, you have to initialize the submodule 
and build RALib, which requires a JDK and Maven to be 
installed.

```
git submodule init
git submodule update
./build.sh
```

## Running experiments

### Running with Docker

The image builds RALib using `build.sh` with JDK 21 and Maven 3.9.16.
It includes Python and LaTeX for experiment summaries and plots.
Initialize the RALib submodule before building:

```bash
git submodule update --init --recursive
docker build -t ralib-benchmark .
mkdir -p results
```

Choose a script after the image name. Additional arguments are passed
unchanged to that script:

```bash
docker run --rm --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-palindrome-experiments.sh --max-n 2

docker run --rm --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-synthetic-models-experiments.sh

docker run --rm --mount type=bind,source="$(pwd)/results",target=/benchmark/results \
  ralib-benchmark run-dtls-experiments.sh
```

Logs and models are written to the host's `results/` directory. On script
exit, generated summary CSVs and DTLS PDFs are also copied there. Existing
host results are excluded from the image build. The scripts may overwrite
results from earlier runs; use a separate host directory to keep runs apart.
Files created by the container use its default root user.

Run `docker run --rm ralib-benchmark` for the script list, or
`docker run --rm ralib-benchmark run-palindrome-experiments.sh --help`
for palindrome options. The synthetic and DTLS scripts currently have no
command-line options. When using palindrome's `--out` option, choose a path
under `/benchmark/results` to keep its logs on the host.

Generally, experiments are organized in series.
Shell scripts exist for running individual experiments and series,
and for generating plots and tables.
Palindrome experiments are treated separately due to the more limited support in RALib for acceptor RAs.

### Experiments for the evaluation of *SLLambdaEq* (ATVA'26)

This section describes how one can run the experiments 
that are reported in the ATVA 2026 submission.

1. Run the synthetic models experiments with

    ```
    ./run-synthetic-models-experiments.sh
    ```

    A table containing the results will be found in ```synthetic-models.csv```
    in the project root directory. Results (logs and models) can be found in
    ```results/```.

2. Run the DTLS experiments with

    ```
    ./run-dtls-experiments.sh
    ```
    
    After the experiments have run, plots will be output to the project
    root directory. Results (logs and models) can be found in ```results/```.

3. Run palindrome experiments for sizes up to max_n

    ```
    ./run-palindrome-experiments.sh --max-n max_n
    ```
    A table containing the results will be found in ```palindrome.csv```
    in the project root directory. Results (logs) can be found in
    ```results/```.
    If successful, the print-out of the learned model is included in the log.

### Running individual experiments

```
scripts/run_experiment.sh [-h] -s series -e experiment -i iterations -l learner
```

Learn model of `experiments/[series]/[experiment].xml` with config
`configs/[series]/[learner]'.

### Running a series

```
scripts/run_series.sh [-h] -s series -i iterations -l learner
```

Run the series of experiments specified in folder
`experiments/[series]` with specified learner.


### Searching in logs

```
scripts/search_logs.sh series experiment learner search_term
```

Searches for `search_term` in the logs from all iterations.

### Running custom palindrome experiments
```
./run-palindrome-experiments.sh -n n -a alg
```

Runs experiments for palindrome languages of size n, using the provided algorithm.

Larger sizes (10 or more) may require allocating more heap space to ensure learning does not run out of memory.
This can be done using the `-m` option, e.g., the command below allocates 10 GB of heap space to learn using SLLambdaEq, a palindrome language of size 10.


```
./run-palindrome-experiments.sh -n 10 -a sllambda -m 10g
```
