#!/usr/bin/env bash
RALIB_DIR=ralib
readonly RALIB_DIR
# Folder to store the experimental results
RESULTS_DIR=results/palindrome
readonly RESULTS_DIR
# Folder to store tables
OUTPUT_DIR=results
readonly OUTPUT_DIR
# Configurable options
mem="default"
out="${RESULTS_DIR}"

declare -A algs=(
    [slstar]="SLStar"
    [sllambda]="SLLambda"
    [slleq]="SLLambdaEq"
)

alg=""
max_n="2"
n=""
csv="palindrome.csv"
opt_even=0


print_usage() {
  echo "Usage $0 [-a|--alg alg][-m|--mem mem][-n n][-mn|--max-n max_n][-o|--out out][-e|--even][-h|--help]"
  echo "Where:"
  echo " alg is an algorithm in ${!algs[@]};"
  echo " mem is the amount of heap allocated (e.g., 124m, 150g, etc.);"
  echo " n learn palindrome language of size n;"
  echo " max_n learn palindrome languages up to size max_n;"
  echo " out is the output directory where to store the results;"
  echo " -e|--even runs for only even values of n;"
  echo " -h|--help prints usage instructions and exits gracefully."
}
while [[ $# -gt 0 && "$1" =~ ^- ]]; do case $1 in
    -a | --alg )
        shift;
        alg=$1
        ;;
    -m | --mem )
        shift;
        mem=$1
        ;;
    -o | --out )
        shift;
        out=$1
        ;;
    -n )
        shift;
        n=$1
        ;;
    -mn | --max-n )
        shift;
        max_n=$1
        ;;
    -e | --even )
        opt_even=1
        ;;
    -h | --help )
        print_usage
        exit 0
        ;;
    * )
        break
        ;;
    esac; shift; done

if [[ $# > 0 ]]; then
  echo "Invalid arguments"
  print_usage
  exit 0
fi

sel_algs=""
if [[ -n ${alg} ]]; then
  if [[ " ${!algs[*]} " == *" $alg "* ]]; then
    echo "Running for algorithm $alg"
    sel_algs=("$alg")
  else 
    echo "Invalid algorithm $alg"
    exit 1
  fi
else 
  echo "Running for all algorithms"
  sel_algs=("${!algs[@]}")
fi

# build command part responsible for configuring JVM memory
maven_opts=""
if [[ ${mem} != "default" ]]; then
  maven_opts="-Xms$mem -Xmx$mem"
fi


# creating result folder if it does not exist
mkdir -p $out

echo "Running benchmark using memory setting $mem and result folder $out"

# run palindrome benchmark for each algorithm
for alg in "${sel_algs[@]}"; do
  test_method="#testLearnPalindrome${algs[$alg]}"
  if [[ -n ${n} ]]; then
    MAVEN_OPTS="$maven_opts" mvn test -f $RALIB_DIR -Dtest=LearnPalindromeTest$test_method -Dpalindrome.size=$n -Dpalindrome.benchmark=true > $out/palindrome-size-$n-"${algs[$alg]}".log 2>&1
  else
    # run palindrome benchmark for increasing palindrome sizes, break once an experiment fails
    for ((i=1; i<=$max_n; i++)); do
      # skip if n is odd and even option is enabled
      if [[ "$opt_even" -eq 0 || $((i % 2)) -eq 0 ]]; then
        echo "Running experiments for size $i using algorithm $alg"
        MAVEN_OPTS="$maven_opts" mvn test -f $RALIB_DIR -Dtest=LearnPalindromeTest$test_method -Dpalindrome.size=$i -Dpalindrome.benchmark=true > $out/palindrome-size-$i-"${algs[$alg]}".log 2>&1
        if [ $? -ne 0 ]; then
          break
        fi
      fi
    done
  fi
done

echo "Benchmark executed, results (logs) stored in ${out}"
echo "Analyzing logs and storing statistics in ${OUTPUT_DIR}/${csv}"

mkdir -p ${OUTPUT_DIR}

python3 scripts/extract_stats_palindrome.py --log-dir "${out}" --output-csv "${OUTPUT_DIR}"/"${csv}"