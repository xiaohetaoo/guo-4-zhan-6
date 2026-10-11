"""打包源码与构建脚本（排除构建产物、机器相关配置与签名密钥）。"""
import os
import zipfile

SRC = r"D:\wby\46\CetVocab"
OUT = r"D:\wby\46\交付物\过四斩六-源码与构建脚本.zip"

SKIP_DIRS = {".gradle", "build", ".idea", ".kotlin", "intermediate"}
SKIP_FILES = {"local.properties", "cetvocab-release.jks"}

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
    return False


def main():
    count = 0
    total = 0
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
