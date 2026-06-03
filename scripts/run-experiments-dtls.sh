#!/usr/bin/env bash

learners=("slstar" "sllambda" "slleq")

for learner in "${learners[@]}"; do
  ./run_series.sh -s dtls -i 1 -r "$learner"
done
