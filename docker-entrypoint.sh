#!/usr/bin/env bash
set -euo pipefail

cd /benchmark

case "${1:---help}" in
    -h|--help)
        echo "Usage: docker run [OPTIONS] IMAGE SCRIPT [SCRIPT_ARGUMENTS...]"
        echo "Scripts:"
        echo "  run-palindrome-experiments.sh [--help | --max-n N | -n N -a ALG -m MEM]"
        echo "  run-synthetic-models-experiments.sh"
        echo "  run-dtls-experiments.sh"
        echo "Mount your results directory at /benchmark/results to retain output."
        exit 0
        ;;
    run-palindrome-experiments.sh|run-synthetic-models-experiments.sh|run-dtls-experiments.sh|./run-palindrome-experiments.sh|./run-synthetic-models-experiments.sh|./run-dtls-experiments.sh)
        ;;
    *)
        echo "Unknown experiment script: $1 (use --help to list scripts)." >&2
        exit 2
        ;;
esac

# Retain summaries and plots that the original scripts write in the project root.
collect_outputs() {
    local status=$?
    trap - EXIT
    shopt -s nullglob
    local outputs=()
    local output
    for output in palindrome.csv synthetic-models.csv plot-dtls-*.pdf; do
        [[ ! -f "$output" ]] || outputs+=("$output")
    done
    if ((${#outputs[@]})); then
        cp -- "${outputs[@]}" results/ || status=1
    fi
    exit "$status"
}
mkdir -p results
trap collect_outputs EXIT
bash "$@"
