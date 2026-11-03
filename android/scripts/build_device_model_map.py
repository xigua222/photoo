import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE_FILES = [
    "apple_all.md",
    "samsung_cn.md",
    "xiaomi.md",
    "huawei_cn.md",
    "honor_cn.md",
    "meizu.md",
    "oppo_cn.md",
    "vivo_cn.md",
    "oneplus.md",
    "realme_cn.md",
]
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "device_model_market_map.json"
PAIR_RE = re.compile(r"^((?:`[^`\n]+`\s*)+)\s*[:：]\s*(.+)$")
CODE_RE = re.compile(r"`([^`\n]+)`")


def decode_text(data: bytes) -> str:
    for enc in ("utf-8", "utf-8-sig", "gb18030", "latin-1"):
        try:
            return data.decode(enc)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="ignore")


def build_map() -> dict[str, str]:
    merged: dict[str, str] = {}
    for rel in SOURCE_FILES:
        path = ROOT / rel
        if not path.exists():
            continue
        text = decode_text(path.read_bytes())
        for line in text.splitlines():
            match = PAIR_RE.search(line.strip())
            if not match:
                continue
            codes_block = match.group(1)
            name = match.group(2).strip()
            if not name:
                continue
            codes = [code.strip().upper() for code in CODE_RE.findall(codes_block)]
            for code in codes:
                if not code:
                    continue
                merged.setdefault(code, name)
    return merged


if __name__ == "__main__":
    model_map = build_map()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(model_map, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generated {len(model_map)} entries -> {OUTPUT}")
