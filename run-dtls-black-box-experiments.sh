#!/usr/bin/env bash

# shellcheck disable=SC2034,SC2154

# Folder to save results
RESULTS_DIR="results/dtls-black-box"
readonly RESULTS_DIR
# Folder to store the table
OUTPUT_DIR="results"
readonly OUTPUT_DIR
# DTLS-Fuzzer argument (configuration) files
ARGS_DIR="configs/dtls-black-box/args"
readonly ARGS_DIR
# Location of DTLS-Fuzzer JAR
DTLSFUZZER_JAR="dtls-fuzzer/target/dtls-fuzzer-0.3-SNAPSHOT-ra-jar-with-dependencies.jar"

# Generic arguments for each SUT
declare -A args
args[mbedtls]="${ARGS_DIR}/learn_mbedtls_server_psk_epoch"
args[scandium]="${ARGS_DIR}/learn_scandium_server_psk_epoch"
args[wolfssl]="${ARGS_DIR}/learn_wolfssl_server_psk_epoch"
args[openssl]="${ARGS_DIR}/learn_openssl_server_psk_epoch"
args[piondtls]="${ARGS_DIR}/learn_piondtls_server_psk_epoch"

# Algorithms
algs=(
    "SLLAMBDA"
    "SLLAMBDAEQ"
    "SLSTAR"
)

# Number of simultaneous experiments
no_sim=2
# Name of result CSV file
csv="dtls-black-box.csv"
alg=""
sut=""

print_usage() {
  echo "Usage $0 [-a|--alg alg][-s|--sut sut][-ns|--no-sim no_sim][-h|--help]"
  echo "Where:"
  echo " alg is an algorithm in ${algs[@]};"
  echo " sut is a SUT in  ${!args[@]};"
  echo " no_sim is the number of experiments to run simultaneously;"
  echo " -h|--help prints usage instructions and exits gracefully."
}
while [[ $# -gt 0 && "$1" =~ ^- ]]; do case $1 in
    -a | --alg )
        shift;
        alg=$1
        ;;
    -s | --sut )
        shift;
        sut=$1
        ;;
    -ns )
        shift;
        ns=$1
        ;;  
    -h | --help )
        print_usage
        exit 0
        ;;
    * )
        break
        ;;
    esac; shift; done


sel_algs=""
if [[ -n ${alg} ]]; then
  if [[ " ${algs[*]} " == *" $alg "* ]]; then
    echo "Running for algorithm $alg"
    sel_algs=("$alg")
  else 
    echo "Invalid algorithm $alg"
    exit 1
  fi
else 
  echo "Running for all algorithms"
  sel_algs=("${algs[@]}")
fi

sel_suts=""
if [[ -n ${sut} ]]; then
  if [[ " ${!args[*]} " == *" $sut "* ]]; then
    echo "Running for SUT $sut"
    sel_suts=("$sut")
  else 
    echo "Invalid sut $sut"
    exit 1
  fi
else 
  echo "Running for all SUTs"
  sel_suts=("${!args[@]}")
fi

function run_experiments() {
    local no_parallel="$1"
    for sut in "${sel_suts[@]}"; do
        for alg in "${sel_algs[@]}"; do
            if [[ $(jobs -r | wc -l) -ge ${no_parallel} ]]; then
                wait -n
            fi
            echo java -jar "${DTLSFUZZER_JAR}" ${args[$sut]} -Doutput.ra=${RESULTS_DIR}/ra-${alg} -learningAlgorithm ${alg}
            (java -jar "${DTLSFUZZER_JAR}" ${args[$sut]} -Doutput.ra=${RESULTS_DIR}/ra-${alg} -learningAlgorithm ${alg}) &
            sleep 1
        done
    done
    wait
}

run_experiments "${no_sim}"

mkdir -p ${OUTPUT_DIR}

python3 scripts/extract_stats_dtls-black-box.py -o ${OUTPUT_DIR}/"${csv}" "${RESULTS_DIR}"
