#!/usr/bin/env bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Configurable options
mem=""
out="${SCRIPT_DIR}/palindrome_results"
alg=""

print_usage() {
  echo "Usage $0 [-a|--alg {SLStar|SLLambda|SLLeq}][-m|--mem mem][-o|--out out] max_size_palindrome"
  echo "Where:"
  echo " alg is the algorithm;"
  echo " mem is the amount of heap allocated;"
  echo " out is the output directory where to store the results."
}

if [[ $# == 0 ]]; then
  print_usage
  exit 0
fi

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
    * )
        break
        ;;
    esac; shift; done

if [[ $# == 0 ]]; then
  echo "Mandatory max_size_palindrome value not provided"
  print_usage
  exit 0
fi

max_size=$1

test_method=""
case "$alg" in
  SLStar|SLLambda|SLLEq)
    test_method="#testLearnPalindrome$alg"
    ;;
  *)
    echo "Algorithm not provided, not supported or invalid"
    echo "Running for all algorithms"
    alg="all"
    ;;
esac

maven_opts=""
if [[ -n ${mem} ]]; then
  maven_opts="-Xms$mem -Xmx$mem"
fi

for ((i=1; i<=$max_size; i++)); do
  echo "Running experiments for size $i using algorithm(s) $alg, memory setting $mem and output folder $out"
  echo MAVEN_OPTS="\"$maven_opts\"" mvn test -Dtest=LearnPalindromeTest$test_method -DargLine="$jvm_args" -Dpalindrome.size=$i
  MAVEN_OPTS="$maven_opts" mvn test -Dtest=LearnPalindromeTest$test_method -Dpalindrome.size=$i > $out/palindrome-size-$i-$alg.txt 2>$1
  if [ $? -ne 0 ]; then
    break
  fi
done

echo "Benchmark executed"
echo "Logs stored in palindrome_results"
