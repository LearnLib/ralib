#!/usr/bin/env python3
"""Generate PDF from synthetic models CSV.
"""

import csv
from table_pdf import ALGORITHMS, escape, number, row, run


SULS = {
    "CHANNEL_FRAME": ("channel-frame", 5, 8, 3, 2),
    "ABP_RECEIVER3": ("abp-receiver3", 6, 10, 3, 2),
    "LOGIN": ("login", 12, 19, 4, 0),
    "FIFO7": ("fifo7", 24, 32, 8, 0),
    "ABP_OUTPUT": ("abp-output", 30, 50, 1, 2),
    "SIP": ("sip", 30, 72, 2, 0),
}


def render(path):
    models = {}
    current = None
    with path.open(newline="", encoding="utf-8-sig") as source:
        for cells in csv.reader(source, delimiter=";"):
            cells = [c.strip() for c in cells]
            if not cells or not any(cells):
                continue
            if cells[0].lower() not in ("slstar", "sllambda", "sllambdaeq"):
                current = cells[0]
                if current not in SULS:
                    raise ValueError(f"No paper SUL metadata for {current}")
                headers = cells[1:]
                models[current] = {}
            else:
                if current is None:
                    raise ValueError("Algorithm row precedes model header")
                models[current][cells[0].upper()] = dict(zip(headers, cells[1:]))
    if not models:
        raise ValueError("No synthetic models found")
    lines = [r"\begin{table}[t!]",
             r"\caption{Results on Synthetic Models from Automata Wiki.}",
             r"\label{tbl:autwiki}", r"\centering", r"\resizebox{.6\textwidth}{!}{",
             r"\begin{tabular}{lrrcc@{\ }|@{\ }rrr@{\ }|@{\ }rrr@{\ }}",
             r"\toprule",
             row([r"\multicolumn{5}{l}{\textbf{SUL}}",
                  r"\multicolumn{3}{@{\ }c@{\ }}{\textbf{Tests (Constr.)}}",
                  r"\multicolumn{3}{@{\ }c@{\ }}{\textbf{Tests (Total)}}"]),
             row(["", "$|Q|$", "$|GAMMA|$", "$|X|$", "$|C|$"] +
                 [r"\multicolumn{1}{@{\ }c@{\ }}{" + a + "}" for a in ALGORITHMS] * 2),
             r"\midrule"]
    for key, metadata in SULS.items():
        if key not in models:
            continue
        cells = [escape(metadata[0])] + [number(n) for n in metadata[1:]]
        for metric in ("Hyp Construction Resets", "Total Resets"):
            values = [int(models[key][a][metric]) for a in ALGORITHMS]
            cells.extend(r"\textbf{" + str(v) + "}" if v == min(values) else str(v)
                         for v in values)
        lines.append(row(cells))
    lines.extend([r"\bottomrule", r"\end{tabular}", "}", r"\end{table}"])
    return "\n".join(lines)


if __name__ == "__main__":
    run("synthetic-models.csv", render, __doc__)
