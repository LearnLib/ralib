#!/usr/bin/env bash

# ###########################################################
#
# PARSE INPUT

usage="
Usage: $(basename "$0") [-h] -s series -i iterations -l learner
  where:
    -s series folder in experiments
    -i number of iterations
    -l learner (none, sllambda or efficient)
"

series=""
iterations=""
learners=""
while getopts 'hs:i:l:' option; do
  case "$option" in
    h) echo "$usage"
       exit
       ;;
    s) series=$OPTARG
       ;;
    i) iterations=$OPTARG
       ;;
    l) learners=$OPTARG
       ;;
    :) printf "missing argument for -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
   \?) printf "illegal option: -%s\n" "$OPTARG" >&2
       echo "$usage" >&2
       exit 1
       ;;
  esac
done

if [ -z "$series" -o -z "$iterations" -o -z "$learners" ]; then
  echo "$usage" >&2
  exit 1
fi


# EXECUTE
echo "=================================================="
echo "= "
echo "= $series" 
echo "= "
echo "=================================================="

for experiment in $(find ../experiments/"$series" | grep .xml | sed 's/.xml//' | sed 's/.*\///')
do

  echo "== Running: $experiment"
  ./run_experiment.sh -s "$series" -e "$experiment" -i "$iterations" -l "$learners"

  ./search_logs.sh "$series" "$experiment" "$learners"

done
