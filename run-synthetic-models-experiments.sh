#!/usr/bin/env bash

learners=("slstar" "sllambda" "slleq")

cd scripts

for learner in "${learners[@]}"; do
  ./run_series.sh -s synthetic -i 1 -l "$learner"
done

./collate_results.sh > synthetic.csv

cd ..

mv scripts/synthetic.csv synthetic.csv

