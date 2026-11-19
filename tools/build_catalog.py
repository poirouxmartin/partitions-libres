#!/usr/bin/env python3
"""Builds the bundled catalogue (app/src/main/assets/catalog.json) from free sources.

Sources (lot 1):
  - OpenScore Lieder and OpenScore String Quartets (CC0 1.0, MusicXML), pinned to a commit.
  - Mutopia Project (public domain, CC BY, CC BY-SA; LilyPond-engraved PDF), one RDF per piece.

Licence policy: only licences that allow free redistribution, commercial use included, are kept:
public domain, CC0, CC BY, CC BY-SA. Anything NC or ND, or unknown, is dropped and counted.
Every piece carries its licence, licence URL, engraver, source page and original edition so the
app can show the full attribution offline. Files are linked as-is, never modified.

Usage: python -I tools/build_catalog.py [--out PATH] [--limit-mutopia N]
Network: GitHub API (5 calls, GITHUB_TOKEN used if set), raw.githubusercontent.com, mutopiaproject.org.
Mutopia RDF files are cached in tools/.cache/ so reruns are cheap.
"""
from __future__ import annotations

import argparse
import concurrent.futures as cf
import csv
import html
import io
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(__file__).resolve().parent / ".cache"
UA = "partitions-libres-catalog-builder/0.1"
MUTOPIA = "https://www.mutopiaproject.org"

CC0 = ("CC0-1.0", "CC0 1.0 (public domain)", "https://creativecommons.org/publicdomain/zero/1.0/")
OPENSCORE_ENGRAVER = "OpenScore community (MuseScore transcription)"


def http_get(url: str, retries: int = 3) -> bytes:
    headers = {"User-Agent": UA}
    if urllib.parse.urlsplit(url).hostname == "api.github.com" and os.environ.get("GITHUB_TOKEN"):
        headers["Authorization"] = "Bearer " + os.environ["GITHUB_TOKEN"]
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 404 or attempt == retries - 1:
                raise
        except urllib.error.URLError:
            if attempt == retries - 1:
                raise
        time.sleep(2 * (attempt + 1))
    raise RuntimeError("unreachable")


def gh_json(path: str):
    return json.loads(http_get("https://api.github.com/" + path))


def raw_url(repo: str, sha: str, path: str) -> str:
    return f"https://raw.githubusercontent.com/{repo}/{sha}/" + urllib.parse.quote(path)


def map_licence(text: str):
    """Maps a free-text licence to (code, label, url), or None when it is not allowed."""
    t = (text or "").strip().lower()
    if not t or re.search(r"non-?commercial|no-?deriv|\bnc\b|\bnd\b", t):
        return None
    if t in ("public domain", "domaine public"):
        return ("PD", "Public domain", f"{MUTOPIA}/legal.html#pd")
    if "cc0" in t or "publicdomain/zero" in t:
        return CC0
    m = re.fullmatch(r"creative commons attribution(-sharealike)?\s+([0-9.]+)", t)
    if m:
        kind = "by-sa" if m.group(1) else "by"
        ver = m.group(2)
        return (f"CC-{kind.upper()}-{ver}", f"CC {kind.upper()} {ver}",
                f"https://creativecommons.org/licenses/{kind}/{ver}/")
    return None


def path_name(segment: str) -> str:
    """'Schubert,_Franz' -> 'Schubert, Franz'."""
    return segment.replace("_", " ").strip()


def parse_simple_yaml(text: str) -> dict:
    """Parses the flat two-level StrictYAML files of OpenScore (id: {key: scalar})."""
    out, cur = {}, None
    for line in text.splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if not line.startswith(" "):
            cur = out.setdefault(line.split(":", 1)[0].strip().strip("'\""), {})
        elif cur is not None and ":" in line:
            k, v = line.strip().split(":", 1)
            v = v.strip()
            if len(v) >= 2 and v[0] == v[-1] and v[0] in "'\"":
                v = v[1:-1]
            cur[k.strip()] = v
    return out


def imslp_edition(ref: str | None) -> str:
    """OpenScore transcribes old editions scanned on IMSLP; only the reference is cited."""
    return f"Transcription of an old edition (IMSLP {ref})" if ref and ref.startswith("#") else ""


def repo_snapshot(repo: str):
    sha = gh_json(f"repos/{repo}/branches/main")["commit"]["sha"]
    tree = gh_json(f"repos/{repo}/git/trees/{sha}?recursive=1")
    if tree.get("truncated"):
        print(f"warning: {repo} tree truncated", file=sys.stderr)
    return sha, {p["path"] for p in tree["tree"]}


