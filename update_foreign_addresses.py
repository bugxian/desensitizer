import pandas as pd
import re

# 读取Excel文件
df = pd.read_excel('desensitizer-spring-boot/src/main/resources/test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx', header=None)

print('=== 更新外国地址的预期脱敏值 ===')
print(f'Excel结构: {len(df.columns)} 列, {len(df)} 行')

updated_count = 0

# 从第3行开始处理（跳过前两行标题）
for i in range(2, len(df)):
    addr = str(df.iloc[i, 4]).strip()  # 原始地址
    current_expected = str(df.iloc[i, 10]).strip()  # 当前预期脱敏值
    country = str(df.iloc[i, 5]).strip()  # 国家
    
    # 跳过空地址
    if addr in ['nan', '', 'None']:
        continue
    
    # 检查是否为纯中文地址（不处理）
    if any('\u4e00' <= c <= '\u9fff' for c in addr) and not any(('\u3040' <= c <= '\u309F') or ('\u30A0' <= c <= '\u30FF') for c in addr):
        continue
    
    # 计算新的预期脱敏值
    new_expected = None
    
    # 1. 美国地址格式：123 Street Name, City, State ZIP
    us_pattern = re.compile(r'^(.+),\s*([A-Za-z\s]+),\s*([A-Z]{2})\s*(\d{5})(-.+)?$')
    us_match = us_pattern.match(addr)
    if us_match:
        city = us_match.group(2).strip()
        state = us_match.group(3)
        zipcode = us_match.group(4)
        new_expected = f'{city}, {state} {zipcode}'
    
    # 2. 德国地址格式：Street, PostalCode City
    if not new_expected:
        de_pattern = re.compile(r'^(.+),\s*(\d{5})\s+([A-Za-z\süöäßÜÖÄß-]+)$')
        de_match = de_pattern.match(addr)
        if de_match:
            postcode = de_match.group(2)
            city = de_match.group(3).strip()
            new_expected = f'{postcode} {city}'
    
    # 3. 法国地址格式：..., PostalCode City
    if not new_expected:
        fr_pattern = re.compile(r'^(.+),\s*(\d{5})\s+([A-Za-z\s-]+)$')
        fr_match = fr_pattern.match(addr)
        if fr_match:
            postcode = fr_match.group(2)
            city = fr_match.group(3).strip()
            new_expected = f'{postcode} {city}'
    
    # 4. 英国地址格式：... City, Postcode
    if not new_expected:
        uk_pattern = re.compile(r'^(.+),\s*([A-Za-z\s]+),\s*([A-Z]{1,2}\d[A-Z\d]?\s*\d[A-Z]{2})$')
        uk_match = uk_pattern.match(addr)
        if uk_match:
            city = uk_match.group(2).strip()
            postcode = uk_match.group(3)
            new_expected = f'{city}, {postcode}'
    
    # 5. 韩国地址格式
    if not new_expected:
        # 格式1：광역시 구 ... 或 特别自治市 구 ...
        kr_pattern1 = re.compile(r'^([가-힣]+광역시|[가-힣]+특별[자치]?시)\s+([가-힣]+구)\s*.+$')
        kr_match1 = kr_pattern1.match(addr)
        if kr_match1:
            city = kr_match1.group(1)
            gu = kr_match1.group(2)
            new_expected = f'{city} {gu}***'
    
    # 格式2：특별자치도 시 구 ...
    if not new_expected:
        kr_pattern2 = re.compile(r'^([가-힣]+특별자치도)\s+([가-힣]+시)\s+([가-힣]+구)\s*.+$')
        kr_match2 = kr_pattern2.match(addr)
        if kr_match2:
            do = kr_match2.group(1)
            si = kr_match2.group(2)
            gu = kr_match2.group(3)
            new_expected = f'{do} {si} {gu}***'
    
    # 格式3：도 시/군 ...
    if not new_expected:
        kr_pattern3 = re.compile(r'^([가-힣]+도)\s+([가-힣]+[시군])\s*.+$')
        kr_match3 = kr_pattern3.match(addr)
        if kr_match3:
            do = kr_match3.group(1)
            sigun = kr_match3.group(2)
            new_expected = f'{do} {sigun}***'
    
    # 格式4：特别自治市 구 ...（세종특별자치시等）
    if not new_expected:
        kr_pattern4 = re.compile(r'^(세종특별자치시)\s+([가-힣]+구)\s*.+$')
        kr_match4 = kr_pattern4.match(addr)
        if kr_match4:
            city = kr_match4.group(1)
            gu = kr_match4.group(2)
            new_expected = f'{city} {gu}***'
    
    # 6. 日本地址格式：都道府県市区町村...
    if not new_expected:
        # 检查是否包含日语假名
        has_japanese = any(('\u3040' <= c <= '\u309F') or ('\u30A0' <= c <= '\u30FF') for c in addr)
        if has_japanese:
            # 匹配到町/村级别
            jp_pattern = re.compile(r'^([\u3040-\u30FF\u4E00-\u9FA5]+[県府都道])([\u3040-\u30FF\u4E00-\u9FA5]+?市)([\u3040-\u30FF\u4E00-\u9FA5]+?[町村]).*$')
            jp_match = jp_pattern.match(addr)
            if jp_match:
                pref = jp_match.group(1)
                city = jp_match.group(2)
                town = jp_match.group(3)
                new_expected = pref + city + town + '***'
            else:
                # 匹配到市/区级别
                jp_pattern2 = re.compile(r'^([\u3040-\u30FF\u4E00-\u9FA5]+[県府都道])([\u3040-\u30FF\u4E00-\u9FA5]+[市区町村]).*$')
                jp_match2 = jp_pattern2.match(addr)
                if jp_match2:
                    pref = jp_match2.group(1)
                    city = jp_match2.group(2)
                    new_expected = pref + city + '***'
    
    # 7. 通用处理：保留最后两个逗号分隔部分
    if not new_expected and ',' in addr:
        parts = addr.split(',')
        if len(parts) >= 2:
            last_part = parts[-1].strip()
            second_last = parts[-2].strip()
            new_expected = f'{second_last}, {last_part}'
    
    # 如果计算出了新的预期值，并且与当前值不同，则更新
    if new_expected and new_expected != current_expected:
        print(f'行{i+1}: {country}')
        print(f'  原始: {addr}')
        print(f'  旧预期: {current_expected}')
        print(f'  新预期: {new_expected}')
        print()
        df.iloc[i, 10] = new_expected
        updated_count += 1

# 保存更新后的Excel文件
df.to_excel('desensitizer-spring-boot/src/main/resources/test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx', index=False, header=False)

print(f'\\n✅ 完成！共更新 {updated_count} 条外国地址的预期脱敏值')
