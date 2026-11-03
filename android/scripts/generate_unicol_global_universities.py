#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import re
import sys
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path


REPO_ZIP_URL = "https://codeload.github.com/hneth/unicol/zip/refs/heads/main"
USER_AGENT = "Codex-Unicol-Importer/1.0"
SKIP_FILES = {
    "0_all_pals.R",
    "1_empty_pal.R",
    "2_example_pal.R",
    "5_util.R",
    "9_start_unicol.R",
}

NAMED_COLORS = {
    "aqua": "#00FFFF",
    "black": "#000000",
    "blue": "#0000FF",
    "brown": "#A52A2A",
    "cyan": "#00FFFF",
    "fuchsia": "#FF00FF",
    "gold": "#FFD700",
    "gray": "#808080",
    "green": "#008000",
    "grey": "#808080",
    "lime": "#00FF00",
    "magenta": "#FF00FF",
    "maroon": "#800000",
    "navy": "#000080",
    "olive": "#808000",
    "orange": "#FFA500",
    "pink": "#FFC0CB",
    "purple": "#800080",
    "red": "#FF0000",
    "silver": "#C0C0C0",
    "teal": "#008080",
    "white": "#FFFFFF",
    "yellow": "#FFFF00",
}

INSTITUTION_RE = re.compile(r'^\s*#\s*inst:\s*"([^"]+)"', re.MULTILINE)
INSTITUTION_ALT_RE = re.compile(r'^\s*#\s*inst_alt:\s*"([^"]+)"', re.MULTILINE)
COUNTRY_RE = re.compile(r"^\s*#\s*country:\s*(.+?)\s*$", re.MULTILINE)
PALETTE_HEADER_RE = re.compile(r"^\s*#\s*-\s*([A-Za-z][A-Za-z0-9_]*)\s*:\s*(.*?)\s*-+\s*$", re.MULTILINE)
ASSIGNMENT_START_RE = re.compile(r"^\s*([A-Za-z][A-Za-z0-9_]*)\s*<-\s*(.+?)\s*$")
NEWPAL_START_RE = re.compile(r"^\s*([A-Za-z][A-Za-z0-9_]*)\s*<-\s*unikn::newpal\s*\(", re.MULTILINE)
RGB_CALL_RE = re.compile(r"(?:grDevices::)?rgb\s*\((.*)\)\s*$", re.DOTALL)
REV_CALL_RE = re.compile(r"(?:base::)?rev\s*\((.*)\)\s*$", re.DOTALL)
SHADES_OF_CALL_RE = re.compile(r"(?:unikn::)?shades_of\s*\((.*)\)\s*$", re.DOTALL)
USECOL_CALL_RE = re.compile(r"(?:unikn::)?usecol\s*\((.*)\)\s*$", re.DOTALL)
AS_VECTOR_CALL_RE = re.compile(r"(?:base::)?as\.vector\s*\((.*)\)\s*$", re.DOTALL)
INDEX_SUFFIX_RE = re.compile(r"^(.*?)(\[[^\]]+\])\s*$", re.DOTALL)

EXTERNAL_COLOR_EXPRESSIONS = {
    "unikn::pal_unikn": (
        'c("#008ECE", "#00A9E0", "#59C7EB", "#A6E1F4", "#CCEEF9", '
        '"#FFFFFF", "#E5E5E5", "#CCCCCC", "#999999", "#666666", "#000000")'
    ),
    "unikn::pal_unikn_pref": (
        'c("#59C7EB", "#E0607E", "#0A9086", "#FEA090", "#3E5496", '
        '"#EFDC60", "#8E2043", "#9AA0A7", "#077187")'
    ),
}


@dataclass
class PaletteEntry:
    key: str
    colors: list[str]


def fetch_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def load_palette_files() -> list[tuple[str, str]]:
    archive = zipfile.ZipFile(io.BytesIO(fetch_bytes(REPO_ZIP_URL)))
    files: list[tuple[str, str]] = []
    for member_name in sorted(archive.namelist()):
        member_path = Path(member_name)
        if len(member_path.parts) < 3 or member_path.parts[-2] != "R":
            continue
        if member_path.suffix != ".R" or member_path.name in SKIP_FILES:
            continue
        raw_bytes = archive.read(member_name)
        try:
            raw_text = raw_bytes.decode("utf-8")
        except UnicodeDecodeError:
            raw_text = raw_bytes.decode("latin-1")
        files.append((member_path.name, raw_text))
    return files


