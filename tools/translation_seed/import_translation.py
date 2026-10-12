# -*- coding: utf-8 -*-
"""把补充的「真题句子翻译」题目写进词库 assets/words.db 的 translation_quiz 表。

用法：
    python import_translation.py            # 试跑（只校验，不写库）
    python import_translation.py --write    # 校验通过后写入

写入前会自动备份一份 words.db.bak。
"""

import os
import re
import shutil
import sqlite3
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DB = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "words.db"))

FILES = [
    "cet4_1.py", "cet4_2.py", "cet4_3.py", "cet4_4.py", "cet4_5.py",
    "cet6_1.py", "cet6_2.py", "cet6_3.py", "cet6_4.py", "cet6_5.py", "cet6_6.py",
]

# 内容合规：涉及政治、宗教、暴力、色情等敏感话题的词一律拒绝入库
BANNED = [
    "政府", "党", "主席", "总理", "军", "武器", "枪", "宗教", "上帝", "佛", "基督",
    "示威", "抗议", "罢工", "游行", "民主", "自由运动", "革命", "镇压", "病毒", "疫情",
    "死亡", "自杀", "毒品", "赌博", "色情", "裸", "奴隶", "歧视妇女", "种族",
]


def load():
    items = []
    for name in FILES:
        path = os.path.join(HERE, name)
        ns = {}
        with open(path, "r", encoding="utf-8") as f:
            exec(compile(f.read(), path, "exec"), ns)
        stage = ns["STAGE"]
        for kw, zh, opts, correct, analysis in ns["ITEMS"]:
            items.append((stage, kw, zh, opts, correct, analysis))
    return items


def main():
    write = "--write" in sys.argv
    items = load()
    print("共读取 %d 条补充题" % len(items))

    db = sqlite3.connect(DB)
    c = db.cursor()

    words = {1: set(), 2: set()}
    for stage, w in c.execute("SELECT stage, word FROM words"):
        words[stage].add(w.lower())

    existing_zh = set(r[0] for r in c.execute("SELECT chinese FROM translation_quiz"))
    existing_kw = {1: set(), 2: set()}
    for stage, kw in c.execute("SELECT stage, keyword FROM translation_quiz"):
        existing_kw[stage].add(kw.lower())

    errors = []
    seen_zh = set()
    seen_kw = {1: set(), 2: set()}
    per_stage = {1: 0, 2: 0}

    for idx, (stage, kw, zh, opts, correct, analysis) in enumerate(items, 1):
        tag = "#%d [%s] %s" % (idx, "四级" if stage == 1 else "六级", kw)
        if kw.lower() not in words[stage]:
            errors.append("%s：关键词不在%s词库中" % (tag, "四级" if stage == 1 else "六级"))
        if kw.lower() in existing_kw[stage] or kw.lower() in seen_kw[stage]:
            errors.append("%s：关键词与已有题目重复" % tag)
        if len(opts) != 4:
            errors.append("%s：选项数量不是 4" % tag)
        if len(set(opts)) != 4:
            errors.append("%s：四个选项存在重复" % tag)
        if any(not o.strip() for o in opts):
            errors.append("%s：存在空选项" % tag)
        if not analysis.strip():
            errors.append("%s：缺少解析" % tag)
        picks = [int(x) for x in correct.split(",") if x.strip().isdigit()]
        if not picks or len(set(picks)) != len(picks) or not all(0 <= p <= 3 for p in picks):
            errors.append("%s：correct 字段非法 -> %r" % (tag, correct))
        if len(picks) != 2:
            errors.append("%s：正确项应为 2 个，实际 %d 个" % (tag, len(picks)))
        if zh in existing_zh or zh in seen_zh:
            errors.append("%s：题干与已有题目重复" % tag)
        for b in BANNED:
            if b in zh:
                errors.append("%s：题干含敏感词「%s」" % (tag, b))
        if len(re.findall(r"[一-龥]", zh)) < 8:
            errors.append("%s：题干过短，不像完整句子" % tag)
        seen_zh.add(zh)
        seen_kw[stage].add(kw.lower())
        per_stage[stage] += 1

    print("分级统计：四级 +%d，六级 +%d" % (per_stage[1], per_stage[2]))
    cur = dict(c.execute("SELECT stage, COUNT(*) FROM translation_quiz GROUP BY stage").fetchall())
    print("入库后预计：四级 %d 条，六级 %d 条，合计 %d 条" % (
        cur.get(1, 0) + per_stage[1], cur.get(2, 0) + per_stage[2],
        cur.get(1, 0) + cur.get(2, 0) + len(items)))

    if errors:
        print("\n发现 %d 处问题：" % len(errors))
        for e in errors[:60]:
            print("  - " + e)
        if len(errors) > 60:
            print("  ... 另有 %d 处" % (len(errors) - 60))
        sys.exit(1)

    print("\n校验全部通过。")
    if not write:
        print("（试跑模式，未写入。加 --write 执行写入）")
        return

    shutil.copy2(DB, DB + ".bak")
    print("已备份 -> " + DB + ".bak")

    next_id = c.execute("SELECT MAX(id) FROM translation_quiz").fetchone()[0] or 0
    rows = []
    for stage, kw, zh, opts, correct, analysis in items:
        next_id += 1
        rows.append((next_id, stage, kw, zh, opts[0], opts[1], opts[2], opts[3], correct, analysis))

    c.executemany(
        "INSERT INTO translation_quiz(id,stage,keyword,chinese,opt_a,opt_b,opt_c,opt_d,correct,analysis) "
        "VALUES(?,?,?,?,?,?,?,?,?,?)", rows)
    db.commit()
    print("已写入 %d 条，当前 id 上限 %d" % (len(rows), next_id))

    for stage in (1, 2):
        n = c.execute("SELECT COUNT(*) FROM translation_quiz WHERE stage=?", (stage,)).fetchone()[0]
        print("  %s：%d 条" % ("四级" if stage == 1 else "六级", n))
    db.close()


if __name__ == "__main__":
    main()
