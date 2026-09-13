#!/usr/bin/env bash

# shellcheck disable=SC2034,SC2154

RESULTS_DIR="results/dtls-black-box"
readonly RESULTS_DIR
ARGS_DIR="configs/dtls-black-box/args"
readonly ARGS_DIR
CSV_NAME="dtls-black-box.csv"
readonly CSV_NAME

RA_SERVER_EXPERIMENTS=(
    "${ARGS_DIR}/learn_mbedtls_server_psk_epoch"
)

#    "args/ra/scandium/learn_scandium_server_psk_epoch"
#    "args/ra/wolfssl/learn_wolfssl_server_psk_epoch"
#    "args/ra/openssl/learn_openssl_server_psk_epoch"
#    "args/ra/piondtls/learn_piondtls_server_psk_epoch"

ALGORITHMS=(
    "SLLAMBDAEQ"
    "SLSTAR"
)

function gen_exp_for_each_alg() {
    # -n is a name reference, otherwise variable is assigned a value
    local -n input="$1"
    local -n output="$2"
    # additional arguments
    local extra_args="$3"

    output=()

    for experiment in "${input[@]}"; do
        for alg in "${ALGORITHMS[@]}"; do
            output+=("${experiment} -Doutput.ra=${RESULTS_DIR}/ra-${alg} -learningAlgorithm ${alg} ${extra_args} -roundLimit 3 ")
        done
    done
}

NO_SIMUL_EXPERIMENTS=2
RA_JAR="dtls-fuzzer/target/dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar"

# Optional filter pattern from command line
EXPERIMENT_FILTER="${1:-}"

function run_experiments() {
    local -n experiments="$1"
    local no_parallel="$2"
    local jarfile="$3"
    local filter="$4"

    for experiment in "${experiments[@]}"; do
        # Skip experiments that do not match the filter
        if [[ -n "$filter" && ! "$experiment" == "$filter" ]]; then
            continue
        fi

        if [[ $(jobs -r | wc -l) -ge ${no_parallel} ]]; then
            wait -n
        fi
        echo java -jar "${jarfile}" "${experiment}"
        (java -jar "${jarfile}" ${experiment}) &
        sleep 1
    done

    wait
}

gen_exp_for_each_alg RA_SERVER_EXPERIMENTS actual_experiments "${ARGS_SERVER}"

echo "The following experiments are considered"
printf '%s\n' "${actual_experiments[@]}"

run_experiments actual_experiments "${NO_SIMUL_EXPERIMENTS}" "${RA_JAR}" "${EXPERIMENT_FILTER}"

python3 scripts/extract_stats_dtls-black-box.py -o "${CSV_NAME}" "${RESULTS_DIR}"
