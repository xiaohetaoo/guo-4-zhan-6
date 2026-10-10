# -*- coding: utf-8 -*-
"""Explore the downloaded open datasets to plan the words.db build."""
import json, csv, os, collections

DATA = r"C:\Users\67153\AppData\Local\AndroidBuild\data"
csv.field_size_limit(10 ** 9)

# ---- 1. tb_vocabulary ----
voc = json.load(open(os.path.join(DATA, "tb_vocabulary.json"), encoding="utf-8"))
print("tb_vocabulary:", type(voc), len(voc))
print("  sample:", json.dumps(voc[0], ensure_ascii=False))

# ---- 2. tb_voc_examples ----
ex = json.load(open(os.path.join(DATA, "tb_voc_examples.json"), encoding="utf-8"))
print("tb_voc_examples:", type(ex), len(ex))
print("  sample:", json.dumps(ex[0], ensure_ascii=False))

# index examples by wordid
by_word = collections.defaultdict(list)
for e in ex:
    by_word[e["wordid"]].append(e)
print("  distinct wordids with examples:", len(by_word))
cnt = collections.Counter(len(v) for v in by_word.values())
print("  examples-per-word distribution (top):", sorted(cnt.items())[:10], "...max", max(cnt))

# word -> wordid map
w2id = {}
for v in voc:
    w2id.setdefault(v["spelling"].lower(), v["wordid"])

# ---- 3. ECDICT CET words ----
cet4, cet6, allw = set(), set(), {}
with open(os.path.join(DATA, "ecdict.csv"), encoding="utf-8", newline="") as f:
    for row in csv.DictReader(f):
        w = (row.get("word") or "").strip()
        if not w:
            continue
        t = (row.get("tag") or "").split()
        if "cet4" in t:
            cet4.add(w.lower())
        if "cet6" in t:
            cet6.add(w.lower())
print("ECDICT cet4:", len(cet4), " cet6:", len(cet6), " overlap:", len(cet4 & cet6))

# ---- 4. coverage ----
def cov(name, s):
    have_voc = sum(1 for w in s if w in w2id)
    have_ex = sum(1 for w in s if w in w2id and w2id[w] in by_word)
    print(f"  {name}: n={len(s)} in_tb_vocabulary={have_voc} ({have_voc*100//max(1,len(s))}%) with_examples={have_ex} ({have_ex*100//max(1,len(s))}%)")

print("coverage:")
cov("cet4", cet4)
cov("cet6", cet6)
cov("cet4|cet6", cet4 | cet6)

# ---- 5. book1/3 coverage ----
vb = json.load(open(os.path.join(DATA, "tb_voc_book.json"), encoding="utf-8"))
id2w = {v["wordid"]: v["spelling"].lower() for v in voc}
for bid in (1, 3):
    ws = {id2w[x["wordid"]] for x in vb if x["bookid"] == bid}
    print(f"book{bid} words={len(ws)}  in_cet4={len(ws & cet4)}  in_cet6={len(ws & cet6)}  in_either={len(ws & (cet4|cet6))}")
