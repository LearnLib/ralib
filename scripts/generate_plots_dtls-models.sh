#!/usr/bin/env bash

./generate_summary_dtls.sh > plots/dtls-models-summary-generated.csv

head -n 1 plots/dtls-summary-generated.csv > plots/dtls-models-mc.csv
less plots/dtls-summary-generated.csv | grep "dtls" >> plots/dtls-models-mc.csv

cd plots

latexmk -pdf plot-dtls-resets.tex

cd ..
