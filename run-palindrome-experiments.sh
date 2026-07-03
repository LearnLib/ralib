#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
RALIB_DIR=$SCRIPT_DIR/ralib

# Configurable options
mem="default"
out="${SCRIPT_DIR}/results/palindrome"
algs=("SLStar" "SLLambda" "SLLEq")
alg=""
max_size="2"
csv="palindrome.csv"


print_usage() {
  echo "Usage $0 [-a|--alg alg][-m|--mem mem][-s|--size size][-o|--out out][-h|--help]"
  echo "Where:"
  echo " alg is an algorithm in ${algs[@]};"
  echo " mem is the amount of heap allocated (e.g., 124m, 150g, etc.);"
  echo " size is the maximum size of the palindrome accepted by the language;"
  echo " out is the output directory where to store the results;"
  echo " -h|--help prints usage instructions and exits gracefully."
}
while [[ "$1" =~ ^- ]]; do case $1 in
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
    -s | --size )
        shift;
        max_size=$1
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

# creating output folder if it does not exist, removing any existing files
mkdir -p $out

if [[ -n ${alg} ]]; then
  if [[ " ${algs[*]} " == *" $alg "* ]]; then
    echo "Running for algorithm $alg"
    algs=("$alg")
  else 
    echo "Invalid algorithm $alg"
    exit 1
  fi
else 
  echo "Running for all algorithms"
fi

# build command part responsible for configuring JVM memory
maven_opts=""
if [[ ${mem} != "default" ]]; then
  maven_opts="-Xms$mem -Xmx$mem"
fi

echo "Running benchmark using memory setting $mem and output folder $out"
# run palindrome benchmark for each algorithm
for alg in ${algs[@]}; do
  test_method="#testLearnPalindrome$alg"
  # run palindrome benchmark for increasing palindrome sizes, break once an experiment fails
  for ((i=1; i<=$max_size; i++)); do
    echo "Running experiments for size $i using algorithm $alg"
    # echo MAVEN_OPTS="\"$maven_opts\"" mvn -f $RALIB_DIR test -Dtest=LearnPalindromeTest$test_method -Dpalindrome.size=$i -Dpalindrome.benchmark=true
    MAVEN_OPTS="$maven_opts" mvn test -f $RALIB_DIR -Dtest=LearnPalindromeTest$test_method -Dpalindrome.size=$i -Dpalindrome.benchmark=true > $out/palindrome-size-$i-$alg.log 2>&1
    if [ $? -ne 0 ]; then
      break
    fi
  done
done

echo "Benchmark executed, results (logs) stored in $out"
echo "Analyzing logs and storing statistics in $csv"

python3 scripts/extract_stats-palindrome.py --log-dir $out --output-csv $csv