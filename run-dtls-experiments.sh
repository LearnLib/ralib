#!/usr/bin/env bash

learners=("slstar" "sllambda" "sllambdaeq")

cd scripts

for learner in "${learners[@]}"; do
  ./run_series.sh -s dtls -i 1 -l "$learner"
done

./generate-summary-dtls.sh > plots/dtls-summary-generated.csv

head -n 1 plots/dtls-summary-generated.csv > plots/dtls-models-mc.csv
less plots/dtls-summary-generated.csv | grep "dtls" >> plots/dtls-models-mc.csv

cd plots

latexmk -pdf plot-dtls-resets.tex
latexmk -pdf plot-dtls-resets-withstar.tex
latexmk -pdf plot-dtls-counterexamples.tex
latexmk -pdf plot-dtls-counterexamples-withstar.tex

cd ../..

mv scripts/plots/plot-dtls-resets.pdf plot-dtls-resets.pdf
mv scripts/plots/plot-dtls-resets-withstar.pdf plot-dtls-resets-withstar.pdf
mv scripts/plots/plot-dtls-counterexamples.pdf plot-dtls-counterexamples.pdf
mv scripts/plots/plot-dtls-counterexamples-withstar.pdf plot-dtls-counterexamples-withstar.pdf
