# -*- coding: utf-8 -*-
"""
构建内置只读词库 assets/words.db

数据来源（全部为开源数据，见设计文档 9.4）：
  1. ECDICT (https://github.com/skywind3000/ECDICT, MIT)
     - 提供官方四/六级大纲标签 tag(cet4/cet6)、中文释义、英文释义、词性、词频、词形变化
  2. zhenghaoyang24/english-vocabulary (开源整理)
     - 提供英美音标、中文释义、单词频率
     - 提供 14 万条中英对照例句，以及《四级词汇乱序便携版》《星火四级词汇必背乱序版》词书

产出表结构 words：
  id, word, phonetic, translation, trans_detail, definition, pos,
  stage, unit, rank, sentence, sentence_cn, roots, similar, mnemonic
"""
import csv, json, os, re, sqlite3, sys, collections

DATA = r"C:\Users\67153\AppData\Local\AndroidBuild\data"
OUT = r"D:\wby\46\CetVocab\app\src\main\assets\words.db"

WORDS_PER_UNIT = 50
STAGE_CET4, STAGE_CET6 = 1, 2

csv.field_size_limit(10 ** 9)
os.makedirs(os.path.dirname(OUT), exist_ok=True)


def log(*a):
    print(*a, flush=True)


# ---------------------------------------------------------------- 词缀表
PREFIXES = [
    ("counter", "反对/相反"), ("circum", "环绕"), ("extra", "超出/额外"), ("hyper", "过度"),
    ("inter", "在…之间/相互"), ("trans", "横越/转移"), ("under", "在…下/不足"), ("super", "超过/在上"),
    ("micro", "微小"), ("macro", "宏大"), ("multi", "多"), ("semi", "半"), ("para", "旁边/辅助"),
    ("over", "过度/在上"), ("anti", "反对/抗"), ("auto", "自己/自动"), ("post", "在后"), ("fore", "在前/预先"),
    ("meta", "超越/变化"), ("hypo", "下/次"), ("syn", "共同"), ("sym", "共同"), ("per", "贯穿/完全"),
    ("pre", "在前/预先"), ("pro", "向前/支持"), ("con", "共同/加强"), ("com", "共同/加强"),
    ("dis", "不/分离"), ("mis", "错误"), ("non", "非"), ("out", "超过/向外"), ("sub", "在下/次"),
    ("sup", "在下"), ("uni", "一"), ("mono", "单一"), ("tele", "远"), ("thermo", "热"),
    ("psych", "心理"), ("chron", "时间"), ("with", "向后/反对"), ("ex", "向外/出"), ("en", "使…/进入"),
    ("em", "使…/进入"), ("im", "不/向内"), ("in", "不/向内"), ("il", "不"), ("ir", "不"),
    ("de", "向下/去除"), ("re", "再/回/反"), ("un", "不/相反"), ("up", "向上"), ("bi", "二"),
    ("tri", "三"), ("ad", "朝向/加强"), ("ab", "离开"), ("co", "共同"), ("be", "使…"),
    ("geo", "地"), ("bio", "生命"),
]

