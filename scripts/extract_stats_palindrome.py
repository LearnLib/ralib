import re
import csv
import argparse
from pathlib import Path
from typing import Callable, Pattern

SLL = "SLLambda"
SLLAMBDAEQ = "SLLambdaEq"
SLSTAR = "SLStar"

ALGS = [SLL, SLLAMBDAEQ, SLSTAR]

# Regex patterns used to extract statistics from the contents of log files.
TOTAL_RESETS_RE = re.compile(r"Total:\s*\{[^}]*Resets:\s*(\d+)")
CE_ANALYSIS_RESETS_RE = re.compile(r"CE Analysis:\s*\{[^}]*Resets:\s*(\d+)")
CE_RE = re.compile(r"Counterexamples:\s*(\d+)")
LOC_RE = re.compile(r"Hyp.\sLocations:\s*(\d+)")
TRANS_RE = re.compile(r"Hyp.\sTransitions:\s*(\d+)")

# Regex used to extract the palindrome language size and learning algorithm from the log file name.
LOG_FILE_RE = re.compile(r"palindrome-size-(\d+)-(SLLambda|SLLambdaEq|SLStar)\.log")


def extract_result_statistic(log_path: Path, extract: Callable[[str], str]) -> str:
    """
    Uses the callable to extract result statistic from one log file.
    """
    current_section = False
    statistic = None
    with log_path.open(encoding="utf-8") as f:
        for line in f:
            statistic = extract(line)
            if statistic is not None:
                return statistic
    return statistic


def extract_result_statistic_with_pattern(log_path: Path, pat) -> str:
    """
    Uses pattern to extract statistic from log file at path.
    """

    def extract(line: str) -> str:
        reset_match = pat.search(line)
        if reset_match:
            return reset_match.group(1)
        else:
            return None

    stat_str = extract_result_statistic(log_path, extract)
    return stat_str


def process_logs(log_dir: Path):
    """
    Process all *.log files in the given directory.
    """
    rows = []
    results = {}

    for log_file in sorted(log_dir.glob("*.log")):
        log_match = LOG_FILE_RE.search(log_file.name)
        if log_match is None:
            raise ValueError(
                f"Invalid file name {log_file.name}. Name should have format {LOG_FILE_RE}."
            )
        size = int(log_match.group(1))
        if size not in results or results[size]["Locations"] is None:
            results[size] = {"Size": size}
            results[size]["Locations"] = extract_result_statistic_with_pattern(
                log_file, LOC_RE
            )
            results[size]["Transitions"] = extract_result_statistic_with_pattern(
                log_file, TRANS_RE
            )

        results_for_size = results[size]
        alg = log_match.group(2)
        results_for_size[alg + "TotalResets"] = extract_result_statistic_with_pattern(
            log_file, TOTAL_RESETS_RE
        )
        results_for_size[alg + "CeAnalysisResets"] = (
            extract_result_statistic_with_pattern(log_file, CE_ANALYSIS_RESETS_RE)
        )
        if (
            results_for_size[alg + "TotalResets"] is not None
            and results_for_size[alg + "CeAnalysisResets"] is not None
        ):
            results_for_size[alg + "HypConResets"] = int(
                results_for_size[alg + "TotalResets"].strip()
            ) - int(results_for_size[alg + "CeAnalysisResets"].strip())
        results_for_size[alg + "Ces"] = extract_result_statistic_with_pattern(
            log_file, CE_RE
        )

    for size in sorted(results):
        rows.append(results[size])

    return rows


def write_csv(csv_path: Path, rows):
    """
    Write aggregated results to CSV.
    """
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    with csv_path.open("w", newline="", encoding="utf-8") as f:
        columns = ["Size", "Locations", "Transitions"]
        columns.extend([alg + "TotalResets" for alg in ALGS])
        columns.extend([alg + "CeAnalysisResets" for alg in ALGS])
        columns.extend([alg + "HypConResets" for alg in ALGS])
        columns.extend([alg + "Ces" for alg in ALGS])
        writer = csv.DictWriter(
            f,
            fieldnames=columns,
        )
        writer.writeheader()
        writer.writerows(rows)


def parse_args():
    parser = argparse.ArgumentParser(
        description="Extract reset statistics from log files and aggregate into a CSV."
    )
    parser.add_argument(
        "--log-dir",
        type=Path,
        default=Path("results/palindrome"),
        help="Directory containing log files (default: ./results/palindrome)",
    )
    parser.add_argument(
        "--output-csv",
        type=Path,
        default=Path("palindrome.csv"),
        help="Output CSV file path (default: ./palindrome.csv)",
    )
    return parser.parse_args()


def main():
    args = parse_args()

    if not args.log_dir.is_dir():
        raise FileNotFoundError(f"Log directory not found: {args.log_dir}")

    rows = process_logs(args.log_dir)
    write_csv(args.output_csv, rows)
    print(f"Stored statistics for {len(rows)} log files in {args.output_csv}")


if __name__ == "__main__":
    main()
