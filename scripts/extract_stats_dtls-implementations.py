#!/usr/bin/env python3
"""Export results from ra-<algorithm>/<configuration>/ to a summary CSV.

States and transitions count occurrences of 'doublecircle' and '[label='
in learnedModel.dot. Tests and time are read from statistics.txt.

Times are converted to minutes. SLLAMBDAEQ
ratios are integer percentages of SLLAMBDA tests and displayed minutes.
Missing statistics/combinations produce empty cells. Configuration names
have the trailing _psk_epoch.
"""

import argparse
import csv
from pathlib import Path
import re
import sys
from decimal import *


FIELDS = {
    "Finished": "\\s*Learning\\sfinished:\\s(false|true)",
    "Tests": "\\s*Number\\sof\\stests:\\s(\\d*)",
    "Time": "\\s*Time\\s\\(ms\\)\\sto\\slearn\\smodel:\\s(\\d*)$"}

DECIMALS_AFTER_COMMA = 1

def read_statistics(path):
    """Read exact statistic labels, preferring the first supported alias."""
    content = path.read_text(encoding="utf-8")
    values = {}
    for field, regex in FIELDS.items():
        match = re.search(regex, content, re.MULTILINE)
        if match:
            strval = match.group(1)
            if field == "Finished":
                values[field] = strval
            else:
                if field == "Time":
                    values[field] = round(Decimal(int(strval)) / Decimal(60000), DECIMALS_AFTER_COMMA)
                else:
                    values[field] = int(strval)
        else:
            values[field] = ""
            print(f"Warning: {path}: missing value to fill field {field}", file=sys.stderr)
    return values


def read_model_statistics(path):
    """Count the literal state and transition markers in learnedModel.dot."""
    if not path.is_file():
        print(f"Warning: missing {path}", file=sys.stderr)
        return {"States": "", "Transitions": ""}
    content = path.read_text(encoding="utf-8")
    return {
        "States": content.count("doublecircle"),
        "Transitions": content.count("[label="),
    }


def percentage(value, baseline):
    return int(value) * 100 // int(baseline) if value != "" and baseline not in ("", 0) else ""


def create_csv(input_folder, output_file):
    if not input_folder.is_dir():
        raise ValueError(f"Input folder does not exist or is not a directory: {input_folder}")
    results = {}
    for algorithm_folder in sorted(input_folder.glob("ra-*")):
        if not algorithm_folder.is_dir():
            continue
        experiments = {}
        for folder in sorted(algorithm_folder.iterdir()):
            if not folder.is_dir():
                continue
            stats_file = folder / "statistics.txt"
            if not stats_file.is_file():
                print(f"Warning: missing {stats_file}", file=sys.stderr)
                continue
            experiments[folder.name] = read_statistics(stats_file)
            experiments[folder.name].update(read_model_statistics(folder / "learnedModel.dot"))
        if experiments:
            results[algorithm_folder.name[3:]] = experiments
    if not results:
        raise ValueError(f"No ra-*/<configuration>/statistics.txt files found in {input_folder}")

    algorithms = sorted(results)
    configurations = sorted({name for experiments in results.values() for name in experiments})
    group_header = [""]
    column_header = ["SUTs"]
    for algorithm in algorithms:
        columns = ["Finished", "States", "Transitions", "Tests", "Time"]
        if algorithm == "SLLAMBDAEQ":
            columns = ["Finished", "States", "Transitions", "Tests", "Ratio Tests", "Time", "Ratio Time"]
        group = [""] * len(columns)
        group[2] = algorithm
        group_header.extend(group)
        column_header.extend(columns)

    rows = [group_header, column_header]
    for configuration in configurations:
        display_name = configuration.removesuffix("_psk_epoch")
        row = [display_name]
        baseline = results.get("SLLAMBDA", {}).get(configuration, {})
        for algorithm in algorithms:
            values = results[algorithm].get(configuration, {})
            row.extend(values.get(field, "") for field in ("Finished", "States", "Transitions", "Tests"))
            if algorithm == "SLLAMBDAEQ":
                row.append(percentage(values.get("Tests", ""), baseline.get("Tests", "")))
            row.append(values.get("Time", ""))
            if algorithm == "SLLAMBDAEQ":
                row.append(percentage(values.get("Time", ""), baseline.get("Time", "")))
        rows.append(row)

    with output_file.open("w", encoding="utf-8", newline="") as stream:
        csv.writer(stream).writerows(rows)
    return len(configurations)


def main():
    #getcontext().prec = TIME_DECIMAL_PLACES
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_folder", type=Path, help="Folder containing ra-<algorithm> folders")
    parser.add_argument(
        "-o", "--output", type=Path,
        help="Output CSV path (default: dtls_<input-folder-name>.csv in the current directory)",
    )
    args = parser.parse_args()
    output = args.output or Path(f"dtls_{args.input_folder.resolve().name}.csv")
    try:
        count = create_csv(args.input_folder, output)
    except (OSError, ValueError) as error:
        parser.error(str(error))
    print(f"Wrote {count} configurations to {output}")


if __name__ == "__main__":
    main()