ROOTS = [
    ("spect", "看"), ("spic", "看"), ("dict", "说"), ("duct", "引导"), ("duc", "引导"),
    ("ject", "投掷"), ("miss", "送"), ("mit", "送"), ("scrib", "写"), ("script", "写"),
    ("tract", "拉"), ("vert", "转"), ("vers", "转"), ("vid", "看"), ("vis", "看"),
    ("capt", "抓"), ("cept", "拿"), ("ceive", "拿"), ("cap", "抓/容纳"), ("fer", "带来"),
    ("gress", "走"), ("grad", "走/步"), ("mort", "死"), ("nat", "出生"), ("ped", "脚"),
    ("puls", "推"), ("pel", "推"), ("pend", "悬挂"), ("pens", "称重"), ("port", "携带"),
    ("rupt", "破裂"), ("sect", "切"), ("sens", "感觉"), ("sent", "感觉"), ("sequ", "跟随"),
    ("secut", "跟随"), ("sist", "站立"), ("stat", "站立"), ("sta", "站立"), ("tact", "接触"),
    ("tang", "接触"), ("tain", "握持"), ("tend", "伸展"), ("tens", "伸展"), ("vac", "空"),
    ("vent", "来"), ("ven", "来"), ("viv", "生命"), ("vit", "生命"), ("vok", "叫喊"),
    ("voc", "叫喊"), ("struct", "建造"), ("form", "形状"), ("cred", "相信"), ("curs", "跑"),
    ("cur", "跑/关心"), ("fluct", "流动"), ("flu", "流"), ("greg", "群体"), ("habit", "拥有"),
    ("her", "粘附"), ("lect", "选/读"), ("leg", "读/选"), ("lumin", "光"), ("luc", "光"),
    ("manu", "手"), ("memor", "记忆"), ("mod", "方式/尺度"), ("mov", "动"), ("mot", "动"),
    ("nom", "名字/法则"), ("part", "部分"), ("pass", "通过"), ("path", "感情/疾病"), ("phil", "爱"),
    ("phon", "声音"), ("plic", "折叠"), ("ply", "折叠"), ("popul", "人民"), ("press", "压"),
    ("prim", "第一"), ("priv", "私人"), ("prob", "证明"), ("punct", "点"), ("quir", "询问"),
    ("quer", "询问"), ("reg", "统治/规则"), ("rect", "直/正"), ("rid", "笑"), ("ris", "笑"),
    ("rog", "询问"), ("sci", "知道"), ("solv", "解开"), ("solu", "解开"), ("son", "声音"),
    ("sper", "希望"), ("spir", "呼吸"), ("strict", "拉紧"), ("string", "拉紧"), ("sumpt", "拿"),
    ("sum", "拿/总"), ("test", "证明"), ("text", "编织"), ("tort", "扭曲"), ("tour", "转"),
    ("trib", "给予"), ("turb", "搅动"), ("urb", "城市"), ("util", "使用"), ("vad", "走"),
    ("vas", "走"), ("vel", "覆盖"), ("verb", "词"), ("vest", "衣服"), ("vict", "征服"),
    ("vinc", "征服"), ("volut", "卷/滚"), ("volv", "卷/滚"),
]

SUFFIXES = [
    ("ability", "名词后缀，表能力"), ("ibility", "名词后缀，表能力"), ("ization", "名词后缀，…化"),
    ("isation", "名词后缀，…化"), ("ationally", "副词后缀"), ("ation", "名词后缀，表行为/状态"),
    ("ition", "名词后缀"), ("ution", "名词后缀"), ("ment", "名词后缀，表行为/结果"),
    ("ness", "名词后缀，表性质"), ("ance", "名词后缀，表状态"), ("ence", "名词后缀，表状态"),
    ("ancy", "名词后缀"), ("ency", "名词后缀"), ("ity", "名词后缀，表性质"), ("ism", "名词后缀，表主义"),
    ("ist", "名词后缀，表人"), ("ship", "名词后缀，表身份"), ("hood", "名词后缀，表时期"),
    ("wards", "副词后缀，表方向"), ("ward", "副词后缀，表方向"), ("wise", "副词后缀，表方式"),
    ("able", "形容词后缀，能…的"), ("ible", "形容词后缀，能…的"), ("less", "形容词后缀，无…的"),
    ("ful", "形容词后缀，充满…的"), ("ious", "形容词后缀，多…的"), ("ous", "形容词后缀，多…的"),
    ("eous", "形容词后缀"), ("ative", "形容词后缀"), ("ive", "形容词后缀，有…性质的"),
    ("ical", "形容词后缀"), ("ic", "形容词后缀"), ("ial", "形容词后缀"), ("ual", "形容词后缀"),
    ("al", "形容词后缀"), ("ary", "形容词/名词后缀"), ("ory", "形容词/名词后缀"),
    ("ant", "名词/形容词后缀"), ("ent", "名词/形容词后缀"), ("ate", "动词后缀，使…"),
    ("ify", "动词后缀，使…"), ("ize", "动词后缀，使…化"), ("ise", "动词后缀，使…化"),
    ("dom", "名词后缀，表领域"), ("ure", "名词后缀"), ("age", "名词后缀，表行为/总称"),
    ("some", "形容词后缀，有…倾向的"), ("like", "形容词后缀，像…的"), ("ish", "形容词后缀，略…的"),
    ("ly", "副词后缀"), ("er", "名词后缀，表人或物"), ("or", "名词后缀，表人或物"),
]