def openscore_lieder() -> list[dict]:
    repo = "OpenScore/Lieder"
    sha, files = repo_snapshot(repo)
    load = lambda f: parse_simple_yaml(http_get(raw_url(repo, sha, f)).decode("utf-8"))
    scores, sets, comps = load("data/scores.yaml"), load("data/sets.yaml"), load("data/composers.yaml")
    comp_by_path = {c.get("path"): c for c in comps.values()}
    pieces = []
    for sid, s in scores.items():
        path = s.get("path", "")
        mxl = f"scores/{path}/lc{sid}.mxl"
        if mxl not in files:
            continue
        cpath = path.split("/")[0]
        comp = comp_by_path.get(cpath, {})
        work = sets.get(s.get("set_id", ""), {}).get("name", "")
        pieces.append(piece(
            id=f"openscore-lieder-{sid}",
            title=s.get("name") or path_name(path.split("/")[-1]),
            work="" if work.startswith("Other songs by") else work,
            composer=comp.get("name") or path_name(cpath),
            composer_sort=comp.get("name_sort") or path_name(cpath),
            born=comp.get("born"), died=comp.get("died"),
            instruments=s.get("instruments") or "Voice, piano",
            lyricist=s.get("lyricist", ""),
            licence=CC0, engraver=OPENSCORE_ENGRAVER, source="OpenScore Lieder",
            source_url=s.get("link") or f"https://musescore.com/openscore-lieder-corpus/scores/{sid}",
            edition=imslp_edition(s.get("imslp")),
            musicxml_url=raw_url(repo, sha, mxl),
        ))
    return pieces


def openscore_quartets() -> list[dict]:
    repo = "OpenScore/StringQuartets"
    sha, files = repo_snapshot(repo)

    def tsv(name):
        text = http_get(raw_url(repo, sha, f"data/{name}")).decode("utf-8")
        return list(csv.DictReader(io.StringIO(text), delimiter="\t"))

    comps = {c["id"]: c for c in tsv("composers.tsv")}
    sets = {s["id"]: s for s in tsv("sets.tsv")}
    pieces = []
    for s in tsv("scores.tsv"):
        path = s["path"]
        mxl, pdf = f"scores/{path}/sq{s['id']}.mxl", f"scores/{path}/sq{s['id']}.pdf"
        if mxl not in files:
            continue
        st = sets.get(s.get("set_id", ""), {})
        comp = comps.get(st.get("composer_id", ""), {})
        cpath = path.split("/")[0]
        work = st.get("name", "")
        pieces.append(piece(
            id=f"openscore-sq-{s['id']}",
            title=s.get("name") or path_name(path.split("/")[-1]),
            work="" if work == s.get("name") else work,
            composer=comp.get("name") or path_name(cpath),
            composer_sort=path_name(cpath),
            born=comp.get("born"), died=comp.get("died"),
            instruments="String quartet",
            licence=CC0, engraver=OPENSCORE_ENGRAVER, source="OpenScore String Quartets",
            source_url=s.get("link", ""),
            edition=imslp_edition(s.get("imslp")),
            musicxml_url=raw_url(repo, sha, mxl),
            pdf_url=raw_url(repo, sha, pdf) if pdf in files else None,
        ))
    return pieces


def mutopia_composers() -> dict:
    raw = http_get("https://raw.githubusercontent.com/MutopiaProject/MutopiaWeb/master/datafiles/composers.dat")
    try:
        lines = raw.decode("utf-8").splitlines()
    except UnicodeDecodeError:
        lines = raw.decode("cp1252").splitlines()
    out = {}
    for key, label in zip(lines[0::2], lines[1::2]):
        m = re.match(r"^(.*?)\s*\((\d{3,4})?\s*[–\-]\s*(\d{3,4})?\)\s*$", label)
        out[key.strip()] = (m.group(1).strip(), m.group(2), m.group(3)) if m else (label.strip(), None, None)
    return out


def mutopia_piece_dirs() -> list[str]:
    tree = gh_json("repos/MutopiaProject/MutopiaProject/git/trees/master?recursive=1")
    if tree.get("truncated"):
        print("warning: Mutopia tree truncated", file=sys.stderr)
    dirs = set()
    for p in tree["tree"]:
        parts = p["path"].split("/")
        if parts[0] != "ftp" or p["type"] != "blob" or not parts[-1].endswith(".ly"):
            continue
        if len(parts) >= 3 and parts[-1] == parts[-2] + ".ly":
            dirs.add("/".join(parts[1:-1]))
        elif len(parts) >= 4 and parts[-2] == parts[-3] + "-lys":
            dirs.add("/".join(parts[1:-2]))
    return sorted(dirs)


def fetch_rdf(d: str) -> bytes | None:
    cached = CACHE / "mutopia" / (d.replace("/", "__") + ".rdf")
    if cached.exists():
        return cached.read_bytes() or None
    try:
        data = http_get(f"{MUTOPIA}/ftp/{d}/{d.split('/')[-1]}.rdf")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
        data = b""
    cached.parent.mkdir(parents=True, exist_ok=True)
    cached.write_bytes(data)
    return data or None