def strip_comments(text: str) -> str:
    result: list[str] = []
    in_string = False
    quote_char = ""
    escape = False
    i = 0
    while i < len(text):
        ch = text[i]
        if in_string:
            result.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote_char:
                in_string = False
            i += 1
            continue
        if ch in {'"', "'"}:
            in_string = True
            quote_char = ch
            result.append(ch)
            i += 1
            continue
        if ch == "#":
            while i < len(text) and text[i] not in "\r\n":
                i += 1
            continue
        result.append(ch)
        i += 1
    return "".join(result)


def count_grouping_delta(text: str) -> int:
    delta = 0
    in_string = False
    quote_char = ""
    escape = False
    for ch in text:
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote_char:
                in_string = False
            continue
        if ch in {'"', "'"}:
            in_string = True
            quote_char = ch
            continue
        if ch in "([{":
            delta += 1
        elif ch in ")]}":
            delta -= 1
    return delta


def find_matching_paren(text: str, open_paren_index: int) -> int:
    depth = 0
    in_string = False
    quote_char = ""
    escape = False
    for index in range(open_paren_index, len(text)):
        ch = text[index]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote_char:
                in_string = False
            continue
        if ch in {'"', "'"}:
            in_string = True
            quote_char = ch
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("unmatched parenthesis")


def split_top_level_csv(text: str) -> list[str]:
    items: list[str] = []
    start = 0
    paren_depth = 0
    bracket_depth = 0
    brace_depth = 0
    in_string = False
    quote_char = ""
    escape = False
    for index, ch in enumerate(text):
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote_char:
                in_string = False
            continue
        if ch in {'"', "'"}:
            in_string = True
            quote_char = ch
            continue
        if ch == "(":
            paren_depth += 1
            continue
        if ch == ")":
            paren_depth = max(paren_depth - 1, 0)
            continue
        if ch == "[":
            bracket_depth += 1
            continue
        if ch == "]":
            bracket_depth = max(bracket_depth - 1, 0)
            continue
        if ch == "{":
            brace_depth += 1
            continue
        if ch == "}":
            brace_depth = max(brace_depth - 1, 0)
            continue
        if ch == "," and paren_depth == 0 and bracket_depth == 0 and brace_depth == 0:
            items.append(text[start:index].strip())
            start = index + 1
    tail = text[start:].strip()
    if tail:
        items.append(tail)
    return items


def resolve_named_color(name: str) -> str | None:
    lowered = name.strip().lower()
    if lowered in NAMED_COLORS:
        return NAMED_COLORS[lowered]
    gray_match = re.fullmatch(r"gr[ae]y(\d{1,3})", lowered)
    if gray_match:
        percent = max(0, min(100, int(gray_match.group(1))))
        channel = round((percent / 100.0) * 255.0)
        return "#{0:02X}{0:02X}{0:02X}".format(channel)
    return None


def normalize_color_literal(value: str) -> str | None:
    token = value.strip().strip('"').strip("'")
    if not token:
        return None
    if token.startswith("#"):
        hex_token = token[1:]
        if re.fullmatch(r"[0-9A-Fa-f]{6}", hex_token):
            return f"#{hex_token.upper()}"
        if re.fullmatch(r"[0-9A-Fa-f]{3}", hex_token):
            expanded = "".join(ch * 2 for ch in hex_token.upper())
            return f"#{expanded}"
    return resolve_named_color(token)


def parse_variable_assignments(cleaned_text: str) -> dict[str, str]:
    assignments: dict[str, str] = {}
    current_name: str | None = None
    current_parts: list[str] = []
    current_delta = 0

    for raw_line in cleaned_text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if current_name is not None:
            current_parts.append(line)
            current_delta += count_grouping_delta(line)
            if current_delta <= 0:
                assignments[current_name] = " ".join(current_parts).strip()
                current_name = None
                current_parts = []
                current_delta = 0
            continue

        match = ASSIGNMENT_START_RE.match(raw_line)
        if match is None:
            continue
        name = match.group(1)
        expression = match.group(2).strip()
        if expression.startswith("function("):
            continue
        current_name = name
        current_parts = [expression]
        current_delta = count_grouping_delta(expression)
        if current_delta <= 0:
            assignments[current_name] = expression
            current_name = None
            current_parts = []
            current_delta = 0

    return assignments


