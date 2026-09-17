#!/usr/bin/env bash

#learners=("slstar" "sllambda" "sllambdaeq")
learners=("sllambda" "sllambdaeq")

cd scripts
for learner in "${learners[@]}"; do
  ./run_series.sh -s dtls-models -i 1 -l "$learner"
done

./generate_summary_dtls-models.sh > plots/dtls-models-summary-generated.csv

head -n 1 plots/dtls-models-summary-generated.csv > plots/dtls-models-mc.csv
less plots/dtls-models-summary-generated.csv | grep "dtls" >> plots/dtls-models-mc.csv

cd plots

latexmk -pdf plot-dtls-models-resets.tex

cd ../..

mv scripts/plots/plot-dtls-models-resets.pdf results/plot-dtls-models-resets.pdf