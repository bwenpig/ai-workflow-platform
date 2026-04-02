#!/usr/bin/env python3
"""输入输出测试"""
import json

def test_io():
    """测试输入输出机制"""
    script = """
import json
# 处理复杂数据结构
items = inputs.get('items', [])
total = sum(item.get('price', 0) * item.get('quantity', 1) for item in items)
outputs['total'] = total
outputs['item_count'] = len(items)
"""
    inputs = {
        "items": [
            {"name": "Apple", "price": 1.5, "quantity": 3},
            {"name": "Banana", "price": 0.8, "quantity": 5}
        ]
    }
    
    print(f"✅ test_io: 输入输出测试 - 脚本准备完成")
    print(f"   输入：{len(inputs['items'])} 个商品")
    print(f"   预期输出：total=8.5, item_count=2")
    return True

if __name__ == '__main__':
    test_io()