# 少量精选谐音/联想记忆（由大模型预生成、人工审核后固化，见 tools/data/mnemonics.json）
_MNEMONIC_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "mnemonics.json")
try:
    with open(_MNEMONIC_FILE, encoding="utf-8") as _f:
        HOMOPHONE = {k.lower(): v for k, v in json.load(_f)["entries"].items()}
except Exception as _e:  # 缺失时不影响主流程
    print("  ! 助记内容包读取失败:", _e)
    HOMOPHONE = {}
print("     助记内容包（谐音/联想）:", len(HOMOPHONE), "条")

# ---------------------------------------------------------------- 读取数据
log("[1/7] 读取 ECDICT ...")
ec = {}
with open(os.path.join(DATA, "ecdict.csv"), encoding="utf-8", newline="") as f:
    for row in csv.DictReader(f):
        w = (row.get("word") or "").strip()
        if not w:
            continue
        ec[w.lower()] = row
log("     ECDICT 词条:", len(ec))

log("[2/7] 读取 tb_vocabulary / tb_book / tb_voc_book / tb_voc_examples ...")
voc = json.load(open(os.path.join(DATA, "tb_vocabulary.json"), encoding="utf-8"))
vbk = json.load(open(os.path.join(DATA, "tb_voc_book.json"), encoding="utf-8"))
examples = json.load(open(os.path.join(DATA, "tb_voc_examples.json"), encoding="utf-8"))

w2id, id2voc = {}, {}
for v in voc:
    key = v["spelling"].strip().lower()
    w2id.setdefault(key, v["wordid"])
    id2voc[v["wordid"]] = v
log("     tb_vocabulary:", len(voc))

book_words = collections.defaultdict(set)
id2w = {v["wordid"]: v["spelling"].strip().lower() for v in voc}
for x in vbk:
    if x["wordid"] in id2w:
        book_words[x["bookid"]].add(id2w[x["wordid"]])

by_wordid = collections.defaultdict(list)
for e in examples:
    by_wordid[e["wordid"]].append(e)
log("     例句总数:", len(examples), " 覆盖单词:", len(by_wordid))

# 例句反查索引：句子中出现过的词形 -> 例句（用于补充无直接例句的词）
log("[3/7] 构建例句词形索引 ...")
TOKEN_RE = re.compile(r"[a-zA-Z][a-zA-Z'-]+")
token_index = collections.defaultdict(list)
for e in examples:
    for t in set(TOKEN_RE.findall(e["en"].lower())):
        token_index[t].append(e)
log("     索引词形数:", len(token_index))

# ---------------------------------------------------------------- 阶段词表
cet4 = {w for w, r in ec.items() if "cet4" in (r.get("tag") or "").split()}
cet6 = {w for w, r in ec.items() if "cet6" in (r.get("tag") or "").split()}

stage4 = set(cet4) | book_words.get(1, set()) | book_words.get(3, set())
stage6 = set(cet6) | stage4
stage4 = {w for w in stage4 if w in ec}
stage6 = {w for w in stage6 if w in ec}
log(f"[4/7] 阶段词表：四级={len(stage4)}  六级={len(stage6)}")


def freq_of(w):
    """排序权重：词频高者在前（tb frequency 0~1，辅以柯林斯星级与牛津核心标记）"""
    s = 0.0
    v = id2voc.get(w2id.get(w, -1))
    if v:
        s += float(v.get("frequency") or 0.0)
    r = ec.get(w) or {}
    try:
        s += 0.02 * int(r.get("collins") or 0)
    except Exception:
        pass
    if (r.get("oxford") or "") == "1":
        s += 0.06
    try:
        bnc = int(r.get("bnc") or 0)
        if bnc > 0:
            s += max(0.0, 0.10 - bnc / 200000.0)
    except Exception:
        pass
    return -s  # 降序