def extract_named_argument_expression(block: str, argument_name: str) -> str | None:
    marker = re.search(rf"\b{re.escape(argument_name)}\s*=\s*", block)
    if marker is None:
        return None
    start = marker.end()
    paren_depth = 0
    bracket_depth = 0
    brace_depth = 0
    in_string = False
    quote_char = ""
    escape = False
    for index in range(start, len(block)):
        ch = block[index]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote_char:
                in_string = False
            continue
        if ch in {'"', "'"}:
            in_string = True
            quote_char = ch
            continue
        if ch == "(":
            paren_depth += 1
            continue
        if ch == ")":
            paren_depth = max(paren_depth - 1, 0)
            continue
        if ch == "[":
            bracket_depth += 1
            continue
        if ch == "]":
            bracket_depth = max(bracket_depth - 1, 0)
            continue
        if ch == "{":
            brace_depth += 1
            continue
        if ch == "}":
            brace_depth = max(brace_depth - 1, 0)
            continue
        if ch == "," and paren_depth == 0 and bracket_depth == 0 and brace_depth == 0:
            return block[start:index].strip()
    return block[start:].strip()


def parse_index_values(index_body: str) -> list[int]:
    body = index_body.strip()
    if not body:
        return []
    if body.startswith("c(") and body.endswith(")"):
        return [
            value
            for token in split_top_level_csv(body[2:-1])
            if (value := int(token.strip())) > 0
        ]
    if ":" in body:
        parts = [part.strip() for part in body.split(":", 1)]
        if len(parts) == 2 and parts[0].isdigit() and parts[1].isdigit():
            start = int(parts[0])
            end = int(parts[1])
            step = 1 if end >= start else -1
            return list(range(start, end + step, step))
    if body.isdigit():
        return [int(body)]
    return []


def apply_vector_index(colors: list[str], index_expression: str | None) -> list[str]:
    if not index_expression:
        return colors
    body = index_expression.strip()[1:-1]
    indexes = parse_index_values(body)
    if not indexes:
        return colors
    result: list[str] = []
    for index in indexes:
        if 1 <= index <= len(colors):
            result.append(colors[index - 1])
    return result


def hex_to_rgb(color: str) -> tuple[int, int, int]:
    normalized = color.removeprefix("#")
    return (
        int(normalized[0:2], 16),
        int(normalized[2:4], 16),
        int(normalized[4:6], 16),
    )


def rgb_to_hex(red: float, green: float, blue: float) -> str:
    return "#{:02X}{:02X}{:02X}".format(
        round(max(0.0, min(255.0, red))),
        round(max(0.0, min(255.0, green))),
        round(max(0.0, min(255.0, blue))),
    )


def interpolate_palette(colors: list[str], count: int) -> list[str]:
    if not colors or count <= 0:
        return []
    if len(colors) == 1 or count == 1:
        return [colors[0]]
    if count == len(colors):
        return colors
    if len(colors) == 2:
        return interpolate_colors(colors[0], colors[1], count)

    result: list[str] = []
    palette_span = len(colors) - 1
    for index in range(count):
        position = (index / (count - 1)) * palette_span
        left_index = int(position)
        right_index = min(left_index + 1, len(colors) - 1)
        factor = position - left_index
        left_rgb = hex_to_rgb(colors[left_index])
        right_rgb = hex_to_rgb(colors[right_index])
        result.append(
            rgb_to_hex(
                left_rgb[0] + ((right_rgb[0] - left_rgb[0]) * factor),
                left_rgb[1] + ((right_rgb[1] - left_rgb[1]) * factor),
                left_rgb[2] + ((right_rgb[2] - left_rgb[2]) * factor),
            )
        )
    return result


def interpolate_colors(start_color: str, end_color: str, count: int) -> list[str]:
    if count <= 1:
        return [start_color]
    start_rgb = hex_to_rgb(start_color)
    end_rgb = hex_to_rgb(end_color)
    colors: list[str] = []
    for index in range(count):
        factor = index / (count - 1)
        colors.append(
            rgb_to_hex(
                start_rgb[0] + ((end_rgb[0] - start_rgb[0]) * factor),
                start_rgb[1] + ((end_rgb[1] - start_rgb[1]) * factor),
                start_rgb[2] + ((end_rgb[2] - start_rgb[2]) * factor),
            )
        )
    return colors


