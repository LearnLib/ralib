#!/usr/bin/env bash

learners=("slstar" "sllambda" "sllambdaeq")

cd scripts

for learner in "${learners[@]}"; do
  ./run_series.sh -s synthetic -i 1 -l "$learner"
done

./collate_results.sh > synthetic.csv

cd ..

mv scripts/synthetic.csv results/synthetic-models.csv

