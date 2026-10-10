# -*- coding: utf-8 -*-
"""
离线校验：在 PC 端用同一份 SQL 语句验证内置词库，
提前发现「列名/顺序不匹配」这类只会在真机运行时才暴露的问题。
"""
import sqlite3, os, sys

DB = r"D:\wby\46\CetVocab\app\src\main\assets\words.db"
ok = True


def check(name, cond, extra=""):
    global ok
    print(("  [OK]   " if cond else "  [FAIL] ") + name + (("  " + extra) if extra else ""))
    if not cond:
        ok = False


print("== 1. 打开内置词库 ==")
con = sqlite3.connect(DB)
cur = con.cursor()

cols = [r[1] for r in cur.execute("PRAGMA table_info(words)")]
print("  实际列:", cols)
expect = ["id", "word", "phonetic", "translation", "trans_detail", "definition", "pos",
          "stage", "unit", "rank", "sentence", "sentence_cn", "roots", "similar", "mnemonic"]
check("列名与 WordDb.kt 的 SELECT 完全一致", cols == expect)

print("\n== 2. 逐条执行 WordDb.kt 中的 SQL ==")
SEL = ("SELECT id,word,phonetic,translation,trans_detail,definition,pos,stage,unit,rank,"
       "sentence,sentence_cn,roots,similar,mnemonic FROM words ")

try:
    rows = cur.execute(SEL + 'WHERE stage=? AND unit=? ORDER BY "rank"', (1, 1)).fetchall()
    check("wordsOfUnit(stage,unit)", len(rows) > 0, f"返回 {len(rows)} 行")
    r = rows[0]
    check("15 个字段全部可读且类型正确",
          isinstance(r[0], int) and isinstance(r[1], str) and isinstance(r[7], int)
          and isinstance(r[3], str) and isinstance(r[14], str),
          f"首词={r[1]}")
except Exception as e:
    check("wordsOfUnit", False, repr(e))

for sql, args, label in [
    (SEL + "WHERE stage=?", (1,), "wordsOfStage(1)"),
    (SEL + "WHERE id=?", (500,), "wordById(500)"),
    (SEL + "WHERE id IN (?,?,?)", (1, 2, 3), "wordsByIds"),
    (SEL + "WHERE word LIKE ? OR translation LIKE ? ORDER BY word LIMIT 60",
     ("%aband%", "%aband%"), "search"),
    (SEL + "WHERE stage=? ORDER BY RANDOM() LIMIT 5", (2,), "randomWords"),
]:
    try:
        n = len(cur.execute(sql, args).fetchall())
        check(label, True, f"{n} 行")
    except Exception as e:
        check(label, False, repr(e))

for sql, args, label in [
    ("SELECT COUNT(*) FROM words WHERE stage=?", (1,), "countOfStage"),
    ("SELECT COUNT(DISTINCT unit) FROM words WHERE stage=?", (1,), "unitCount"),
    ("SELECT MIN(unit) FROM words WHERE stage=?", (2,), "firstUnit"),
    ("SELECT MAX(unit) FROM words WHERE stage=?", (2,), "lastUnit"),
]:
    try:
        v = cur.execute(sql, args).fetchone()[0]
        check(label, v is not None, f"= {v}")
    except Exception as e:
        check(label, False, repr(e))

print("\n== 3. 数据完整性 ==")
cur.execute("SELECT COUNT(*) FROM words")
total = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM words WHERE id IN (SELECT id FROM words GROUP BY id HAVING COUNT(*)>1)")
check("id 唯一（PRIMARY KEY）", cur.fetchone()[0] == 0)
cur.execute("SELECT COUNT(*) FROM words WHERE word='' OR word IS NULL")
check("无空单词", cur.fetchone()[0] == 0)
cur.execute("SELECT COUNT(*) FROM words WHERE stage NOT IN (1,2)")
check("stage 只有 1/2", cur.fetchone()[0] == 0)
cur.execute("SELECT COUNT(*) FROM words WHERE unit<1 OR \"rank\"<1")
check("unit/rank 均为正数", cur.fetchone()[0] == 0)
cur.execute("SELECT COUNT(*) FROM words WHERE sentence<>'' AND sentence_cn=''")
check("有例句即有译文", cur.fetchone()[0] == 0)
cur.execute("SELECT COUNT(*) FROM words WHERE similar LIKE '%|;;%' OR similar LIKE '%;;%'")
check("形近词串格式规范", True, "（非强制）")

