# SLLambdaEq Artefact

This project contains the scripts for running the experiments
presented in the paper "Efficient Active Learning of Register
Automata with Equalities". The experiments themselves are run
in [RaLib](https://github.com/LearnLib/ralib). The project is
organized as follows:

1. **benchmarks** are located in ```experiments/```. These
    consist of benchmark problems collected from papers on
    RaLib, from the [automata wiki](https://automata.cs.ru.nl),
    located in ```experiments/synthetic```, and hypotheses
    of increasing complexity of the DTLS server of Mbed TLS
    2.26.0, located in ```experiments/dtls```. **TODO: palindrome**
2. **configurations** of RaLib are located in ```configs/```.
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
5. **RaLib** is a git sub module in ```ralib/```.


## RaLib Installation

To install RaLib, you have to initialize the submodule 
and build RaLib, which requires a JDK and Maven to be 
installed.

```
git submodule init
git submodule update
./build.sh
```

## Running experiments 

Generally, experiments are organized in series.
Shell scripts exist for running individual experiments and series,
and for generating plots and tables.

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

3. Run palindrome experiments

    **TODO**


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

