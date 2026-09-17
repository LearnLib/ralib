# VMCAI 2027 Artifact
=======================================
Paper title: Efficient Active Learning of Register Automata with Equalities

Claimed badges: Available + Functional + Reusable

**Badge Justification**
*Availability.* Our artifact is available under the DOI ().
Our algorithm (SLLAMBDAEQ) is implemented in [RALib](https://github.com/LearnLib/ralib), an open-source library which also implements the other algorithms we compared ours with (SLLAMBDA, SLSTAR).
The scripts and any other tooling needed to reproduce our experiments are also open sourced, and included in the artifact.
In particular, we include our extension of [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer) which was used to perform the experiments on DTLS implementations (we will hencefortfth call these "DTLS-Fuzzer experiments").
We plan to incorporate this extension into the next release of the tool.
Information regarding the versions of RALib and DTLS-Fuzzer used is available in #dependencies.

*Functional.* In our repeated testing, we were able to consistently reproduce the non-timing results reported in the evaluation.
Timing measurements exhibited only minor variations, but can vary substantially depending on the computation available, though this should not affect the outcome, such as which algorithm finish the fastest.
We note that reproducing the results for the DTLS-Fuzzer experiments requires appropriately setting timing parameters for each SUT, such as the time to wait for each response.
Not doing so might cause learning with any of the compared algorithms to fail due to apparent non-determinism.
The parameters chosen have worked on our machine, but might not work on a substantially weaker machine.

*Reusable.* RALib, based on which we implemented our algorithm, has been recently incorporated in [ProtocolState-Fuzzer](https://github.com/protocol-fuzzing/protocol-state-fuzzer/), a framework for building protocol state fuzzers, to provide support for learning register automata.
ProtocolState-Fuzzer is the backbone of fuzzers for an increasing number of protocols including DTLS (see DTLS-Fuzzer), SSH (see SSH-Fuzzer) and EDHOC (see EDHOC-Fuzzer).
Recent works have used the fuzzers mentioned, and the RALib-based extension in particular, to generate infer register automata of implementations of these protocols.
Effort is underway to port ProtocolState-Fuzzer to use the most recent RALib which implements SLLAMBDAEQ, with a prototype already being available and used by DTLS-Fuzzer.
Once porting is complete, all fuzzers will be able to use SLLAMBDAEQ.
RALib has also been used in areas such as specification extraction, which will also benefit from the performance of SLLAMBDAEQ.

Requirements:


External connectivity: yes

Maven needs to download possibly missing components from its repository,
Spotless downloads some metadata from maven's central repository.
Re-installing DTLS-Fuzzer downloads sources for its 

## Smoke test

1. Download docker image

```bash
docker load < sllambdaeq-vmcai27-image.tar.gz
```

Inspect that  the image was created (look for `sllambdaeq:vmcai27`)
```bash
docker image  ls -a
```

2. Create and run the container

This creates a container named `sllambdaeq-vmcai27` and starts an interactive shell inside the container.
```bash
docker run -it --name sllambdaeq-vmcai27 sllambdaeq:vmcai27
```

Steps 3. and 4. re-install RALib and DTLS-Fuzzer, which are already installed.
These steps can be ommitted when performing the evaluation.

3. Inside the container install RALib

RALib is included in the `ralib` folder and is already built in the image.
Go to that directory, cleanly install RALib, executing its test suite (~ 2-3 min), and assembling a JAR including all of RALib's dependencies.

```bash
cd ralib
mvn clean install assembly:single
```

This should result in a `BUILD SUCCESS` message and RALib's `target` directory should contain  `ralib-0.2.jar` and `ralib-0.2-jar-with-dependencies.jar`.
`cd ..` before proceeding to the next step.

4. Inside the container clean install DTLS-Fuzzer and its dependencies.

DTLS-Fuzzer is included in the `dtls-fuzzer` folder and is already built in the image, along its dependencies (TLS-Attacker and ProtocolState-Fuzzer, provided as separate sub-folders) and the five SUTs considered in our benchmarks, included in the `suts` subfolder.
Cleanly install DTLS-Fuzzer and its dependencies.

```bash
cd dtls-fuzzer 
(cd ProtocolState-Fuzzer; mvn clean install)
(cd TLS-Attacker; mvn clean install)
mvn clean install
```

All Maven installations should result in `BUILD SUCCESS`.

5. Execute a test with DTLS-Fuzzer

We now check that DTLS-Fuzzer with RALib support works by executing a handshake on Mbed TLS using input actions parametric on epoch.

```bash
java -jar dtls-fuzzer/target/dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar configs/dtls-black-box/args/learn_mbedtls_server_psk_epoch -test configs/dtls-black-box/tests/psk_epoch
```

The expected result should contain:

```
Test: PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_KEY_EXCHANGE[0[epoch_i]] CHANGE_CIPHER_SPEC[0[epoch_i]] FINISHED[1[epoch_i]] APPLICATION[1[epoch_i]]
1 times outputs: HELLO_VERIFY_REQUEST[] SERVER_HELLO|SERVER_HELLO_DONE[] TIMEOUT[] TIMEOUT[] CHANGE_CIPHER_SPEC|FINISHED[] APPLICATION[]
```

6. Exit container, restart it.

In the container, run `exit` to exit.
Then restart it.
```bash
docker start --interactive  sllambdaeq-vmcai27 
```

7. Remove container (after evaluation)

Once evaluation is done and the artifact is no longer needed, remove the container.
```bash
docker container rm sllambdaeq-vmcai27
```

*Troubleshooting.*
In case container does not work, check that it is still there (look for `sllambdaeq-vmcai27`), and if not, go to step 2.
```bash
docker container ls -a
```

If needed, the container can be removed (step 7), and then possibly re-created (step 2).

## Full review


This assumes the container is set up. Start container.
```bash
docker start --interactive  sllambdaeq-vmcai27 
```

### Analyzing saved results

Results are extracted from the logs/models generated by each experiment, and summarized in tables or plots.
The full results reported in the paper are stored in the `saved_results` folder, which contains:
- `synthetic` contains results (logs and models) for the [Automata Wiki](https://automata.cs.ru.nl) experiments, 
a summary is stored in `synthentic-models.csv`;
- `palindrome` contains results (logs) for the palindrome language experiments,
a summary is stored in `palindrome.csv`;
- `dtls-models` contains results (logs and models) for the experiments on DTLS models,
the resulting plot is stored in `plot-dtls-models-resets.pdf`;
- `dtls-implementations` contains results (logs and models) for the experiments on DTLS experiments, 
a summary is included in `dtls-implementations.csv`.

The tables and the plot can be checked for consistency with those in the paper.

### Running experiments

For each experiment type, we provide a script that can run all or a selected subset of the experiments included in the paper.
Results are stored in `results`, and can be compared to the ones saved (`saved_results`), or to the ones reported in the paper.


1. Run the synthetic models experiments with

```bash
./run-synthetic-models-experiments.sh
```

A table containing the results will be found in ```synthetic-models.csv``` in the project root directory.
Results (logs and models) can be found in ```results/```.

2. Run the DTLS experiments with

```bash
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




## Overview

The artifact contains the scripts for running the experiments.
The experiments themselves are run using [RALib](https://github.com/LearnLib/ralib).
Experiments on DTLS implementations additionally use [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer).
See [Dependencies](#dependencies) for the exact versions of the libraries.
The artifact is organized as follows:

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
5. **RALib** is a git submodule in ```ralib/```, which implements learning algorithms for register automata including SLLAMBDAEQ.
6. **DTLS-Fuzzer** is a git submodule in ```dtls-fuzzer/```, which is the RALib-based learning tool used in our black-box DTLS experiments.



## Installation

Installing RALib and DTLS-Fuzzer, you have to initialize their respective submodule and build the tools,
which require a JDK 21 and Maven to be installed.

```
git submodule init
git submodule update
./build.sh --dtlsfuzzer --dtlssuts
```

`--dtlsfuzzer` and `--dtlssuts` enable building also DTLS-Fuzzer and the DTLS SUTs, respectively.

## Smoke test


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

## Dependencies