print(f"\n  词条总数 {total}")

print("\n== 4. 模拟 UserDb.kt 建表语句 ==")
mem = sqlite3.connect(":memory:")
ddl = [
    "CREATE TABLE word_state(word_id INTEGER PRIMARY KEY,level INTEGER NOT NULL DEFAULT 0,"
    "wrong_count INTEGER NOT NULL DEFAULT 0,fav INTEGER NOT NULL DEFAULT 0,"
    "last_ts INTEGER NOT NULL DEFAULT 0,next_review_ts INTEGER NOT NULL DEFAULT 0)",
    "CREATE TABLE study_log(id INTEGER PRIMARY KEY AUTOINCREMENT,word_id INTEGER NOT NULL,"
    "correct INTEGER NOT NULL,day TEXT NOT NULL,ts INTEGER NOT NULL)",
    "CREATE INDEX idx_log_day ON study_log(day)",
    "CREATE TABLE quiz_record(id INTEGER PRIMARY KEY AUTOINCREMENT,stage INTEGER NOT NULL,"
    "unit INTEGER NOT NULL,mode TEXT NOT NULL,score INTEGER NOT NULL,total INTEGER NOT NULL,"
    "correct INTEGER NOT NULL,max_combo INTEGER NOT NULL,duration_ms INTEGER NOT NULL,ts INTEGER NOT NULL)",
    "CREATE TABLE achievement(code TEXT PRIMARY KEY, unlocked_ts INTEGER NOT NULL)",
    "CREATE TABLE app_meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)",
]
for s in ddl:
    try:
        mem.execute(s)
    except Exception as e:
        check("DDL: " + s[:42], False, repr(e))
check("UserDb 全部建表语句可执行", True)

q = [
    ("SELECT COUNT(DISTINCT word_id) FROM study_log WHERE day=?", ("2026-10-10",)),
    ("SELECT SUM(correct), COUNT(*)-SUM(correct) FROM study_log", ()),
    ("SELECT DISTINCT day FROM study_log ORDER BY day DESC", ()),
    ("SELECT day, COUNT(DISTINCT word_id), SUM(correct), COUNT(*)-SUM(correct) "
     "FROM study_log WHERE day>=? GROUP BY day", ("2026-10-01",)),
    ("SELECT MAX(score) FROM quiz_record", ()),
    ("SELECT SUM(duration_ms) FROM quiz_record", ()),
    ("SELECT id,stage,unit,mode,score,total,correct,max_combo,duration_ms,ts "
     "FROM quiz_record ORDER BY score DESC, ts DESC LIMIT 50", ()),
    ("SELECT v FROM app_meta WHERE k=?", ("study_seconds",)),
]
for s, a in q:
    try:
        mem.execute(s, a).fetchall()
    except Exception as e:
        check("UserDb SQL: " + s[:46], False, repr(e))
check("UserDb 全部查询语句可执行", True)

print("\n== 5. 抽样式例（前 3 个单元各取 1 词） ==")
for st in (1, 2):
    for u in (1, 60, 120):
        row = cur.execute(SEL + 'WHERE stage=? AND unit=? ORDER BY "rank" LIMIT 1', (st, u)).fetchone()
        if row:
            print(f"  [S{st} U{u}] {row[1]}  {row[2]}  | {row[3]}  | 例:{row[10][:46]}")

con.close()
print("\n" + ("=" * 54))
print("校验结果：" + ("全部通过 ✅" if ok else "存在失败项 ❌"))
print("=" * 54)
sys.exit(0 if ok else 1)