def resolve_rgb_expression(expression: str) -> str | None:
    match = RGB_CALL_RE.fullmatch(expression.strip())
    if match is None:
        return None
    positional: list[float] = []
    max_color_value = 1.0
    for token in split_top_level_csv(match.group(1)):
        if "=" in token:
            key, value = token.split("=", 1)
            if key.strip() == "maxColorValue":
                try:
                    max_color_value = float(value.strip())
                except ValueError:
                    return None
            continue
        try:
            positional.append(float(token.strip()))
        except ValueError:
            return None
    if len(positional) < 3 or max_color_value <= 0:
        return None
    scale = 255.0 / max_color_value
    return rgb_to_hex(positional[0] * scale, positional[1] * scale, positional[2] * scale)


def resolve_color_expression(
    expression: str,
    variables: dict[str, str],
    stack: set[str] | None = None,
) -> list[str]:
    stack = stack or set()
    stripped = expression.strip()
    if not stripped:
        return []

    index_match = INDEX_SUFFIX_RE.match(stripped)
    index_expression: str | None = None
    if index_match is not None:
        stripped = index_match.group(1).strip()
        index_expression = index_match.group(2)

    literal = normalize_color_literal(stripped)
    if literal is not None:
        return apply_vector_index([literal], index_expression)

    rgb_literal = resolve_rgb_expression(stripped)
    if rgb_literal is not None:
        return apply_vector_index([rgb_literal], index_expression)

    c_match = re.fullmatch(r"c\s*\((.*)\)\s*", stripped, re.DOTALL)
    if c_match is not None:
        colors: list[str] = []
        for token in split_top_level_csv(c_match.group(1)):
            colors.extend(resolve_color_expression(token, variables, stack.copy()))
        return apply_vector_index(colors, index_expression)

    rev_match = REV_CALL_RE.fullmatch(stripped)
    if rev_match is not None:
        colors = resolve_color_expression(rev_match.group(1), variables, stack.copy())
        return apply_vector_index(list(reversed(colors)), index_expression)

    as_vector_match = AS_VECTOR_CALL_RE.fullmatch(stripped)
    if as_vector_match is not None:
        colors = resolve_color_expression(as_vector_match.group(1), variables, stack.copy())
        return apply_vector_index(colors, index_expression)

    usecol_match = USECOL_CALL_RE.fullmatch(stripped)
    if usecol_match is not None:
        args = split_top_level_csv(usecol_match.group(1))
        positional: list[str] = []
        named: dict[str, str] = {}
        for arg in args:
            if "=" in arg:
                key, value = arg.split("=", 1)
                named[key.strip()] = value.strip()
            else:
                positional.append(arg.strip())
        palette_source = named.get("pal") or (positional[0] if positional else "")
        palette_colors = resolve_color_expression(palette_source, variables, stack.copy())
        if not palette_colors:
            return []
        count_source = named.get("n") or (positional[1] if len(positional) > 1 else "")
        if count_source:
            try:
                count = int(float(count_source))
            except ValueError:
                count = len(palette_colors)
            colors = interpolate_palette(palette_colors, count)
        else:
            colors = palette_colors
        return apply_vector_index(colors, index_expression)

    shades_match = SHADES_OF_CALL_RE.fullmatch(stripped)
    if shades_match is not None:
        args = split_top_level_csv(shades_match.group(1))
        positional: list[str] = []
        named: dict[str, str] = {}
        for arg in args:
            if "=" in arg:
                key, value = arg.split("=", 1)
                named[key.strip()] = value.strip()
            else:
                positional.append(arg.strip())
        if positional:
            try:
                count = int(float(positional[0]))
            except ValueError:
                count = 0
        else:
            count = int(float(named.get("n", "5")))
        start_candidates = (
            resolve_color_expression(positional[1], variables, stack.copy())
            if len(positional) > 1
            else resolve_color_expression(named.get("col_1", '"black"'), variables, stack.copy())
        )
        end_source = named.get("col_n") or (positional[2] if len(positional) > 2 else '"white"')
        end_candidates = resolve_color_expression(end_source, variables, stack.copy())
        if count > 0 and start_candidates and end_candidates:
            colors = interpolate_colors(start_candidates[0], end_candidates[0], count)
            return apply_vector_index(colors, index_expression)
        return []

    if stripped.startswith("unikn::newpal"):
        open_paren = stripped.find("(")
        if open_paren >= 0:
            close_paren = find_matching_paren(stripped, open_paren)
            block = stripped[open_paren + 1:close_paren]
            col_expression = extract_named_argument_expression(block, "col")
            if col_expression:
                colors = resolve_color_expression(col_expression, variables, stack.copy())
                return apply_vector_index(colors, index_expression)

    external_expression = EXTERNAL_COLOR_EXPRESSIONS.get(stripped)
    if external_expression is not None:
        colors = resolve_color_expression(external_expression, variables, stack.copy())
        return apply_vector_index(colors, index_expression)

    if stripped in stack:
        return []
    if stripped in variables:
        stack.add(stripped)
        colors = resolve_color_expression(variables[stripped], variables, stack)
        return apply_vector_index(colors, index_expression)

    return []


