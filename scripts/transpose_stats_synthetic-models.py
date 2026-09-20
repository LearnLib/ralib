#!/usr/bin/env python3
"""Transpose statistics for obtained for synthetic experiments.

This representation can be more convenient, e.g., when using this data to fill up Latex tables.
"""


import csv
import sys


def transform_csv(input_file, output_file):
    with open(input_file, newline="", encoding="utf-8") as f:
        reader = csv.reader(f, delimiter=";")
        rows = [row for row in reader if row]

    output_rows = []
    header = None

    i = 0
    while i < len(rows):
        system_name = rows[i][0]
        metrics = rows[i][1:]

        slstar_row = rows[i + 1]
        sllambda_row = rows[i + 2]
        sllambdaeq_row = rows[i + 3]

        if header is None:
            header = ["System"]

            for learner in ("slstar", "sllambda", "sllambdaeq"):
                for metric in metrics:
                    header.append(f"{learner}_{metric}")

            output_rows.append(header)

        output_row = [system_name]

        for learner_row in (slstar_row, sllambda_row, sllambdaeq_row):
            output_row.extend(learner_row[1:])

        output_rows.append(output_row)

        i += 4

    with open(output_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerows(output_rows)


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} INPUT_CSV OUTPUT_CSV")
        sys.exit(1)

    transform_csv(sys.argv[1], sys.argv[2])


if __name__ == "__main__":
    main()
