import re
import csv
import argparse
from pathlib import Path
from typing import Callable, Pattern

SLL="SLLambda"
SLLAMBDAEQ="SLLambdaEq"
SLSTAR="SLStar"

ALGS = [SLL, SLLAMBDAEQ, SLSTAR]
# Regex patterns
ALG_RE = re.compile(r".*(SLLambda|SLLambdaEq|SLStar).*")
TOTAL_RESETS_RE = re.compile(r"Total:\s*\{[^}]*Resets:\s*(\d+)")
CE_ANALYSIS_RESETS_RE = re.compile(r"CE Analysis:\s*\{[^}]*Resets:\s*(\d+)")
CE_RE = re.compile(r"Counterexamples:\s*(\d+)")
LOC_RE = re.compile(r"Hyp.\sLocations:\s*(\d+)")
TRANS_RE = re.compile(r"Hyp.\sTransitions:\s*(\d+)")
SIZE_RE = re.compile(r".*-(\d+)(-(SLLambda|SLLambdaEq|SLStar|all))?\.log")



def extract_result_statistic(log_path: Path, alg: str, extract: Callable[[str],str]) -> str:
    """
    Uses the callable to extract result statistic for one algorithm from one log file.
    """
    current_section = False
    statistic = None
    with log_path.open(encoding="utf-8") as f:
        for line in f:
            header_match = ALG_RE.search(line)
            if header_match:
                section = header_match.group(1)
                if section == alg:
                    current_section = True
                continue
            if current_section:
                statistic = extract(line)
                if statistic is not None:
                    return statistic
    return statistic

def extract_algs(log_path: Path) -> set:
    algs = set()
    with log_path.open(encoding="utf-8") as f:
        for line in f:
            for alg in ALGS:
                if alg in line:
                    algs.add(alg)
    return algs

def extract_result_statistic_with_pattern(log_path: Path, alg: str, pat) -> str:
    """
    Uses pattern to extract statistic for alg from log file at path.
    """

    def extract(line:str) -> str:
        reset_match = pat.search(line)
        if reset_match:
            return reset_match.group(1)
        else:
            return None
    
    stat_str = extract_result_statistic(log_path, alg, extract)
    return stat_str


def extract_total_resets(log_path: Path, alg: str):
    """
    Extract total reset counts for alg from one log file.
    """

    num_resets = extract_result_statistic_with_pattern(log_path, alg, TOTAL_RESETS_RE)
    return num_resets

def extract_ces(log_path: Path, alg: str):
    """
    Extract ce count for alg from one log file.
    """

    num_ces = extract_result_statistic_with_pattern(log_path, alg, CE_RE)
    return num_ces

def extract_ce_analysis_resets(log_path: Path, alg: str):
    """
    Extract ce analysis reset counts for alg from one log file.
    """

    num_resets = extract_result_statistic_with_pattern(log_path, alg, CE_ANALYSIS_RESETS_RE)
    return num_resets

def process_logs(log_dir: Path):
    """
    Process all *.log files in the given directory.
    """
    rows = []
    size_results = {}

    for log_file in sorted(log_dir.glob("*.log")):
        print(log_file.name)
        size_match = SIZE_RE.search(log_file.name)
        size = int(size_match.group(1))
        if size not in size_results:
            size_results[size] = {"Size": size}
        results = size_results[size]
        algs = sorted(extract_algs(log_file))
        if "Locations" not in results or results["Locations"] is None:
            for (stat, stat_re) in [("Locations", LOC_RE), ("Transitions", TRANS_RE)]:
                for alg in algs:
                    results[stat] = extract_result_statistic_with_pattern(log_file, alg, stat_re) 
                    if results[stat] is not None:
                        break

        for alg in algs:
            results[alg +"TotalResets"] = extract_total_resets(log_file, alg)
            results[alg +"CeAnalysisResets"] = extract_ce_analysis_resets(log_file, alg)
            if results[alg +"TotalResets"] is not None and results[alg +"CeAnalysisResets"] is not None:
                results[alg +"HypConResets"] = int(results[alg +"TotalResets"].strip()) - int(results[alg +"CeAnalysisResets"].strip())
            results[alg +"Ces"] = extract_ces(log_file, alg)
            
    for size in sorted(size_results):
        rows.append(size_results[size])
    
    #sorted_rows = sorted(rows, key=lambda row: row["Size"])
        

    return rows

def write_csv(csv_path: Path, rows):
    """
    Write aggregated results to CSV.
    """
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    with csv_path.open("w", newline="", encoding="utf-8") as f:
        columns=["Size", "Locations", "Transitions"]
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