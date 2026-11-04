"""打包源码与构建脚本（排除构建产物、机器相关配置与签名密钥）。"""
import os
import sys
import zipfile

# 路径口径与 tools 下其余脚本一致：命令行 SRC=... / OUT=... > 环境变量 > 按脚本位置推导。
# 默认输出到工程同级目录的 dist/ 下，不会去猜某个人的「交付物」目录叫什么。
def _resolve(cli_key, env_key, default):
    for arg in sys.argv[1:]:
        if arg.startswith(cli_key + "="):
            return arg.split("=", 1)[1]
    return os.environ.get(env_key) or default


_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = _resolve("SRC", "CETVOCAB_PROJECT_DIR", _ROOT)
OUT = _resolve("OUT", "CETVOCAB_SOURCE_ZIP",
               os.path.join(os.path.dirname(_ROOT), "dist", "过四斩六-源码与构建脚本.zip"))

SKIP_DIRS = {".gradle", "build", ".idea", ".kotlin", "intermediate"}
SKIP_FILES = {"local.properties", "cetvocab-release.jks"}
# 备份与临时快照不该进交付包：
#   · words_db_rollback.bak 是 v1.4.3 前后留下的一份旧词库快照（7.86 MB），
#     没有任何源码价值，却让源码包凭空多出一大截体积；
#   · words.db.before_hs.bak 由 tools/mark_highschool.py 生成（脚本已改为写到
#     tools/_backup/，这里再兜一层，防止它又被写回 assets 或 tools/ 下）。
SKIP_SUFFIXES = (".bak", ".tmp", ".log")

INCLUDE_TOP = [
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "app/build.gradle.kts",
    "app/proguard-rules.pro",
    "tools",
    "app/src",
    "README-构建说明.md",
]


def should_skip(rel):
    parts = rel.replace("\\", "/").split("/")
    if any(p in SKIP_DIRS for p in parts):
        return True
    if parts[-1] in SKIP_FILES:
        return True
    if parts[-1].endswith(SKIP_SUFFIXES):
        return True
    return False


def main():
    count = 0
    total = 0
    # 默认输出目录（工程同级的 dist/）可能还不存在，先建出来再写包
    parent = os.path.dirname(os.path.abspath(OUT))
    if parent:
        os.makedirs(parent, exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for item in INCLUDE_TOP:
            p = os.path.join(SRC, item)
            if not os.path.exists(p):
                print("skip (missing):", item)
                continue
            if os.path.isfile(p):
                rel = item
                if should_skip(rel):
                    continue
                z.write(p, "CetVocab/" + rel)
                count += 1
                total += os.path.getsize(p)
                continue
            for root, dirs, files in os.walk(p):
                dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
                for f in files:
                    fp = os.path.join(root, f)
                    rel = os.path.relpath(fp, SRC)
                    if should_skip(rel):
                        continue
                    z.write(fp, "CetVocab/" + rel.replace("\\", "/"))
                    count += 1
                    total += os.path.getsize(fp)
    print("files:", count, " raw MB:", round(total / 1048576, 2),
          " zip MB:", round(os.path.getsize(OUT) / 1048576, 2))


if __name__ == "__main__":
    main()