# ---------------------------------------------------------------- 词根词缀拆解
# 只在这些“派生后缀”上允许在词干不是独立单词时仍作拆解
SAFE_DERIV = {
    "tion", "sion", "ation", "ition", "ution", "ment", "ness", "ance", "ence",
    "ancy", "ency", "ity", "ism", "ist", "ship", "hood", "ward", "wards", "wise",
    "able", "ible", "less", "ful", "ous", "ious", "eous", "ive", "ative", "ical",
    "ify", "ize", "ise",
}


def decompose(word):
    """返回 (prefix, p_meaning, root, r_meaning, suffix, s_meaning, stem)"""
    w = word.lower()
    pre = pmean = suf = smean = None
    body = w
    for p, m in PREFIXES:
        if len(p) >= 2 and w.startswith(p):
            rest = w[len(p):]
            # 去掉前缀后必须仍是像样的词干，避免 in/on/be 之类的误拆
            if len(rest) >= 5 or rest in ec:
                body = rest
                pre, pmean = p, m
            break
    tail = body
    for s, m in SUFFIXES:
        if len(s) >= 2 and tail.endswith(s):
            stem = tail[: len(tail) - len(s)]
            ok = (stem in ec and len(stem) >= 4) or (s in SAFE_DERIV and len(stem) >= 4)
            if ok:
                tail = stem
                suf, smean = s, m
            break
    root = rmean = None
    for r, m in ROOTS:
        if len(r) >= 3 and r in body:
            # 词根必须落在去缀后的主体里，且剩余部分不过长
            if len(body) - len(r) <= 5:
                root, rmean = r, m
                break
    if not (pre or suf or root):
        return None
    return pre, pmean, root, rmean, suf, smean, tail


def build_roots(word):
    d = decompose(word)
    if not d:
        return ""
    pre, pmean, root, rmean, suf, smean, stem = d
    parts = []
    if pre:
        parts.append(f"{pre}-（{pmean}）")
    if root:
        parts.append(f"{root}（{rmean}）")
    if suf:
        parts.append(f"-{suf}（{smean}）")
    if not parts:
        return ""
    return " + ".join(parts)


def build_mnemonic(word, roots, similar_list, exchange, stage):
    tips = []
    curated = HOMOPHONE.get(word)
    if curated:
        tips.append(curated)
    # 精选助记本身若是词根词缀式拆解，就不再重复输出自动拆解
    curated_is_roots = bool(curated) and ("+" in curated) and ("→" in curated)
    if roots and not curated_is_roots:
        tips.append("词根词缀记忆：" + roots)
    fam = word_family(word, exchange)
    if fam:
        tips.append("词形变化：" + "、".join(fam))
    if similar_list:
        tips.append("形近辨析：注意与 " + "、".join(w for w, _ in similar_list) + " 区分")
    if not tips:
        tips.append("高频核心词，建议结合下方例句在语境中记忆")
    return "\n".join(tips[:3])


FORM_LABEL = {"p": "过去式", "d": "过去分词", "i": "现在分词", "3": "第三人称单数",
              "r": "比较级", "t": "最高级", "s": "复数", "0": "原形", "1": "变换形式"}


def word_family(word, exchange):
    if not exchange:
        return []
    out = []
    for chunk in exchange.split("/"):
        if ":" not in chunk:
            continue
        k, v = chunk.split(":", 1)
        v = v.strip()
        if not v or v == word:
            continue
        lbl = FORM_LABEL.get(k, "")
        if k in ("0", "1") and v in ec:
            lbl = "派生"
        out.append(f"{v}" + (f"（{lbl}）" if lbl else ""))
    return out[:5]


# ---------------------------------------------------------------- 释义整理
POS_RE = re.compile(r"^((?:n|v|vt|vi|adj|adv|prep|conj|pron|num|art|int|aux|abbr|u|c)\.\s*)+", re.I)


TAIL_TRIM = "，,；;、.。:： 　…·-—"


