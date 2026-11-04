# -*- coding: utf-8 -*-
"""词库文本修复：只处理「确定的字符/笔误错误」，不动词条增删、不动 rank。

范围（与用户确认）：
  A. 音标里的 ASCII 冒号 ':' → IPA 长音符 'ː'（仅限 /…/ 内部）
  B. 西里尔字母 'ә'（U+04D9）→ 拉丁 'ə'（U+0259）
  C. 组合下加点 '̣'（U+0323）→ 删除（原始数据里的多余修饰符）
  D. 音标里用作重音的 ASCII 撇号 ' → ˈ（仅限 /…/ 内部，且其后紧跟元音/辅音）
  E. 「挡案」→「档案」、「的的」→「的」、录相→录像
  F. 例句中的 U+FFFD 乱码 → ASCII 撇号 '

刻意不做的：
  · 不给 36 个空音标补值（其中 b.c./dvd/p.m. 等缩写本身无音标，补错更糟）
  · 不删 reservior / uptodate 这类重复或错拼词条 —— 删了会让 rank 断号，
    而闯关重放依赖 rank 顺序不变
  · 不改任何释义措辞（除了明确的笔画错误 f）

用法：python tools/fix_text.py [--dry]
"""
import collections
import os
import re
import sqlite3
import sys

def _resolve(cli_key, env_key, default):
    for arg in sys.argv[1:]:
        if arg.startswith(cli_key + "="):
            return arg.split("=", 1)[1]
    return os.environ.get(env_key) or default


_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB = _resolve("DB", "CETVOCAB_WORDS_DB",
              os.path.join(_ROOT, "app", "src", "main", "assets", "words.db"))
if not os.path.isfile(DB):
    sys.exit("找不到词库：%s  用 DB=<路径> 指过来" % DB)
DRY = "--dry" in sys.argv

# ---- 音标内部替换表 ----
INNER_REPLACE = [
    (":", "ː"),          # A 长音符
    ("\u04d9", "ə"),      # B 西里尔 schwa
    ("\u0060", "ˈ"),      # 反引号冒充重音
]
# 刻意不做批量替换的几类，理由记在这里：
#   · '.'（U+002E）：inclination 的 /ˌɪn.klɪˈneɪ.ʃən/ 里它是**合法**的音节分隔符，
#     而 /.disprә'pɒ:ʃәnit/ 这类只是数据源把分隔符写在了词首。
#     统一删掉会破坏合法用法，统一保留又留着词首的点 —— 两种都不对，
#     所以这一类留作人工判断，不放进自动修复。
#   · U+0323（组合下加点，5 处）：多为「重音位置标错」的痕迹，
#     直接删掉会留下一个看着正常、实际重音不对的音标，同样人工处理。
#   · '^'：全库检查后并不存在于音标内部，不需要规则。


# ---- 整字段笔误表（只改中文里的错字）----
TYPO = [
    ("挡案", "档案"),
    ("的的", "的"),
    ("录相磁带", "录像磁带"),
]

# ---- 例句乱码 ----
MOJIBAKE = [
    ("\ufffd \ufffd s ", "'s "),   # "Bowman � � s" -> "Bowman's"
    ("\ufffd", "'"),
]

PHON_RE = re.compile(r"/[^/]*/")


def fix_phonetic(p):
    """只处理 /…/ 内部，避免把字段名「英 / 美 /」里的斜杠当音标。"""
    if not p:
        return p, 0
    count = 0

    def do_body(m):
        nonlocal count
        body = m.group(0)
        original = body
        for bad, good in INNER_REPLACE:
            if bad in body:
                body = body.replace(bad, good)
        # D：ASCII 撇号当重音。只替换「撇号 + 音素」这种组合，
        # 避免误伤 /'fɑ:ðәrinlɔ:/ 结尾那种（结尾的 ' 后面没有音素时不动）
        new_body, n = re.subn(r"'(?=[a-zæɑɒɔəɜɛɪʊʌʃʒθðŋɚɡɱɲ])", "ˈ", body)
        body = new_body
        if body != original:
            count += 1
        return body

    out = PHON_RE.sub(do_body, p)
    return out, count


def main():
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.execute("SELECT id, word, phonetic, translation, trans_detail, definition,"
                " sentence, sentence_cn, mnemonic, similar, roots FROM words")
    rows = cur.fetchall()
    stats = collections.Counter()
    updates = []

    for (wid, word, ph, tr, td, df, se, sc, mn, si, ro) in rows:
        nph, c = fix_phonetic(ph)
        if c:
            stats["音标"] += 1
        ntr, ntd, ndf, nse, nsc = tr or "", td or "", df or "", se or "", sc or ""
        for bad, good in TYPO:
            for name, val in (("translation", ntr), ("trans_detail", ntd),
                              ("definition", ndf), ("sentence_cn", nsc)):
                if bad in val:
                    if name == "translation":
                        ntr = val.replace(bad, good)
                    elif name == "trans_detail":
                        ntd = val.replace(bad, good)
                    elif name == "definition":
                        ndf = val.replace(bad, good)
                    else:
                        nsc = val.replace(bad, good)
                    stats[f"笔误 {bad}"] += 1
        for bad, good in MOJIBAKE:
            if bad in nse:
                nse = nse.replace(bad, good)
                stats["例句乱码"] += 1
            if bad in nsc:
                nsc = nsc.replace(bad, good)
                stats["例句乱码"] += 1
        if (nph, ntr, ntd, ndf, nse, nsc) != (ph or "", tr or "", td or "",
                                              df or "", se or "", sc or ""):
            updates.append((wid, nph, ntr, ntd, ndf, nse, nsc))

    print("待更新行数:", len(updates))
    for k, v in stats.items():
        print(f"  {k}: {v}")

    if DRY:
        print("\n(--dry) 未写入")
        return

    for wid, nph, ntr, ntd, ndf, nse, nsc in updates:
        cur.execute("UPDATE words SET phonetic=?, translation=?, trans_detail=?, "
                    "definition=?, sentence=?, sentence_cn=? WHERE id=?",
                    (nph, ntr, ntd, ndf, nse, nsc, wid))
    con.commit()
    print("\n已写入并提交")


if __name__ == "__main__":
    main()