MP = "{http://www.mutopiaproject.org/piece-data/0.1/}"
RDF = "{http://www.w3.org/1999/02/22-rdf-syntax-ns#}"


def mutopia(limit: int | None, stats: dict) -> list[dict]:
    comps = mutopia_composers()
    dirs = mutopia_piece_dirs()
    if limit:
        dirs = sorted(set([d for d in dirs if d.startswith("RachmaninoffS/")] + dirs[:limit]))
    with cf.ThreadPoolExecutor(max_workers=6) as pool:
        rdfs = list(pool.map(fetch_rdf, dirs))
    pieces = []
    for d, rdf in zip(dirs, rdfs):
        if not rdf:
            stats["mutopiaNoRdf"] += 1
            continue
        try:
            desc = ET.fromstring(rdf).find(RDF + "Description")
        except ET.ParseError:
            stats["mutopiaBadRdf"] += 1
            continue
        f = lambda k: html.unescape((desc.findtext(MP + k) or "").strip())
        lic = map_licence(f("licence"))
        if not lic:
            stats["excludedLicence"] += 1
            stats["excludedLicenceTexts"].add(f("licence"))
            continue
        if not f("pdfFileA4"):
            stats["mutopiaNoPdf"] += 1
            continue
        num = f("id").rsplit("-", 1)[-1]
        ckey = f("composer")
        cname, born, died = comps.get(ckey, (ckey, None, None))
        pieces.append(piece(
            id=f"mutopia-{num}", title=f("title"), work=f("opus"),
            composer=cname,
            composer_sort=re.sub(r"([a-z])([A-Z][A-Za-z]*)$", r"\1, \2", ckey),
            born=born, died=died, instruments=f("for"), lyricist=f("lyricist"), year=f("date"),
            licence=lic, engraver=f("maintainer") or "Mutopia Project", source="Mutopia Project",
            source_url=f"{MUTOPIA}/cgibin/piece-info.cgi?id={num}",
            edition=f("source"),
            pdf_url=f"{MUTOPIA}/ftp/{d}/{f('pdfFileA4')}",
        ))
    return pieces


def piece(*, id, title, composer, composer_sort, licence, engraver, source, source_url,
          work="", born=None, died=None, instruments="", lyricist="", edition="",
          musicxml_url=None, pdf_url=None, year="") -> dict:
    code, label, url = licence
    p = {
        "id": id, "title": title, "work": work,
        "composer": composer, "composerSort": composer_sort,
        "born": int(born) if born and str(born).isdigit() else None,
        "died": int(died) if died and str(died).isdigit() else None,
        "instruments": instruments, "lyricist": lyricist, "year": year,
        "licence": code, "licenceLabel": label, "licenceUrl": url,
        "engraver": engraver, "source": source, "sourceUrl": source_url, "edition": edition,
        "musicxmlUrl": musicxml_url, "pdfUrl": pdf_url,
    }
    return {k: v for k, v in p.items() if v not in (None, "")}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(ROOT / "app/src/main/assets/catalog.json"))
    ap.add_argument("--limit-mutopia", type=int, default=None,
                    help="only fetch N Mutopia pieces (plus Rachmaninoff), for quick tests")
    args = ap.parse_args()
    stats = {"mutopiaNoRdf": 0, "mutopiaBadRdf": 0, "mutopiaNoPdf": 0,
             "excludedLicence": 0, "excludedLicenceTexts": set()}
    pieces = openscore_lieder() + openscore_quartets() + mutopia(args.limit_mutopia, stats)
    unique = list({p["id"]: p for p in pieces}.values())
    unique.sort(key=lambda p: (p["composerSort"].lower(), p["title"].lower()))
    catalog = {
        "schema": 1,
        "generated": date.today().isoformat(),
        # Display text (policy, licence names) lives in the app string resources, translated there.
        "policy": "Public domain, CC0, CC BY, CC BY-SA. Excluded: NC, ND, unknown licence.",
        "sources": [
            {"name": "OpenScore Lieder", "url": "https://github.com/OpenScore/Lieder", "licence": "CC0-1.0"},
            {"name": "OpenScore String Quartets", "url": "https://github.com/OpenScore/StringQuartets", "licence": "CC0-1.0"},
            {"name": "Mutopia Project", "url": MUTOPIA, "licence": "PD / CC BY / CC BY-SA"},
        ],
        "pieces": unique,
    }
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    by_source: dict[str, int] = {}
    for p in unique:
        by_source[p["source"]] = by_source.get(p["source"], 0) + 1
    stats["excludedLicenceTexts"] = sorted(stats["excludedLicenceTexts"])
    print(json.dumps({"pieces": len(unique), "bySource": by_source, **stats}, ensure_ascii=False, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
