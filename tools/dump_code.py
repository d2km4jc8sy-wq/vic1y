import os

# 项目根目录（根据你的实际路径改）
project_dir = r"C:\Users\Administrator\Desktop\S1Client"

# 输出文件
output_file = os.path.join(project_dir, "all_code.txt")

# 要排除的文件夹
exclude_dirs = {".gradle", "build", ".idea", "gradle", ".git"}

# 要收集的文件扩展名
include_exts = {".kt", ".java", ".xml", ".kts", ".gradle", ".properties", ".pro"}

def should_skip(path):
    parts = path.split(os.sep)
    for p in parts:
        if p in exclude_dirs:
            return True
    return False

with open(output_file, "w", encoding="utf-8") as out:
    for root, dirs, files in os.walk(project_dir):
        # 过滤目录
        dirs[:] = [d for d in dirs if d not in exclude_dirs]

        for f in sorted(files):
            ext = os.path.splitext(f)[1]
            if ext not in include_exts:
                continue

            full = os.path.join(root, f)
            rel = os.path.relpath(full, project_dir)

            out.write("\n")
            out.write("=" * 80 + "\n")
            out.write(f"文件: {rel}\n")
            out.write("=" * 80 + "\n")

            try:
                with open(full, "r", encoding="utf-8") as src:
                    out.write(src.read())
            except Exception as e:
                out.write(f"(读取失败: {e})\n")

print(f"完成！所有源码已写入: {output_file}")