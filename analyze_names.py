import pandas as pd
import re

# 读取 Excel 数据
df = pd.read_excel('desensitizer-spring-boot/src/main/resources/test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx', header=1)
df.columns = ['姓名', '手机号', '身份证号', '银行卡号', '地址', '国家']

print(f"总姓名数: {len(df)}")

# 当前正则表达式
current_pattern = re.compile(r'^[\u4e00-\u9fa5]{2,4}$')

matched = []
unmatched = []

for name in df['姓名'].dropna():
    name = str(name).strip()
    if current_pattern.match(name):
        matched.append(name)
    else:
        unmatched.append(name)

print(f"\n匹配数: {len(matched)}")
print(f"未匹配数: {len(unmatched)}")
print(f"覆盖率: {len(matched)/len(df)*100:.1f}%")

print("\n=== 未匹配的姓名 (前20个) ===")
for i, name in enumerate(unmatched[:20], 1):
    print(f"{i}. '{name}' (长度: {len(name)})")

print("\n=== 未匹配原因分析 ===")
# 分析原因
cat1 = [n for n in unmatched if len(n) < 2]  # 单字
cat2 = [n for n in unmatched if len(n) > 4]  # 超过4字
cat3 = [n for n in unmatched if not re.match(r'^[\u4e00-\u9fa5]+$', n)]  # 含非中文

print(f"1. 单字名 (长度<2): {len(cat1)} 个")
if cat1:
    print(f"   示例: {cat1[:3]}")

print(f"2. 超长姓名 (长度>4): {len(cat2)} 个")
if cat2:
    print(f"   示例: {cat2[:3]}")

print(f"3. 含非中文字符: {len(cat3)} 个")
if cat3:
    print(f"   示例: {cat3[:3]}")

print(f"4. 其他: {len(unmatched) - len(cat1) - len(cat2) - len(cat3)} 个")
