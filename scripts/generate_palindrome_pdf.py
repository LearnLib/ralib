#!/usr/bin/env python3
"""Generate PDF from palindrome CSV.
"""

import csv
from table_pdf import ALGORITHMS, number, ratio, row, run


def render(path, all_rows=False):
    lines = [r"\begin{table}[t!]",
             r"\caption{Results on PALINDROME(n); OOM denotes out of memory; ratio is SLLAMBDAEQ/SLLAMBDA.}",
             r"\label{tbl:palindrome}", r"\centering", r"\resizebox{.99\textwidth}{!}{",
             r"\begin{tabular}{r@{\ \ }rrc@{\ }|@{\ }rrrr@{\ }|@{\ }rrrr}",
             r"\toprule",
             row([r"\multicolumn{4}{l}{}",
                  r"\multicolumn{4}{c}{\textbf{Tests (Construction)}}",
                  r"\multicolumn{4}{c}{\textbf{Tests (Total)}}"]),
             row(["$n$", "$|Q|$", "$|GAMMA|$", "$|X|$"] +
                 ([r"\multicolumn{1}{c}{" + a + "}" for a in ALGORITHMS] + ["ratio"]) * 2),
             r"\midrule"]
    with path.open(newline="", encoding="utf-8-sig") as source:
        records = sorted(csv.DictReader(source), key=lambda r: int(r["Size"]))
    count = 0
    for record in records:
        n = int(record["Size"])
        count += 1
        locations, transitions = record["Locations"], record["Transitions"]
        cells = [str(n), number(locations), number(transitions), str(n // 2)]
        for metric in ("HypConResets", "TotalResets"):
            values = [record[a + metric].strip() for a in ("SLStar", "SLLambda", "SLLambdaEq")]
            cells.extend(number(v, commas=True) if v else "OOM" for v in values)
            cells.append(ratio(values[2], values[1]))
        lines.append(row(cells))
    if not count:
        raise ValueError("No matching palindrome rows found")
    lines.extend([r"\bottomrule", r"\end{tabular}", "}", r"\end{table}"])
    return "\n".join(lines)


if __name__ == "__main__":
    run("palindrome.csv", render, __doc__, palindrome=True)
