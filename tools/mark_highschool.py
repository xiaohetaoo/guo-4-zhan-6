# -*- coding: utf-8 -*-
"""给内置词库加「高中课标」标记列。

口径：ECDICT 的 `gk` 标签（gaokao，3,677 词）= 官方高考大纲词表。
本项目的 words.db 是四级(4750)∪六级(6248)=6248 个不同词，
与 gk 的交集是 2604 词（占词库 23.7%）。

只加一列 is_hs，不改任何既有列、不动行序 —— 这样：
  · rank / unit / id 全部不变，闯关重放的「同一 seed → 同一题序」不受影响；
  · 老用户升级后只需重新拷一次词库（DB_VERSION 6→7），学习记录不动。

用法：python tools/mark_highschool.py DATA=<ecdict.csv 所在目录>
"""
import csv
import os
import shutil
import sqlite3
import sys

csv.field_size_limit(10 ** 9)

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _resolve(cli_key, env_key, default):
    for arg in sys.argv[1:]:
        if arg.startswith(cli_key + "="):
            return arg.split("=", 1)[1]
    return os.environ.get(env_key) or default


DATA = _resolve("DATA", "CETVOCAB_DATASET_DIR",
                os.path.join(os.path.dirname(_ROOT), "_datasets"))
DB = _resolve("OUT", "CETVOCAB_WORDS_DB", os.path.join(_ROOT, "app", "src", "main", "assets", "words.db"))
ECDICT = os.path.join(DATA, "ecdict.csv")

if not os.path.isfile(ECDICT):
    sys.exit(f"找不到 {ECDICT}\n  用 DATA=<目录> 指过来，目录里要有 ecdict.csv")
if not os.path.isfile(DB):
    sys.exit(f"找不到词库 {DB}")

# 1) 读 gk 标签
gk = set()
with open(ECDICT, encoding="utf-8") as f:
    for row in csv.DictReader(f):
        if "gk" in (row.get("tag") or "").split():
            gk.add((row.get("word") or "").strip().lower())
print(f"ECDICT gk（高考大纲）词：{len(gk)}")

# 2) 备份 → 加列 → 回填
#
# 备份刻意放在**词库同级目录之外**：assets/ 下的任何文件都会被整包打进 APK
# （Gradle 的 assets 目录是整个搬进去的）。曾有版本把 words.db.bak 留在
# assets 里直接发布，安装包凭空多出 7.98 MB。这里落到 tools/_backup/ 下。
bak_dir = os.path.join(_ROOT, "tools", "_backup")
os.makedirs(bak_dir, exist_ok=True)
bak = os.path.join(bak_dir, "words.db.before_hs.bak")
if not os.path.exists(bak):
    shutil.copy2(DB, bak)
    print(f"已备份原词库 → {bak}")
else:
    print(f"备份已存在，跳过 → {bak}")

con = sqlite3.connect(DB)
cur = con.cursor()
cols = {r[1] for r in cur.execute("PRAGMA table_info(words)")}
if "is_hs" not in cols:
    cur.execute("ALTER TABLE words ADD COLUMN is_hs INTEGER NOT NULL DEFAULT 0")
    print("已新增列 is_hs（INTEGER NOT NULL DEFAULT 0）")
else:
    print("列 is_hs 已存在，直接重算")

cur.execute("UPDATE words SET is_hs=0")
cur.execute("SELECT id, word FROM words")
rows = cur.fetchall()
hit_ids = [i for i, w in rows if (w or "").strip().lower() in gk]
# 分批 UPDATE：SQLite 变量数上限 999，2604 个 id 一次拼字符串反而更快且不占变量
cur.executemany("UPDATE words SET is_hs=1 WHERE id=?", [(i,) for i in hit_ids])
con.commit()

cur.execute("SELECT COUNT(*) FROM words WHERE is_hs=1")
n = cur.fetchone()[0]
cur.execute("SELECT stage, COUNT(*) FROM words WHERE is_hs=1 GROUP BY stage")
by_stage = dict(cur.fetchall())

# 3) 建索引：设置页要统计、词库页要过滤，1.1 万行虽小但别让每次全扫
cur.execute("CREATE INDEX IF NOT EXISTS idx_hs ON words(is_hs)")
con.commit()
cur.execute("VACUUM")
con.commit()

print(f"标记完成：{n} 词（四级 {by_stage.get(1, 0)} / 六级 {by_stage.get(2, 0)}）")
print(f"词库总量：{len(rows)}，占比 {n / len(rows) * 100:.1f}%")
con.close()