def humanize_palette_id(palette_id: str) -> str:
    text = palette_id.replace("_", " ").strip()
    return re.sub(r"\s+", " ", text)


def sanitize_key(text: str) -> str:
    cleaned = text.replace(":", " ").replace('"', "'").replace("\t", " ")
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return cleaned


def parse_palette_file(name: str, raw_text: str) -> list[PaletteEntry]:
    if "unikn::newpal" not in raw_text or "# inst:" not in raw_text:
        return []

    institution_match = INSTITUTION_ALT_RE.search(raw_text) or INSTITUTION_RE.search(raw_text)
    institution = institution_match.group(1).strip() if institution_match else Path(name).stem
    country_match = COUNTRY_RE.search(raw_text)
    country = country_match.group(1).strip() if country_match else ""

    palette_titles = {
        match.group(1): sanitize_key(match.group(2))
        for match in PALETTE_HEADER_RE.finditer(raw_text)
    }
    cleaned_text = strip_comments(raw_text)
    variables = parse_variable_assignments(cleaned_text)

    entries: list[PaletteEntry] = []
    for match in NEWPAL_START_RE.finditer(cleaned_text):
        palette_id = match.group(1)
        open_paren = cleaned_text.find("(", match.end() - 1)
        close_paren = find_matching_paren(cleaned_text, open_paren)
        block = cleaned_text[open_paren + 1:close_paren]
        col_expression = extract_named_argument_expression(block, "col")
        if col_expression is None:
            continue
        resolved_colors = resolve_color_expression(col_expression, variables)
        deduped_colors = list(dict.fromkeys(resolved_colors))
        if not deduped_colors:
            continue
        palette_title = palette_titles.get(palette_id, humanize_palette_id(palette_id))
        key_parts = [institution, palette_title]
        if country:
            key_parts.append(country)
        key = sanitize_key(" · ".join(part for part in key_parts if part))
        entries.append(PaletteEntry(key=key, colors=deduped_colors))
    return entries


def build_yaml(entries: list[PaletteEntry]) -> str:
    lines = [
        "# Auto-generated from https://github.com/hneth/unicol/tree/main",
        "# Topic: global universities (全球高校)",
        "# Do not edit by hand; rerun scripts/generate_unicol_global_universities.py",
        "",
    ]
    used_keys: dict[str, int] = {}
    for entry in sorted(entries, key=lambda item: item.key.casefold()):
        index = used_keys.get(entry.key, 0) + 1
        used_keys[entry.key] = index
        key = entry.key if index == 1 else f"{entry.key} · {index}"
        lines.append(f"{key}:")
        for color in entry.colors:
            lines.append(f'  - "{color}"')
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output",
        default="unicol_global_universities.yml",
        help="output YAML file path",
    )
    args = parser.parse_args()

    entries: list[PaletteEntry] = []
    failures: list[str] = []
    for file_name, raw_text in load_palette_files():
        try:
            entries.extend(parse_palette_file(file_name, raw_text))
        except Exception as exc:  # pragma: no cover - operational fallback
            failures.append(f"{file_name}: {exc}")

    if not entries:
        print("No palettes parsed from unicol.", file=sys.stderr)
        if failures:
            print("\n".join(failures), file=sys.stderr)
        return 1

    output_path = Path(args.output)
    output_path.write_text(build_yaml(entries), encoding="utf-8")

    print(f"Generated {len(entries)} palette entries to {output_path}")
    if failures:
        print(f"Skipped {len(failures)} files:", file=sys.stderr)
        print("\n".join(failures), file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
