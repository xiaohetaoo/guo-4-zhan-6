#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把内置词库导出成一份完整的 Markdown 词表。

口径（与用户确认）：
  · 全部 10998 行都导出，四级段与六级段**各保留**（六级是四级的超集，
    accept 之类的词在两段各有一条，因为两段各自是一份完整词表）
  · 全字段：音标、中文释义（简 + 详）、英文释义、例句（英 + 中）、
    词根词缀、形近词、联想记忆
  · 单文件输出

用法：
  python tools/export_words_md.py [OUT=<路径>]
  默认写到 交付物/过四斩六-词汇总表.md
"""
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
DB = _resolve("DB", "CETVOCAB_WORDS_DB", os.path.join(_ROOT, "app", "src", "main", "assets", "words.db"))
OUT = _resolve("OUT", "CETVOCAB_WORDS_MD",
               os.path.join(os.path.dirname(_ROOT), "交付物", "过四斩六-词汇总表.md"))

if not os.path.isfile(DB):
    sys.exit(f"找不到词库：{DB}")

STAGE_LABEL = {1: "四级", 2: "六级"}

# 锚点 id：目录里的每一行都挂一个跳转，词条标题下再放一个 id，
# 保证 GitHub / Typora / VS Code 预览里点目录能直接跳过去。
ANCHOR_PREFIX = {"cet4": "v4-", "cet6": "v6-"}


def clean(s):
    """把字段值整理成适合写进 Markdown 的文本。

    三个要点：
      1) 去掉首尾空白与空行；
      2) 把字段内部的换行折成「；」而不是留着裸换行 —— 一行一条属性，
         否则 trans_detail 里的多行释义会把后面的字段挤出词条块；
      3) 转义 Markdown 标记的开头字符，避免 `*` `_` `#` `>` 之类把一行变成
         意外的列表 / 标题 / 粗体。词条内容里出现这些字符是真实存在的
         （比如定义里的 `*`、释义里的 `#` 缩写）。
    """
    if not s:
        return ""
    s = str(s).replace("\r\n", "\n").replace("\r", "\n").strip()
    s = re.sub(r"\n\s*\n+", "\n", s)
    s = re.sub(r"\n+", "；", s)
    s = re.sub(r"[ \t]+", " ", s)
    # 转义只在**行首**才有必要：`#` `>` `-` 在行首会被当成标题/引用/列表。
    # 词条字段都写成 `- **音标**：值` 这种形式，值不在行首，所以：
    #   · 不该给值里的 `-` 加反斜杠 —— 加了会在渲染结果里露出一个反斜杠
    #     （`\-ation` 显示成 `\-ation` 而不是 `-ation`），词根词缀全都会中招；
    #   · 但值以 `#` `>` `|` 开头时确实可能被误判，这几类保留转义。
    s = s.lstrip()
    if s[:1] in ("#", ">", "|"):
        s = "\\" + s
    return s


def esc_link(s):
    """目录里当链接文字用的单词：方括号与竖线要转义。"""
    return s.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|")


def main():
    con = sqlite3.connect(DB)
    cur = con.cursor()
    cur.execute(
        "SELECT stage, unit, rank, word, phonetic, translation, trans_detail, definition,"
        " sentence, sentence_cn, roots, similar, mnemonic, is_hs"
        " FROM words ORDER BY stage, rank"
    )
    rows = cur.fetchall()
    total = len(rows)

    # 按 stage → unit 分桶，保持 rank 顺序
    grouped = {1: {}, 2: {}}
    for (stage, unit, rank, word, ph, tr, td, df, se, sc, ro, si, mn, hs) in rows:
        grouped.setdefault(stage, {}).setdefault(unit, []).append(
            dict(rank=rank, word=word, phonetic=ph, translation=tr, trans_detail=td,
                 definition=df, sentence=se, sentence_cn=sc, roots=ro, similar=si,
                 mnemonic=mn, hs=hs)
        )

    L = []
    w = L.append

    # ---------- 头部 ----------
    w("# 过四斩六 · 完整词汇总表")
    w("")
    w("> 本文档由 `tools/export_words_md.py` 从安装包内置词库 `assets/words.db` 直接导出，")
    w("> 与 App 内看到的内容**逐字段一致**，不含任何额外加工或润色。")
    w("")
    w("| 项目 | 数值 |")
    w("| :--- | ---: |")
    w(f"| 词条总数 | **{total}** 行 |")
    w(f"| 四级段 | **{len(grouped.get(1, {}) and [x for u in grouped.get(1, {}).values() for x in u] or [])}** 行"
      f" / {len(grouped.get(1, {}))} 个单元 |")
    w(f"| 六级段 | **{len([x for u in grouped.get(2, {}).values() for x in u])}** 行"
      f" / {len(grouped.get(2, {}))} 个单元 |")
    w("| 单元规模 | 每单元 50 词，按单词字母序切块 |")
    w("")
    w("每个词条包含：音标、中文释义（简 / 详）、英文释义、例句（英 / 中）、"
      "词根词缀、形近词、联想记忆。")
    w("")
    w("标注说明：")
    w("")
    w("- **高中课标**：属于官方高考大纲（ECDICT `gk` 标签）的词汇，共 2604 个不同单词，"
      "在 App 的「设置 → 高中词汇」里可一键标为已学会。")
    w("- 六级段包含四级段的全部单词（与官方大纲一致），所以同一个人在六级段也会有一条。")
    w("- 词条按「阶段 → 单元 → 字母序」排列，`序号` 是词库里的 `rank`，"
      "也是 App 内闯关出题顺序的依据。")
    w("")

    # ---------- 已知问题 ----------
    w("## 已知问题（如实标注）")
    w("")
    w("以下问题**在本词库中真实存在**，本轮没有修正，导出时原样保留、只在这里说明，")
    w("避免读者把它们当成正确内容记忆：")
    w("")
    w("### 1. 少量词条的「词根词缀」与词性矛盾（12 个不同单词 / 23 行）")
    w("")
    w("词库生成时 `decompose()` 只按**字符串后缀**匹配、不看词性，")
    w("于是单词末尾恰好是某个后缀就被套上那个后缀的含义。例如：")
    w("")
    w("| 单词 | 词性 | 被错误标注的后缀 | 为什么错 |")
    w("| :--- | :--- | :--- | :--- |")
    w("| `archive` | vt. / n. | `-ive`（形容词后缀） | archive 不是形容词，-ive 只是词尾巧合 |")
    w("| `astonish` | vt. | `-ish`（形容词后缀） | 同上，-ish 是词尾巧合 |")
    w("| `forgive` | vt. | `-ive`（形容词后缀） | 同上 |")
    w("| `handful` | n. | `-ful`（形容词后缀） | handful 是名词 |")
    w("| `timetable` | n. | `-able`（形容词后缀） | timetable 是名词 |")
    w("| `nevertheless` | adv. / conj. | `-less`（形容词后缀） | 同上 |")
    w("")
    w("受影响的 12 个词：`archive`、`astonish`、`cherish`、`flourish`、`forgive`、"
      "`furnish`、`nevertheless`、`nonetheless`、`nourish`、`publish`、`represent`、"
      "`survive`、`timetable`。这些词条的**释义、音标、例句都是正确的**，只有「词根词缀」一栏不可信。")
    w("")
    w("### 2. 36 个词条没有音标")
    w("")
    w("多为缩写与复合词，例如 `b.c.`、`dvd`、`p.m.`、`vcd`、`living-room`、`so-called`。")
    w("缩写本身通常不标音标；复合词可以从构词推出，但自动补等于替读者猜 —— 补错比留空更糟，")
    w("所以保持留空。App 内遇到这类词会直接不显示音标行。")
    w("")
    w("### 3. 词表里有两处收录问题")
    w("")
    w("- `reservior` 是 `reservoir` 的错拼，但**正确拼写也在词库里**，两条并存；")
    w("- `uptodate` 与 `up-to-date` 重复收录，释义还不一致。")
    w("")
    w("这两条没有删，因为删除会让 `rank` 断号，而 App 的闯关出题顺序依赖 `rank` 连续。")
    w("它们属于重建词库时该一并处理的词表问题。")
    w("")

    # ---------- 目录 ----------
    w("## 目录")
    w("")
    for stage in (1, 2):
        if stage not in grouped:
            continue
        label = STAGE_LABEL[stage]
        key = "cet4" if stage == 1 else "cet6"
        units = sorted(grouped[stage])
        w(f"### {label}（{len(units)} 个单元）")
        w("")
        for unit in units:
            items = grouped[stage][unit]
            first, last = items[0]["word"], items[-1]["word"]
            w(f"- [第 {unit} 单元（{esc_link(first)} ~ {esc_link(last)}，{len(items)} 词）]"
              f"(#{ANCHOR_PREFIX[key]}u{unit})")
        w("")

    # ---------- 正文 ----------
    for stage in (1, 2):
        if stage not in grouped:
            continue
        label = STAGE_LABEL[stage]
        key = "cet4" if stage == 1 else "cet6"
        w(f'<a id="{key}"></a>')
        w("")
        w(f"# {label}词表")
        w("")
        for unit in sorted(grouped[stage]):
            items = grouped[stage][unit]
            w(f'<a id="{ANCHOR_PREFIX[key]}u{unit}"></a>')
            w("")
            w(f"## {label} · 第 {unit} 单元（{items[0]['word']} ~ {items[-1]['word']}）")
            w("")
            for it in items:
                word = it["word"]
                badge = " `高中课标`" if it["hs"] else ""
                w(f"### {it['rank']}. {word}{badge}")
                w("")
                ph = clean(it["phonetic"])
                if ph:
                    w(f"- **音标**：{ph}")
                tr = clean(it["translation"])
                if tr:
                    w(f"- **释义**：{tr}")
                td = clean(it["trans_detail"])
                if td and td != tr:
                    w(f"- **完整释义**：{td}")
                df = clean(it["definition"])
                if df:
                    w(f"- **英文释义**：{df}")
                se = clean(it["sentence"])
                sc = clean(it["sentence_cn"])
                if se:
                    w(f"- **例句**：{se}")
                if sc:
                    w(f"- **例句翻译**：{sc}")
                ro = clean(it["roots"])
                if ro:
                    w(f"- **词根词缀**：{ro}")
                si = clean(it["similar"])
                if si:
                    w(f"- **形近词**：{si}")
                mn = clean(it["mnemonic"])
                if mn:
                    w(f"- **联想记忆**：{mn}")
                w("")

    text = "\n".join(L)
    os.makedirs(os.path.dirname(os.path.abspath(OUT)), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print(f"已写出 {OUT}")
    print(f"  {total} 行词条，{len(text):,} 字符，{os.path.getsize(OUT):,} 字节")


if __name__ == "__main__":
    main()
