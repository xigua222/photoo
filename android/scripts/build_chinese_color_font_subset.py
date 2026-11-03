from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

from openpyxl import load_workbook


DEFAULT_OUTPUT_FONT = Path("app/src/main/assets/watermark_fonts/zhengqingke_chinese_colors_subset.ttf")
DEFAULT_OUTPUT_CHARSET = Path("app/src/main/assets/watermark_fonts/zhengqingke_chinese_colors_charset.txt")
DEFAULT_OUTPUT_REPORT = Path("app/src/main/assets/watermark_fonts/zhengqingke_chinese_colors_subset_report.json")
DEFAULT_OUTPUT_PALETTE_JSON = Path("app/src/main/assets/watermark_chinese_colors.json")
DEFAULT_EXTRA_TEXT = (
    "\u4e2d\u56fd\u4f20\u7edf\u8272"
    "\u4e2d\u56fd\u8272\u8272\u540d"
    "CMYKRGBHEX"
    "#():/-\u00b7 "
    "0123456789"
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build subset assets for the Chinese color watermark template.")
    parser.add_argument("--xlsx", type=Path, help="Workbook path. Defaults to the only xlsx file in the repo root.")
    parser.add_argument("--font", type=Path, help="Source font path. Defaults to auto-discovered TTF/OTF font.")
    parser.add_argument("--out-font", type=Path, default=DEFAULT_OUTPUT_FONT)
    parser.add_argument("--out-charset", type=Path, default=DEFAULT_OUTPUT_CHARSET)
    parser.add_argument("--out-report", type=Path, default=DEFAULT_OUTPUT_REPORT)
    parser.add_argument("--out-palette-json", type=Path, default=DEFAULT_OUTPUT_PALETTE_JSON)
    return parser.parse_args()


def resolve_workbook(explicit_path: Path | None) -> Path:
    if explicit_path:
        if explicit_path.exists():
            return explicit_path
        raise FileNotFoundError(f"Workbook not found: {explicit_path}")

    matches = sorted(Path(".").glob("*.xlsx"))
    if len(matches) == 1:
        return matches[0]
    if not matches:
        raise FileNotFoundError("No xlsx file found in repo root.")

    preferred = [path for path in matches if any(token in path.stem for token in ("色", "传统", "中国"))]
    if len(preferred) == 1:
        return preferred[0]
    raise FileNotFoundError(
        f"Found multiple xlsx files in repo root: {[str(path) for path in matches]}. Pass --xlsx to select one."
    )


def resolve_font(explicit_path: Path | None) -> Path:
    if explicit_path:
        if explicit_path.exists():
            return explicit_path
        raise FileNotFoundError(f"Font not found: {explicit_path}")

    candidates = sorted(Path(".").rglob("*.ttf")) + sorted(Path(".").rglob("*.otf"))
    if not candidates:
        raise FileNotFoundError("No TTF/OTF font file found under the repo.")

    preferred = [path for path in candidates if "ZhengQingKe" in path.name or any(token in path.as_posix() for token in ("中国", "宋体"))]
    pool = preferred or candidates
    if len(pool) == 1:
        return pool[0]
    return max(pool, key=lambda path: path.stat().st_size)


def normalize_header(value: object) -> str:
    text = "" if value is None else str(value)
    return "".join(text.strip().lower().split())


def read_color_rows(xlsx_path: Path) -> list[dict[str, object]]:
    workbook = load_workbook(xlsx_path, read_only=True, data_only=True)
    try:
        sheet = workbook.worksheets[0]
        rows = sheet.iter_rows(values_only=True)
        header = next(rows, None)
        if not header:
            raise ValueError("Workbook is empty.")

        header_map = {normalize_header(cell): index for index, cell in enumerate(header)}
        required = {
            "name": "name",
            "c": "c",
            "m": "m",
            "y": "y",
            "k": "k",
            "r": "r",
            "g": "g",
            "b": "b",
        }
        missing = [column for column in required if column not in header_map]
        if missing:
            raise ValueError(f"Missing workbook columns: {missing}")

        page_index = header_map.get("page")
        records: list[dict[str, object]] = []
        for row in rows:
            if not row:
                continue
            name = str(row[header_map["name"]]).strip() if row[header_map["name"]] is not None else ""
            if not name:
                continue
            r = int(float(row[header_map["r"]] or 0))
            g = int(float(row[header_map["g"]] or 0))
            b = int(float(row[header_map["b"]] or 0))
            record = {
                "name": name,
                "hex": f"#{r:02X}{g:02X}{b:02X}",
                "rgb": {"r": r, "g": g, "b": b},
                "cmyk": {
                    "c": int(float(row[header_map["c"]] or 0)),
                    "m": int(float(row[header_map["m"]] or 0)),
                    "y": int(float(row[header_map["y"]] or 0)),
                    "k": int(float(row[header_map["k"]] or 0)),
                },
            }
            if page_index is not None and page_index < len(row) and row[page_index] is not None:
                record["page"] = int(float(row[page_index]))
            records.append(record)

        if not records:
            raise ValueError("No color rows found in workbook.")
        return records
    finally:
        workbook.close()


def build_charset(color_rows: list[dict[str, object]]) -> str:
    ordered_chars: list[str] = []
    seen: set[str] = set()
    texts = [str(item["name"]) for item in color_rows] + [DEFAULT_EXTRA_TEXT]
    for text in texts:
        for char in text:
            if char not in seen:
                seen.add(char)
                ordered_chars.append(char)
    return "".join(ordered_chars)


def run_subset(font_path: Path, output_font_path: Path, charset: str) -> None:
    output_font_path.parent.mkdir(parents=True, exist_ok=True)
    command = [
        "py",
        "-3",
        "-m",
        "fontTools.subset",
        str(font_path),
        f"--output-file={output_font_path}",
        f"--text={charset}",
        "--layout-features=*",
        "--glyph-names",
        "--symbol-cmap",
        "--legacy-cmap",
        "--notdef-glyph",
        "--notdef-outline",
        "--recommended-glyphs",
        "--name-IDs=*",
        "--name-legacy",
        "--name-languages=*",
        "--drop-tables=",
        "--desubroutinize",
    ]
    subprocess.run(command, check=True)


def write_palette_json(color_rows: list[dict[str, object]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "version": 1,
        "count": len(color_rows),
        "colors": color_rows,
    }
    output_path.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


def build_report(
    *,
    xlsx_path: Path,
    font_path: Path,
    output_font_path: Path,
    output_charset_path: Path,
    output_palette_json_path: Path,
    color_rows: list[dict[str, object]],
    charset: str,
) -> dict[str, object]:
    return {
        "xlsx": str(xlsx_path),
        "font": str(font_path),
        "out_font": str(output_font_path),
        "out_charset": str(output_charset_path),
        "out_palette_json": str(output_palette_json_path),
        "total_color_names": len(color_rows),
        "unique_color_names": len({str(item["name"]) for item in color_rows}),
        "unique_characters": len(charset),
        "source_font_bytes": font_path.stat().st_size,
        "subset_font_bytes": output_font_path.stat().st_size,
        "subset_ratio": round(output_font_path.stat().st_size / font_path.stat().st_size, 6),
        "color_name_samples": [str(item["name"]) for item in color_rows[:12]],
    }


def main() -> None:
    args = parse_args()
    xlsx_path = resolve_workbook(args.xlsx)
    font_path = resolve_font(args.font)

    color_rows = read_color_rows(xlsx_path)
    charset = build_charset(color_rows)

    args.out_charset.parent.mkdir(parents=True, exist_ok=True)
    args.out_charset.write_text(charset, encoding="utf-8")
    write_palette_json(color_rows, args.out_palette_json)
    run_subset(font_path, args.out_font, charset)

    report = build_report(
        xlsx_path=xlsx_path,
        font_path=font_path,
        output_font_path=args.out_font,
        output_charset_path=args.out_charset,
        output_palette_json_path=args.out_palette_json,
        color_rows=color_rows,
        charset=charset,
    )
    args.out_report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
