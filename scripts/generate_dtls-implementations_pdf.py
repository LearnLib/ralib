#!/usr/bin/env python3
"""Generate PDF from DTLS implementations CSV.
"""

import csv
from decimal import Decimal, InvalidOperation
from pathlib import Path
import sys

# Support both this working directory and installation beside table_pdf.py.
if not Path(__file__).with_name("table_pdf.py").is_file():
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))
from table_pdf import ALGORITHMS, escape, number, row, run


NAMES = {
    "mbedtls-2.26.0_server": "Mbed TLS 2.26.0",
    "openssl-1.1.1k_server": "OpenSSL 1.1.1k",
    "piondtls-2.0.9_server": "Pion 2.0.9",
    "scandium-2.6.2_server": "Scandium 2.6.2",
    "wolfssl-5.7.6_server": "WolfSSL 5.7.6",
}


def decimal_text(value):
    if not value:
        return "--"
    try:
        parsed = Decimal(value)
    except InvalidOperation as error:
        raise ValueError(f"Invalid numeric value: {value!r}") from error
    if not parsed.is_finite() or parsed < 0:
        raise ValueError(f"Expected a finite nonnegative value: {value!r}")
    return format(parsed.normalize(), "f")


def read_records(path):
    with path.open(newline="", encoding="utf-8-sig") as source:
        rows = [[c.strip() for c in cells] for cells in csv.reader(source) if any(c.strip() for c in cells)]
    if len(rows) < 3:
        raise ValueError("Expected two header rows and at least one result row")
    groups, headers = rows[:2]
    if headers[0] != "SUTs" or len(groups) != len(headers):
        raise ValueError("Expected the grouped DTLS implementations CSV headers")
    starts = [i for i, name in enumerate(headers) if name == "Finished"]
    mappings = {}
    for start, end in zip(starts, starts[1:] + [len(headers)]):
        labels = [name for name in groups[start:end] if name]
        if len(labels) != 1 or labels[0] not in ALGORITHMS or labels[0] in mappings:
            raise ValueError(f"Invalid algorithm group: {labels}")
        mapping = {headers[i]: i for i in range(start, end)}
        required = {"Finished", "States", "Transitions", "Tests", "Time"}
        if labels[0] == "SLLAMBDAEQ":
            required |= {"Ratio Tests", "Ratio Time"}
        if not required <= mapping.keys():
            raise ValueError(f"Missing columns for {labels[0]}")
        mappings[labels[0]] = mapping
    if not mappings:
        raise ValueError("No algorithm groups found")
    records = []
    for line, cells in enumerate(rows[2:], 3):
        if len(cells) != len(headers):
            raise ValueError(f"CSV row {line}: expected {len(headers)} columns, got {len(cells)}")
        record = {a: {field: cells[i] for field, i in mapping.items()}
                  for a, mapping in mappings.items()}
        for values in record.values():
            if values["Finished"].lower() not in ("", "true", "false"):
                raise ValueError(f"CSV row {line}: invalid Finished value")
        records.append((cells[0], record))
    return records


def render(path):
    lines = [r"\begin{table}[t!]",
             r"\caption{Results on learning RA models of DTLS servers; ratio is SLLAMBDAEQ/SLLAMBDA.}",
             r"\label{tbl:dtls-bb}", r"\centering", r"\resizebox{.9\textwidth}{!}{",
             r"\begin{tabular}{l@{\ }|@{\ }rrr@{\ }|@{\ }rrr@{\ }|@{\ }rrrr@{\ }|@{\ }rrrr@{\ }}",
             r"\toprule",
             row([r"\multicolumn{1}{l}{}"] +
                 [r"\multicolumn{" + str(n) + r"}{@{\ }c@{\ }}{\textbf{" + title + "}}"
                  for n, title in [(3, "$|Q|$"), (3, "$|GAMMA|$"),
                                   (4, "Tests (Total)"), (4, "Time (min.)")]]),
             row([r"\multicolumn{1}{l}{\textbf{SUL}}"] +
                 [cell for with_ratio in (False, False, True, True)
                  for cell in ([r"\multicolumn{1}{@{\ }c@{\ }}{" + a + "}" for a in ALGORITHMS]
                               + (["ratio"] if with_ratio else []))]),
             r"\midrule"]
    incomplete = False
    for name, record in read_records(path):
        cells = [escape(NAMES.get(name, name))]
        for metric in ("States", "Transitions", "Tests", "Time"):
            for algorithm in ALGORITHMS:
                values = record.get(algorithm, {})
                if values.get("Finished", "").lower() == "false":
                    cells.append("INCOMPLETE")
                    incomplete = True
                else:
                    value = values.get(metric, "")
                    cells.append(decimal_text(value) if metric == "Time" else number(value))
            if metric in ("Tests", "Time"):
                value = record.get("SLLAMBDAEQ", {}).get("Ratio " + metric, "")
                finished = all(record.get(a, {}).get("Finished", "").lower() == "true"
                               for a in ("SLLAMBDA", "SLLAMBDAEQ"))
                cells.append(decimal_text(value) + r"\%" if value and finished else "--")
        lines.append(row(cells))
    lines.extend([r"\bottomrule", r"\end{tabular}", "}"])
    if incomplete:
        lines.append(r"\par\smallskip\footnotesize INCOMPLETE denotes a run that did not finish.")
    lines.append(r"\end{table}")
    return "\n".join(lines)


if __name__ == "__main__":
    run("dtls-implementations.csv", render, __doc__)
