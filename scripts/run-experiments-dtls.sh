#!/usr/bin/env bash

learners=("slstar" "sllambda" "sllambdaeq")

for learner in "${learners[@]}"; do
  ./run_series.sh -s dtls -i 1 -r "$learner"
done
