#!/usr/bin/env bash

#learners=("slstar" "sllambda" "sllambdaeq")
learners=("sllambda" "sllambdaeq")

cd scripts
for learner in "${learners[@]}"; do
  ./run_series.sh -s dtls-models -i 1 -l "$learner"
done

./generate_summary_dtls-models.sh > plots/dtls-models.csv

cd plots

latexmk -pdf plot-dtls-models-tests.tex

cd ../..

mv scripts/plots/dtls-models.csv results/dtls-models.csv
mv scripts/plots/plot-dtls-models-tests.pdf results/plot-dtls-models-tests.pdf