def unescape(raw):
    """ECDICT 中换行以字面量 \\n 存储，需还原为真实换行"""
    return (raw or "").replace("\\n", "\n").replace("\\r", "")


def norm_pos(txt):
    """统一词性缩写写法：a. -> adj. , ad. -> adv."""
    txt = re.sub(r"(^|[\s,，])(?:a)\.", r"\1adj.", txt)
    txt = re.sub(r"(^|[\s,，])(?:ad)\.", r"\1adv.", txt)
    return txt


def clean_translation(raw):
    if not raw:
        return []
    txt = unescape(raw)
    lines = [re.sub(r"[ \t]+", " ", x).strip() for x in txt.split("\n")]
    return [norm_pos(x) for x in lines if x]


def short_translation(lines, limit=26):
    """用于闯关选项的短释义：取首义项，去掉冗余"""
    if not lines:
        return ""
    s = lines[0]
    m = POS_RE.match(s)
    pos, rest = ("", s)
    if m:
        pos = norm_pos(m.group(0).strip())
        rest = s[m.end():].strip()
    if not rest:
        rest = s
        pos = ""
    rest = rest.strip(TAIL_TRIM)
    # 优先在标点处断开
    for sep in ("；", ";", "，", ",", "："):
        if sep in rest:
            head = rest.split(sep)[0].strip(TAIL_TRIM)
            if 2 <= len(head) <= limit:
                rest = head
                break
    if len(rest) > limit:
        rest = rest[:limit].rstrip(TAIL_TRIM) + "…"
    return (pos + " " + rest).strip() if pos else rest


def pos_tags(word, row, lines):
    p = (row.get("pos") or "").strip()
    out = []
    if p:
        items = []
        for chunk in p.split("/"):
            if ":" in chunk:
                k, v = chunk.split(":", 1)
                try:
                    items.append((int(v), k.strip()))
                except Exception:
                    items.append((0, k.strip()))
        items.sort(reverse=True)
        for _, k in items[:3]:
            if k and k not in out:
                out.append(k)
    if not out:
        for ln in lines[:3]:
            m = POS_RE.match(ln)
            if m:
                for t in re.findall(r"(n|vt|vi|v|adj|adv|prep|conj|pron|num|art|int)\.", m.group(0), re.I):
                    t = t.lower()
                    if t not in out:
                        out.append(t)
    return " ".join(t + "." for t in out[:3])


def pick_phonetic(word, row, v):
    uk = (v.get("UKphonetic") or "").strip() if v else ""
    us = (v.get("USphonetic") or "").strip() if v else ""
    ecp = (row.get("phonetic") or "").strip()
    if not uk:
        uk = ecp
    if not us:
        us = ecp
    norm = lambda x: x.replace("ˈ", "").replace("ˌ", "").replace(" ", "").strip("/")
    if uk and us and norm(uk) != norm(us):
        return f"英 /{uk}/  美 /{us}/"
    one = uk or us
    return f"/{one}/" if one else ""


# ---------------------------------------------------------------- 例句挑选
GOOD_LEN = (12, 110)


