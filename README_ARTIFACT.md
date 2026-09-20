# SLLAMBDAEQ Artifact

Paper title: Efficient Active Learning of Register Automata with Equalities

Claimed badges: Available + Functional + Reusable

**Badge Justification**

*Availability.* Our artifact is available under the DOI ().
Our algorithm (SLLAMBDAEQ) is implemented in [RALib](https://github.com/LearnLib/ralib), an open-source library which also implements the other algorithms we compared ours with (SLLAMBDA, SLSTAR).
The scripts and any other tooling needed to reproduce our experiments are also open sourced, and included in the artifact.
In particular, we include our extension of [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer) which was used to perform the DTLS implementation experiments.
We plan to incorporate this extension into the next release of the tool.
Information regarding the versions of RALib and DTLS-Fuzzer used is available in the Dependencies section at the end of the page.

*Functional.* In our repeated testing, we were able to consistently reproduce the non-timing results reported in the evaluation.
Timing measurements exhibited only minor variations, but can vary substantially depending on the computation available, though this should not affect the outcome, such as which algorithm finish the fastest.

*Reusable.* RALib, based on which we implemented our algorithm, has been recently incorporated in [ProtocolState-Fuzzer](https://github.com/protocol-fuzzing/protocol-state-fuzzer/), a framework for building protocol state fuzzers, to provide support for learning register automata.
ProtocolState-Fuzzer is the backbone of fuzzers for an increasing number of protocols including DTLS (see [DTLS-Fuzzer](https://github.com/assist-project/dtls-fuzzer)), EDHOC (see [EDHOC-Fuzzer](https://github.com/protocol-fuzzing/edhoc-fuzzer)) and SSH (see [SSH-Fuzzer](https://github.com/assist-project/ssh-fuzzer)).
Recent works have used the fuzzers mentioned (on DTLS [1], SSH [2] and EDHOC [3]), and the RALib-based extension in particular, to infer register automata of implementations of these protocols.
Effort is underway to port ProtocolState-Fuzzer to use the most recent RALib which implements SLLAMBDAEQ, with a prototype already being available and used by DTLS-Fuzzer.
Once porting is complete, all fuzzers will be able to use SLLAMBDAEQ.
RALib has also been used in areas such as specification extraction [4], which might also benefit from the improved performance of SLLAMBDAEQ.

Requirements:

  * RAM: 8 GB
  * CPU cores: 1
  * Time (smoke test): 1 to 2 minutes
  * Time (full review): 1 to 2 minutes

External connectivity: yes

Maven needs to download possibly missing components from its repository,
Spotless downloads some metadata from maven's central repository.


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

Steps 3 and 4 re-install RALib and DTLS-Fuzzer, which are already installed.
These steps can be ommitted when performing the evaluation.

3. Install RALib and DTLS-Fuzzer

RALib is included in the `ralib` folder and is already built in the image.
DTLS-Fuzzer is included in the `dtls-fuzzer` folder and is already built in the image, along its dependencies (TLS-Attacker and ProtocolState-Fuzzer, provided as separate sub-folders) and the five SUTs considered in our benchmarks, which are deployed in the `suts` subfolder.

We install RALib and DTLS-Fuzzer using the provided `build.sh` script.

```bash
./build.sh
```

The script should finish successfully, echoing `BUILD SUCCESS`.
`ralib/target` should exist and should contain  `ralib-0.2-jar-with-dependencies.jar`, which is the executable JAR we use to run RALib.
`dtls-fuzzer/target` should exist and should contain `dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar`, which is the executable JAR we use to run DTLS-Fuzzer extended to use RALib.

4. Execute a test with DTLS-Fuzzer

We now check that DTLS-Fuzzer with RALib support works by executing a handshake on Mbed TLS using input actions parametric on epoch.

```bash
java -jar dtls-fuzzer/target/dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar configs/dtls-black-box/args/learn_mbedtls_server_psk_epoch -test configs/dtls-black-box/tests/psk_epoch
```

The expected result should contain:

```
Test: PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_HELLO[0[epoch_i]] PSK_CLIENT_KEY_EXCHANGE[0[epoch_i]] CHANGE_CIPHER_SPEC[0[epoch_i]] FINISHED[1[epoch_i]] APPLICATION[1[epoch_i]]
1 times outputs: HELLO_VERIFY_REQUEST[] SERVER_HELLO|SERVER_HELLO_DONE[] TIMEOUT[] TIMEOUT[] CHANGE_CIPHER_SPEC|FINISHED[] APPLICATION[]
```

5. Exit container, restart it

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

We assume the image has been downloaded, and that container has been created.
Start the container.

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
a summary is stored in `dtls-models.csv`, and a plot in `plot-dtls-models-resets.pdf`;
- `dtls-implementations` contains results (statistics files and models) for the experiments on DTLS implementations, 
a summary is included in `dtls-implementations.csv`.

The tables and the plot can be checked for consistency with those in the paper.

### Running experiments

For each experiment type, we provide a script that can run all or a selected subset of the experiments included in the paper.
Results are stored in `results`, and can be compared to the ones saved (`saved_results`), or to the ones reported in the paper.
After each experiment, results can be copied to the host for easier analysis.
This is done using the command below:

```bash
docker cp sllambdaeq-vmcai27:/results ${results}
```

where `${results}` is replaced by a directory on the host machine.


1. Run the synthetic models (? minutes)

```bash
./run-synthetic-models-experiments.sh
```

2. Run palindrome experiments for size up to `max_n`

```bash
./run-palindrome-experiments.sh --max-n max_n
```

We suggest using `max_n` of 8 or smaller, which should reduce experiment time to under 10 minutes.

3. Run DTLS model experiments

```bash
./run-dtls-model-experiments.sh
```

We excluded SLSTAR for these experiments to reduce execution time.

4. Run DTLS implementation experiments, optionally providing the algorithm (SLLAMBDA, SLLAMBDAEQ, SLSTAR) and the implementation/SUT (mbedtls, openssl, piondtls, scandium, wolfssl)

```bash
./run-dtls-experiments.sh [-a algorithm][-s sut]
```

Without aguments, the script runs all the experiments which can take over six hours.
For the sake of time, we suggest restricting experiments to one algorithm and/or one implementation.
The resulting table (in `results/dtls-implementations.csv`) will contain results only for the experiment(s) run.
For example,
```bash
./run-dtls-experiments.sh -s mbedtls -a SLLAMBDAEQ
```
performs learning with SLLAMBDAEQ of the Mbed TLS server implementations.

The time estimate can be drawn from Table 3 in the paper.
Learning Mbed TLS with SLLAMBDAEQ should take under 10 minutes.

Experiment success is indicated in the "Finished" column in `results/dtls-implementations.csv`.
DTLS implementation experiments can fail if timing parameters (e.g., time to wait for each response) are not suitable (typically too low) for the SUT given the running machine.
This can result in the implementation exhibiting timing-related non-determinism, which none of the algorithms can handle.
On such an occurrence, a different SUT can be chosen.

## Dependencies

We record the used repositories/commits for RALib and its dependencies. We forked the original repositories and made minor edits to ease setup.

- name: dtls-fuzzer
   url: https://github.com/artifact-sllambdaeq/dtls-fuzzer.git
   commit: 2393e995e22796398ae1121575301d10cd9cddb5
- name: ralib
   url: https://github.com/artifact-sllambdaeq/ralib.git
   commit: 91965b283d2b8e4c692d12dec6e028026dba72cc
- name: protocol-state-fuzzer
   url: https://github.com/artifact-sllambdaeq/protocol-state-fuzzer.git
   commit: "f9f8f146c9725e98a819c0f79555df56994f5d06"

## References

[1] Oskar Pettersson Löfstedt. *Protocol State Fuzzing of DTLS Using Register Automata Learning*. Master's Thesis, Uppsala University, 2026.
[2] Linus Olofsson. *Protocol State Fuzzing of EDHOC Implementations Using Register Automata Learning*. Master's Thesis, Uppsala University, 2024.
[3] Hasan Syed Muhammad. *Extending State Fuzzing of SSH Implementations Using Model Learning for Register Automata*. Master's Thesis, Uppsala University, 2026.
[4] S. Ghosal, B. Jonsson, and P. Rümmer, "An Active Learning Approach to Synthesizing Program Contracts," in *Software Engineering and Formal Methods (SEFM 2023)*, pp. 126-144, 2023. 【1-54d1ca】【2-f19281】