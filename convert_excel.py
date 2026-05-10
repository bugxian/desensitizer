#!/usr/bin/env python3
import pandas as pd
import re

df = pd.read_excel('desensitizer-spring-boot/src/main/resources/test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx', header=1)
df.columns = ['姓名', '手机号', '身份证号', '银行卡号', '地址', '国家']

print(f"总数据行数: {len(df)}")

# 生成 accuracy-tests.csv
accuracy_lines = ['# 脱敏准确率测试用例 - 从Excel导入']

phone_count = 0
for _, row in df.iterrows():
    phone = str(row['手机号']).strip()
    if phone and phone != 'nan':
        expected = phone[:3] + '****' + phone[-4:]
        accuracy_lines.append(f"PHONE,{phone},{expected}")
        phone_count += 1
print(f"手机号: {phone_count} 条")

id_card_count = 0
for _, row in df.iterrows():
    id_card = str(row['身份证号']).strip()
    if id_card and id_card != 'nan':
        expected = id_card[:6] + '********' + id_card[-4:]
        accuracy_lines.append(f"ID_CARD,{id_card},{expected}")
        id_card_count += 1
print(f"身份证号: {id_card_count} 条")

bank_card_count = 0
for _, row in df.iterrows():
    bank_card = str(row['银行卡号']).strip()
    if bank_card and bank_card != 'nan':
        expected = bank_card[:6] + '********' + bank_card[-4:]
        accuracy_lines.append(f"BANK_CARD,{bank_card},{expected}")
        bank_card_count += 1
print(f"银行卡号: {bank_card_count} 条")

name_count = 0
for _, row in df.iterrows():
    name = str(row['姓名']).strip()
    if name and name != 'nan':
        length = len(name)
        if length == 2:
            expected = name[0] + '*'
        elif length == 3:
            expected = name[0] + '*' + name[-1]
        elif length >= 4:
            expected = name[0] + '**' + name[-1]
        else:
            expected = name
        accuracy_lines.append(f"NAME,{name},{expected}")
        name_count += 1
print(f"姓名: {name_count} 条")

addr_count = 0
for _, row in df.iterrows():
    addr = str(row['地址']).strip()
    if addr and addr != 'nan':
        # 检测外国地址并按对应格式脱敏
        # 美国地址 (包含州代码如 NY, MH, MI)
        us_match = re.search(r',\s*([A-Za-z\s]+),\s*([A-Z]{2})\s*(\d{5})$', addr)
        if us_match:
            city = us_match.group(1).strip()
            state = us_match.group(2)
            zipcode = us_match.group(3)
            expected = f"{city}, {state} {zipcode}"
            addr_count += 1
            accuracy_lines.append(f"ADDRESS,{addr},{expected}")
            continue
        
        # 英国地址 (包含英国邮编)
        uk_match = re.search(r',\s*([A-Za-z\s]+),\s*([A-Z]{1,2}\d[A-Z\d]?\s*\d[A-Z]{2})$', addr)
        if uk_match:
            city = uk_match.group(1).strip()
            postcode = uk_match.group(2)
            expected = f"{city}, {postcode}"
            addr_count += 1
            accuracy_lines.append(f"ADDRESS,{addr},{expected}")
            continue
        
        # 法国地址 (5位数字邮编 + 城市名结尾)
        fr_match = re.search(r',\s*(\d{5})\s+([A-Za-z\s-]+)$', addr)
        if fr_match:
            postcode = fr_match.group(1)
            city = fr_match.group(2)
            expected = f"{postcode} {city}"
            addr_count += 1
            accuracy_lines.append(f"ADDRESS,{addr},{expected}")
            continue
        
        # 韩国地址 (包含 광역시)
        kr_match = re.search(r'([가-힣]+광역시)\s+([가-힣]+구)', addr)
        if kr_match:
            city = kr_match.group(1)
            gu = kr_match.group(2)
            expected = f"{city} {gu}***"
            addr_count += 1
            accuracy_lines.append(f"ADDRESS,{addr},{expected}")
            continue
        
        # 日本地址 (包含県或市)
        jp_match = re.search(r'([\u3040-\u30FF\u4E00-\u9FA5]+[県府都道])([\u3040-\u30FF\u4E00-\u9FA5]+[市区町村])', addr)
        if jp_match:
            pref = jp_match.group(1)
            city = jp_match.group(2)
            expected = f"{pref}{city}***"
            addr_count += 1
            accuracy_lines.append(f"ADDRESS,{addr},{expected}")
            continue
        
        # 中文地址
        if len(addr) <= 10:
            expected = addr
        elif '区' in addr or '县' in addr:
            index = max(addr.rfind('区'), addr.rfind('县'))
            if index > 0 and index < len(addr) - 1:
                expected = addr[:index + 1] + '***'
            else:
                expected = '***'
        elif '市' in addr:
            index = addr.find('市')
            if index > 0 and index < len(addr) - 3:
                expected = addr[:index + 1] + '***'
            else:
                expected = '***'
        else:
            expected = '***'
        
        accuracy_lines.append(f"ADDRESS,{addr},{expected}")
        addr_count += 1
print(f"地址: {addr_count} 条")

with open('desensitizer-spring-boot/src/main/resources/test-data/accuracy-tests.csv', 'w', encoding='utf-8') as f:
    for line in accuracy_lines:
        f.write(line + '\n')
print(f"\n✅ 已生成 accuracy-tests.csv: {len(accuracy_lines) - 1} 条测试用例")

# 生成 coverage-valid.txt（带类型前缀，便于统计）
valid_lines = ['# 有效样本 - 从Excel导入']
valid_lines += [f"PHONE:{str(row['手机号']).strip()}" for _, row in df.iterrows() if str(row['手机号']).strip() and str(row['手机号']).strip() != 'nan']
valid_lines += [f"ID_CARD:{str(row['身份证号']).strip()}" for _, row in df.iterrows() if str(row['身份证号']).strip() and str(row['身份证号']).strip() != 'nan']
valid_lines += [f"BANK_CARD:{str(row['银行卡号']).strip()}" for _, row in df.iterrows() if str(row['银行卡号']).strip() and str(row['银行卡号']).strip() != 'nan']
valid_lines += [f"NAME:{str(row['姓名']).strip()}" for _, row in df.iterrows() if str(row['姓名']).strip() and str(row['姓名']).strip() != 'nan']
valid_lines += [f"ADDRESS:{str(row['地址']).strip()}" for _, row in df.iterrows() if str(row['地址']).strip() and str(row['地址']).strip() != 'nan']

with open('desensitizer-spring-boot/src/main/resources/test-data/coverage-valid.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(valid_lines))
print(f"✅ 已生成 coverage-valid.txt: {len(valid_lines) - 1} 条有效样本")

# 生成 coverage-invalid.txt
invalid_lines = ['# 无效样本 - 不应该被脱敏的数据']
invalid_lines += ['test', '12345', 'abc', 'hello world', 'test@test', '123', '654321']
invalid_lines += ['1', '2', '3', '12', '34', '111', '222']
invalid_lines += ['user', 'admin', 'root', 'guest', 'test123']
invalid_lines += ['订单号:20240101001', '金额:99.99', '状态:成功']

with open('desensitizer-spring-boot/src/main/resources/test-data/coverage-invalid.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(invalid_lines))
print(f"✅ 已生成 coverage-invalid.txt: {len(invalid_lines) - 1} 条无效样本")

print("\n📁 所有文件已更新完成！")