def pick_example(word, wid):
    """
    例句挑选：
      1) 优先「整词出现」的句子（\\bword\\b），避免 want 匹配到 Wanta 这类伪命中；
      2) 其次允许词形变化（前缀匹配，如 abandon 匹配 abandoned）；
      3) 同层内按热度、长度、位置、专有名词/数字数量打分。
    """
    pool = list(by_wordid.get(wid, [])) if wid else []
    pool += list(token_index.get(word, []))
    pool = [e for e in pool if e.get("en") and e.get("cn")]
    if not pool:
        return "", ""

    whole = re.compile(r"\b" + re.escape(word) + r"\b", re.I)
    prefix = re.compile(r"\b" + re.escape(word), re.I)

    tier_a = [e for e in pool if whole.search(e["en"])]
    tier_b = [e for e in pool if prefix.search(e["en"])]
    cands = tier_a or tier_b
    if not cands:
        return "", ""

    def score(e):
        en = e["en"].strip()
        s = float(e.get("heat") or 0)
        L = len(en)
        if GOOD_LEN[0] <= L <= GOOD_LEN[1]:
            s += 60
        elif L < 8:
            s -= 40
        elif L > 140:
            s -= 50
        pos = en.lower().find(word)
        if pos >= 0:
            s += max(0, 15 - pos // 5)
        s -= 8 * len(re.findall(r"[0-9]", en))
        # 专有名词（句中人名/地名）与引号、冒号多为引文，降权
        s -= 5 * len(re.findall(r"\b[A-Z][a-z]{2,}", en))
        s -= 14 * en.count(":")
        s -= 5 * (en.count('"') + en.count("“") + en.count("”"))
        # 全大写缩写（VIA / ATC 之类）与分号、过多逗号多见于行业文本
        s -= 12 * len(re.findall(r"\b[A-Z]{2,}\b", en))
        s -= 4 * en.count(";")
        if en.count(",") > 2:
            s -= 6 * (en.count(",") - 2)
        if whole.search(en):
            s += 30
        return s

    cands.sort(key=score, reverse=True)
    best = cands[0]
    return best["en"].strip(), (best.get("cn") or "").strip()


# ---------------------------------------------------------------- 形近词
def edit_within(a, b, maxd=2):
    if abs(len(a) - len(b)) > maxd:
        return maxd + 1
    la, lb = len(a), len(b)
    prev = list(range(lb + 1))
    for i in range(1, la + 1):
        cur = [i] + [0] * lb
        best = cur[0]
        ca = a[i - 1]
        for j in range(1, lb + 1):
            cost = 0 if ca == b[j - 1] else 1
            cur[j] = min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            if cur[j] < best:
                best = cur[j]
        if best > maxd:
            return maxd + 1
        prev = cur
    return prev[lb]


# ---------------------------------------------------------------- 生成记录
def build_stage(words, stage):
    # 按字母顺序排列：与官方大纲词汇表的呈现方式一致，便于查找与核对
    ordered = sorted(words)
    recs = []
    for i, w in enumerate(ordered):
        row = ec.get(w) or {}
        v = id2voc.get(w2id.get(w, -1))
        lines = clean_translation(row.get("translation"))
        if not lines and v:
            lines = clean_translation(v.get("paraphrase"))
        short = short_translation(lines)
        detail = "\n".join(lines[:6]) if lines else (v.get("paraphrase") if v else "")
        definition = unescape((row.get("definition") or "").strip())[:400]
        roots = build_roots(w)
        sent, sent_cn = pick_example(w, w2id.get(w))
        recs.append({
            "word": w,
            "phonetic": pick_phonetic(w, row, v),
            "translation": short,
            "trans_detail": detail,
            "definition": definition,
            "pos": pos_tags(w, row, lines),
            "stage": stage,
            "unit": i // WORDS_PER_UNIT + 1,
            "rank": i + 1,
            "sentence": sent,
            "sentence_cn": sent_cn,
            "roots": roots,
            "similar": "",
            "mnemonic": "",
            "_exchange": row.get("exchange") or "",
            "_lines": lines,
        })
    return recs


def dedup_unit_options(recs):
    """同一单元内若短释义完全相同，会让闯关选项出现歧义，用第二义项加以区分"""
    groups = collections.defaultdict(list)
    for r in recs:
        groups[(r["stage"], r["unit"], r["translation"])].append(r)
    fixed = 0
    for (st, un, t), items in groups.items():
        if len(items) < 2:
            continue
        for r in items:
            lines = r.get("_lines") or []
            alt = short_translation(lines[1:], 26) if len(lines) > 1 else ""
            base = short_translation(lines, 40)
            cand = base if base and base != t else (t + "｜" + alt if alt else t)
            if cand and cand != t:
                r["translation"] = cand
                fixed += 1
    return fixed


log("[5/7] 生成词条 ...")
recs4 = build_stage(stage4, STAGE_CET4)
recs6 = build_stage(stage6, STAGE_CET6)
allrecs = recs4 + recs6
log(f"     四级 {len(recs4)} 词 / 六级 {len(recs6)} 词")
log(f"     消歧同单元重复释义: {dedup_unit_options(recs4) + dedup_unit_options(recs6)} 处")

# 形近词：仅在“同阶段”内查找，避免跨级别干扰
log("[6/7] 计算形近词 ...")
for stage, recs in ((STAGE_CET4, recs4), (STAGE_CET6, recs6)):
    bylen = collections.defaultdict(list)
    for r in recs:
        bylen[len(r["word"])].append(r["word"])
    short_map = {r["word"]: r["translation"] for r in recs}
    for r in recs:
        w = r["word"]
        cands = []
        for L in range(len(w) - 2, len(w) + 3):
            for other in bylen.get(L, ()):
                if other == w:
                    continue
                if other[0] != w[0] and other[-1] != w[-1]:
                    continue
                d = edit_within(w, other, 2)
                if d <= 2:
                    cands.append((d, other))
        cands.sort(key=lambda t: (t[0], t[1]))
        picked = []
        for d, other in cands[:8]:
            t = short_translation(clean_translation(ec.get(other, {}).get("translation")), 12)
            if t:
                picked.append((other, t.strip(TAIL_TRIM)))
            if len(picked) >= 4:
                break
        r["similar"] = ";;".join(f"{a}|{b}" for a, b in picked)
        r["mnemonic"] = build_mnemonic(w, r["roots"], picked, r["_exchange"], stage)

# ---------------------------------------------------------------- 写库
log("[7/7] 写入 SQLite ...")
if os.path.exists(OUT):
    os.remove(OUT)
con = sqlite3.connect(OUT)
cur = con.cursor()
cur.executescript("""
CREATE TABLE words(
  id INTEGER PRIMARY KEY,
  word TEXT NOT NULL,
  phonetic TEXT,
  translation TEXT,
  trans_detail TEXT,
  definition TEXT,
  pos TEXT,
  stage INTEGER NOT NULL,
  unit INTEGER NOT NULL,
  rank INTEGER NOT NULL,
  sentence TEXT,
  sentence_cn TEXT,
  roots TEXT,
  similar TEXT,
  mnemonic TEXT
);
CREATE INDEX idx_stage_unit ON words(stage, unit);
CREATE INDEX idx_word ON words(word);
""")
rows = []
for i, r in enumerate(allrecs, start=1):
    rows.append((i, r["word"], r["phonetic"], r["translation"], r["trans_detail"],
                 r["definition"], r["pos"], r["stage"], r["unit"], r["rank"],
                 r["sentence"], r["sentence_cn"], r["roots"], r["similar"], r["mnemonic"]))
cur.executemany("INSERT INTO words VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", rows)
con.commit()
cur.execute("VACUUM")
con.commit()

# 校验
cur.execute("SELECT COUNT(*) FROM words"); n = cur.fetchone()[0]
cur.execute("SELECT stage, COUNT(*), COUNT(DISTINCT unit) FROM words GROUP BY stage")
stat = cur.fetchall()
cur.execute("SELECT COUNT(*) FROM words WHERE sentence<>''"); with_s = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM words WHERE roots<>''"); with_r = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM words WHERE similar<>''"); with_sim = cur.fetchone()[0]
cur.execute("SELECT COUNT(*) FROM words WHERE translation=''"); empty_t = cur.fetchone()[0]
# 同一单元内短释义重复会导致闯关选项歧义，检查一下
dup = 0
cur.execute("SELECT stage,unit,translation,COUNT(*) c FROM words GROUP BY stage,unit,translation HAVING c>1")
dup = len(cur.fetchall())
con.close()

size = os.path.getsize(OUT) / 1024 / 1024
log("=" * 62)
log(f"输出: {OUT}")
log(f"大小: {size:.2f} MB   总词条: {n}")
for st, c, u in stat:
    log(f"  stage={st}  词数={c}  单元数={u}  每单元≈{c/u:.1f}")
log(f"含例句: {with_s} ({with_s*100//n}%)")
log(f"含词根词缀: {with_r} ({with_r*100//n}%)")
log(f"含形近词: {with_sim} ({with_sim*100//n}%)")
log(f"释义为空: {empty_t}   同单元重复释义组: {dup}")
log("=" * 62)
