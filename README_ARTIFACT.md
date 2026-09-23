# SLLAMBDAEQ Artifact

Paper title: Efficient Active Learning of Register Automata with Equalities

Claimed badges: Available + Functional + Reusable

**Badge Justification**

*Availability.* Our artifact is available under the DOI provided.
Our algorithm (SLLAMBDAEQ) is implemented in [RALib](https://github.com/LearnLib/ralib), an open-source library which also implements the other algorithms we compared ours with (SLLAMBDA, SLSTAR).
The scripts and any other tooling needed to reproduce our experiments are also open source, and included in the artifact.
In particular, we include our extension of [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer) which was used to perform the DTLS implementation experiments.
We plan to incorporate this extension into the next release of the tool.
Information regarding the versions of RALib and DTLS-Fuzzer used is available in the Dependencies section at the end of the page.

*Functional.* In our repeated testing, we were able to consistently reproduce the non-timing results reported in the evaluation.
Timing measurements exhibited only minor variations, but can vary substantially depending on the computation available, though this should not affect the outcome, such as which algorithm finish the fastest.

*Reusable.* RALib, which contains the implementation of our algorithm, has been recently incorporated in [ProtocolState-Fuzzer](https://github.com/protocol-fuzzing/protocol-state-fuzzer/), a framework for building protocol state fuzzers, to provide support for learning register automata.
ProtocolState-Fuzzer is the backbone of fuzzers for an increasing number of protocols including DTLS (see [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer)), EDHOC (see [EDHOC-Fuzzer](https://github.com/protocol-fuzzing/edhoc-fuzzer)) and SSH (see [SSH-Fuzzer](https://github.com/assist-project/ssh-fuzzer)).
Recent works have used the fuzzers mentioned (on DTLS [1], EDHOC [2] and SSH [3]), and the RALib-based extension in particular, to infer register automata of implementations of these protocols.
Effort is underway to port ProtocolState-Fuzzer the most latest version of RALib, which implements SLLAMBDAEQ.
A prototype is already available and is used by the DTLS-Fuzzer version included in this artifact.
Once porting is complete, all fuzzers will be able to use SLLAMBDAEQ.
RALib has also been used in areas such as specification extraction [4], which might also benefit from the improved performance of SLLAMBDAEQ.

Requirements (recommended):

  * RAM: 16 GB
  * CPU cores: 2
  * Time (smoke test): 10 minutes
  * Time (full review): 2 to 4 hours

External connectivity: yes

Maven needs to download possibly missing components from its repository,
Spotless downloads some metadata from maven's central repository.


## Smoke test

1. **Download docker image**

   ```bash
   docker load -i sllambdaeq_${arch}.tar.gz
   ```

   Inspect that the image was created (look for `sllambdaeq:latest`)
   ```bash
   docker image  ls -a
   ```

2. **Create and run the container**

   This creates a container named `sllambdaeq` and starts an interactive shell inside the container.
   ```bash
   docker run -it --name sllambdaeq sllambdaeq:latest
   ```

   The work directory is `/benchmark`.

3. **Run brief RALib experiment**

   RALib is included in the `ralib` folder and is pre-built in the image; its build artifacts are stored under `ralib/target`.
   We use RALib to infer palindromes up to 6.

   ```bash
   ./run-palindrome-experiments.sh --even --max-n 6
   ```

   This should generate:
   (1) a folder `results/palindrome`  which should contain files with names of form `palindrome-size-${n}-${alg}.log ` where `{n}` is a number and `${alg}` is an algorithm;
   (2) a non-empty file `results/palindrome.csv`.

4. **Execute a test with DTLS-Fuzzer**

   DTLS-Fuzzer with register automata learning support is included in the `dtls-fuzzer` folder and is pre-built in the image; its build artifacts are stored under `dtls-fuzzer/target`.
   We use DTLS-Fuzzer to execute a handshake on Mbed TLS using input actions parametric on epoch.

   ```bash
   java -jar dtls-fuzzer/target/dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar configs/dtls-implementations/args/learn_mbedtls_server_psk_epoch -test configs/dtls-implementations/tests/psk_epoch
   ```

   The log should contain the excerpt below. Having `APPLICATION` as last output indicates that we have successfully completed a handshake.

   ```
   Test: PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_KEY_EXCHANGE[0[epoch_i]] CHANGE_CIPHER_SPEC[0[epoch_i]] FINISHED[1[epoch_i]] APPLICATION[1[epoch_i]]
   1 times outputs: HELLO_VERIFY_REQUEST[] SERVER_HELLO|SERVER_HELLO_DONE[] TIMEOUT[] TIMEOUT[] CHANGE_CIPHER_SPEC|FINISHED[] APPLICATION[]
   ```

5. **Exit container, restart it**

   In the container, run `exit` to exit.
   Then restart it.
   ```bash
   docker start --interactive  sllambdaeq
   ```

6. **Remove container (after evaluation)**

   Once evaluation is done and the artifact is no longer needed, remove the container.
   ```bash
   docker container rm sllambdaeq
   ```

*Troubleshooting.*

In case the container does not work, check that it is still there (look for `sllambdaeq`), and if not, go to step 2.
```bash
docker container ls -a
```

If needed, the container can be removed (step 6), and then possibly re-created (step 2).

## Full review


The full review is organized as follows.
First we briefly describe the structure of the artifact.
Next, we explain the procedure for running each of the four benchmarks presented in our work, and reviewing the results.
We end with instructions on how to build RALib and DTLS-Fuzzer from their sources contained in the artifact.
> [!NOTE]
> The benchmarks are _synthetic models_, _DTLS models_, _palindrome_ and _DTLS implementations_.



We assume the image has been loaded, and that container has been created.
Start the container.

```bash
docker start --interactive  sllambdaeq
```

### Structure

```
/benchmark/
├── ralib/                          # source code and build outputs for RALib
├── dtls-fuzzer/                    # source code and build outputs for DTLS-Fuzzer, its two major dependencies in ProtocolState-Fuzzer and TLS-Attacker and the implementations considered
|   |── ProtocolState-Fuzzer
|   └── Tls-Attacker
|   └── suts                        # source code and build outputs for five implementations
└── configs/                        # configuration files used by three benchmarks
└── experiments/                    # models used by the benchmarks on synthetic and DTLS models
└── saved_results/                  # results and summaries that are used in the paper's evaluation
└── scripts/                        # helper scripts used to run experiments/summarize results
└── run-${benchmark}-experiments.sh # script to execute experiments and summarize results for the benchmark ${benchmark}
└── build.sh                        # script to build RALib and DTLS-Fuzzer
└── Dockerfile                      # used to build the Docker image
└── LICENSE                         # license
└── README.md                       # this file
```

### Running experiments

We provide for each benchmark a script that can automatically run its experiments and summarize the results in tables or plots.
Results are stored under the `results` folder, organized in distinct subfolders for each benchmark.
Summaries are stored directly under `results`.
Results can be compared to the those reported in the paper, which are stored in `saved_results`.
Note that re-running a benchmark will overwrite any old results/summaries connected to it.

To facilitate analysis, after running each benchmark, results can be copied to the host, e.g., using the command below:

```bash
docker cp sllambdaeq:/benchmark/results ${results}
```

where `${results}` is replaced by a path to a directory on the host machine where results should be stored.


1. **Run learning experiments on the synthetic models from the [Automata Wiki](https://automata.cs.ru.nl)**

   ```bash
   ./run-synthetic-models-experiments.sh
   ```

   Experiments take around 5–10 minutes to finish.
   Results are stored in `synthetic` (within the `results` folder).
   They are organized in distinct folders named after the system and algorithm used (e.g., `abp_output-sllambda`).
   Each folder includes the learned model and the log, from which results are derived.

   A summary of the results is stored in `synthetic-models.csv` (in the `results` folder).
> [!NOTE]
> The term *resets* (as in “Total Resets”) is used synonymously with *tests*.

2. **Run palindrome experiments for even values of `n` up to `max_n`**

   ```bash
   ./run-palindrome-experiments.sh --even --max-n max_n [--mem mem]
   ```

   We suggest using `max_n` of 10 or smaller, which keeps experiment time under 5 minutes.
   For a `max_n` of 12, experiments take around 20 minutes.
   We advise against running experiments with `max_n` of 14 or higher, as they require a much larger heap and can take over 5 hours to finish.
   Such a heap can be configured via `--mem`, e.g., `--mem 150g` sets a 150‑GB heap, which is what we used in the paper.

   Results are stored in `palindrome`.
   They appear as output logs named after the algorithm and the maximum palindrome size used in the experiment (e.g., `palindrome-size-4-SLLambdaEq.log`).
   A summary is stored in `palindrome.csv`.


3. **Run DTLS model experiments**

   ```bash
   ./run-dtls-models-experiments.sh
   ```

   To save time, SLSTAR is ommitted from these experiments, which take around 20 minutes to finish.
   Results are stored in `dtls-models`.
   They are organized in distinct folders which are named after the system (client/server), the hyptothesis number and algorithm used (e.g., `client-10-sllambda`).
   Each folder includes the log and learned model, from which results are derived.
   A summary of the results is stored in `dtls-models.csv`, and a plot in `plot-dtls-models-tests.pdf`.

4. **Run DTLS implementation experiments, optionally providing the algorithm (SLLAMBDA, SLLAMBDAEQ, SLSTAR) and the implementation/SUT (mbedtls, openssl, piondtls, scandium, wolfssl)**

   ```bash
   ./run-dtls-implementations-experiments.sh [-a algorithm][-s sut]
   ```

   Without aguments, the script runs all the experiments which can take over six hours.
   For the sake of time, we suggest restricting experiments to one algorithm and/or one implementation.
   The resulting table (in `results/dtls-implementations.csv`) will contain results only for the experiment(s) run.
   For example,
   ```bash
   ./run-dtls-implementations-experiments.sh -s mbedtls -a SLLAMBDAEQ
   ```
   performs learning with SLLAMBDAEQ of the Mbed TLS server implementation.
   A rough time estimate can be drawn from Table 3 in the paper.
   Learning Mbed TLS with SLLAMBDAEQ should take under 10 minutes.
   Note that the output log reports a higher number of locations than the paper or summary because it counts sink locations, which the paper and summary omit.
   
   Results are stored in `dtls-implementations`.
   They are organized in distinct sub-folders for each algorithm, and further subdivided for each SUT.
   The output folder for an experiment (e.g., `ra-SLSTAR/mbedtls-2.26.0_server_psk_epoch`)  contains learned model (`learnedModel.dot`) and the statistics file (`statistics.txt`) from which results are derived.
   A summary of the results is included in `dtls-implementations.csv`.
> [!NOTE]
> DTLS implementation experiments can fail if timing parameters (e.g., time to wait for each response) are not suitable (typically too low) for the SUT given the running machine.
> Failure can be identified by checking the "Finished" column in `dtls-implementations.csv`, which denotes experiment success.
> We chose parameters that should work on most machines, but there is always a risk that they do not.
> If that happens, a different SUT can be chosen, or experiments can be run on a more powerful machine.

### Rebuilding RALib and DTLS-Fuzzer

The source code of both RALib and DTLS-Fuzzer is included in the artifact, as is the code for DTLS-Fuzzer's dependencies (TLS-Attacker and ProtocolState-Fuzzer).
This allows both tools to be re-built, which can be done conveniently using the provided `build.sh` script.

```bash
./build.sh --ralib --dtlsfuzzer
```

The script should finish successfully, echoing `BUILD SUCCESS`.
`ralib/target` should exist and should contain a newly-generated `ralib-0.2-jar-with-dependencies.jar`, which is the executable JAR we use to run RALib.
`dtls-fuzzer/target` should exist and should contain a newly-generated `dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar`, which is the executable JAR we use to run DTLS-Fuzzer extended to use RALib.


## Dependencies

We record the used repositories/commits for RALib and its dependencies. We forked the original repositories and made minor edits to ease setup.

- name: dtls-fuzzer
   url: https://github.com/artifact-sllambdaeq/dtls-fuzzer.git
   commit: 2393e995e22796398ae1121575301d10cd9cddb5
- name: protocol-state-fuzzer
   url: https://github.com/artifact-sllambdaeq/protocol-state-fuzzer.git
   commit: f9f8f146c9725e98a819c0f79555df56994f5d06
- name: ralib
   url: https://github.com/artifact-sllambdaeq/ralib.git
   commit: 91965b283d2b8e4c692d12dec6e028026dba72cc

## References

1. **Pettersson Löfstedt, O.** (2026). *Protocol State Fuzzing of DTLS Using Register Automata Learning*. Master’s Thesis, Uppsala University.

2. **Olofsson, L.** (2024). *Protocol State Fuzzing of EDHOC Implementations Using Register Automata Learning*. Master’s Thesis, Uppsala University.

3. **Hasan Syed Muhammad** (2026). *Extending State Fuzzing of SSH Implementations Using Model Learning for Register Automata*. Master’s Thesis, Uppsala University.

4. **Ghosal, S., Jonsson, B., & Rümmer, P.** (2023). An active learning approach to synthesizing program contracts. In *Software Engineering and Formal Methods (SEFM 2023)*, pp. 126–144. 【1‑54d1ca】【2‑f19281】
