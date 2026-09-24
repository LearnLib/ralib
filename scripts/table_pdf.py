"""Shared CSV-to-LaTeX helpers; requires Python 3 and pdflatex with booktabs."""

import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile


ALGORITHMS = ("SLSTAR", "SLLAMBDA", "SLLAMBDAEQ")


def escape(value):
    replacements = {"\\": r"\textbackslash{}", "&": r"\&", "%": r"\%",
                    "$": r"\$", "#": r"\#", "_": r"\_", "{": r"\{",
                    "}": r"\}", "~": r"\textasciitilde{}", "^": r"\textasciicircum{}"}
    return "".join(replacements.get(c, c) for c in str(value))


def number(value, commas=False):
    if value is None or not str(value).strip():
        return "--"
    value = int(value)
    return format(value, ",d" if commas else "d")


def ratio(numerator, denominator):
    # The paper truncates percentages rather than rounding them.
    if not numerator or not denominator or int(denominator) == 0:
        return "--"
    return str(100 * int(numerator) // int(denominator)) + r"\%"


def row(cells):
    return " & ".join(cells) + r" \\"


def run(default_csv, render, description, palindrome=False):
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("csv", nargs="?", type=Path,
                        default=Path(__file__).resolve().parent / default_csv)
    parser.add_argument("--output-dir", type=Path, help="Defaults to the CSV directory")
    parser.add_argument("--tex-only", action="store_true", help="Write LaTeX without compiling")
    if palindrome:
        parser.add_argument("--all-rows", action="store_true", help="Include sizes omitted in the paper")
    args = parser.parse_args()
    try:
        body = render(args.csv, **({"all_rows": args.all_rows} if palindrome else {}))
        document = (r"\documentclass{article}" + "\n"
                    r"\usepackage[a4paper,margin=15mm]{geometry}" + "\n"
                    r"\usepackage{booktabs,graphicx}" + "\n"
                    r"\pagestyle{empty}" + "\n"
                    r"\begin{document}" + "\n" + body + "\n"
                    r"\end{document}" + "\n")
        output = (args.output_dir or args.csv.resolve().parent).resolve()
        output.mkdir(parents=True, exist_ok=True)
        tex = output / (args.csv.stem + ".tex")
        tex.write_text(document, encoding="utf-8")
        print(f"LaTeX: {tex}")
        if args.tex_only:
            return
        compiler = shutil.which("pdflatex")
        if not compiler:
            parser.exit(1, "pdflatex is required to generate the PDF; LaTeX was saved.\n")
        with tempfile.TemporaryDirectory(prefix="csv-table-") as tmp:
            result = subprocess.run(
                [compiler, "-interaction=nonstopmode", "-halt-on-error", "-no-shell-escape",
                 "-output-directory", tmp, str(tex)],
                cwd=tmp, capture_output=True, text=True)
            if result.returncode:
                log = output / (args.csv.stem + ".log")
                log.write_text(result.stdout + result.stderr, encoding="utf-8")
                parser.exit(1, f"LaTeX compilation failed; see {log}\n")
            pdf = output / (args.csv.stem + ".pdf")
            shutil.copyfile(Path(tmp) / pdf.name, pdf)
        print(f"PDF: {pdf}")
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"Error: {error}\n")
