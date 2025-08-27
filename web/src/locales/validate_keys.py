#!/usr/bin/env python3
import json

def get_all_keys(obj, prefix=''):
    """递归获取所有键路径"""
    keys = set()
    if isinstance(obj, dict):
        for key, value in obj.items():
            current_path = f"{prefix}.{key}" if prefix else key
            keys.add(current_path)
            keys.update(get_all_keys(value, current_path))
    return keys

def main():
    # 读取语言包文件
    with open('zh.json', 'r', encoding='utf-8') as f:
        zh_data = json.load(f)
    
    with open('en.json', 'r', encoding='utf-8') as f:
        en_data = json.load(f)
    
    # 获取所有键路径
    zh_keys = get_all_keys(zh_data)
    en_keys = get_all_keys(en_data)
    
    # 比较键
    only_in_zh = zh_keys - en_keys
    only_in_en = en_keys - zh_keys
    common_keys = zh_keys & en_keys
    
    print(f"中文语言包总键数: {len(zh_keys)}")
    print(f"英文语言包总键数: {len(en_keys)}")
    print(f"共同键数: {len(common_keys)}")
    print()
    
    if only_in_zh:
        print("只在中文语言包中存在的键:")
        for key in sorted(only_in_zh):
            print(f"  - {key}")
        print()
    
    if only_in_en:
        print("只在英文语言包中存在的键:")
        for key in sorted(only_in_en):
            print(f"  - {key}")
        print()
    
    if not only_in_zh and not only_in_en:
        print("✅ 两个语言包的键结构完全一致!")
    else:
        print("❌ 两个语言包的键结构不一致")
        
    return len(only_in_zh) + len(only_in_en) == 0

if __name__ == "__main__":
    main()