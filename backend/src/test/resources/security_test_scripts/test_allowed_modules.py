#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试用例：允许模块测试
测试目标：验证 math/json/requests 等安全模块可以正常使用

预期结果：所有允许的模块都应该可以正常导入和使用
"""

import sys

def test_math_module():
    """测试 math 模块正常使用"""
    print("=" * 60)
    print("测试用例 1: math 模块正常使用")
    print("=" * 60)
    
    try:
        import math
        
        # 测试常用数学函数
        result1 = math.sqrt(16)
        result2 = math.sin(math.pi / 2)
        result3 = math.log(10)
        
        print(f"✅ math.sqrt(16) = {result1}")
        print(f"✅ math.sin(π/2) = {result2}")
        print(f"✅ math.log(10) = {result3}")
        
        # 验证结果正确性
        assert result1 == 4.0, f"sqrt 结果错误：{result1}"
        assert abs(result2 - 1.0) < 0.0001, f"sin 结果错误：{result2}"
        
        print("✅ math 模块功能正常")
        return True
    except ImportError as e:
        print(f"❌ 错误：math 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：math 模块使用异常 - {type(e).__name__}: {e}")
        return False


def test_json_module():
    """测试 json 模块正常使用"""
    print("\n" + "=" * 60)
    print("测试用例 2: json 模块正常使用")
    print("=" * 60)
    
    try:
        import json
        
        # 测试序列化
        data = {"name": "test", "value": 123, "items": [1, 2, 3]}
        json_str = json.dumps(data)
        print(f"✅ 序列化：{json_str}")
        
        # 测试反序列化
        parsed = json.loads(json_str)
        print(f"✅ 反序列化：{parsed}")
        
        # 验证数据完整性
        assert parsed == data, "数据不一致"
        
        print("✅ json 模块功能正常")
        return True
    except ImportError as e:
        print(f"❌ 错误：json 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：json 模块使用异常 - {type(e).__name__}: {e}")
        return False


def test_requests_module():
    """测试 requests 模块正常使用（如果网络允许）"""
    print("\n" + "=" * 60)
    print("测试用例 3: requests 模块正常使用")
    print("=" * 60)
    
    try:
        import requests
        
        # 注意：在沙箱环境中网络可能被禁用
        # 这里只测试模块导入和基本功能
        print(f"✅ requests 版本：{requests.__version__}")
        
        # 测试基本功能（可能需要网络）
        # 如果网络被禁用，这里会失败，但模块本身是允许的
        try:
            response = requests.get('https://httpbin.org/get', timeout=5)
            print(f"✅ 网络请求成功，状态码：{response.status_code}")
        except requests.exceptions.ConnectionError:
            print("⚠️  网络连接被禁用（预期行为，沙箱环境）")
        except requests.exceptions.Timeout:
            print("⚠️  网络请求超时（预期行为，沙箱环境）")
        
        print("✅ requests 模块可导入")
        return True
    except ImportError as e:
        print(f"❌ 错误：requests 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：requests 模块使用异常 - {type(e).__name__}: {e}")
        return False


def test_datetime_module():
    """测试 datetime 模块正常使用"""
    print("\n" + "=" * 60)
    print("测试用例 4: datetime 模块正常使用")
    print("=" * 60)
    
    try:
        from datetime import datetime, timedelta
        
        # 测试当前时间
        now = datetime.now()
        print(f"✅ 当前时间：{now}")
        
        # 测试时间计算
        tomorrow = now + timedelta(days=1)
        print(f"✅ 明天时间：{tomorrow}")
        
        # 测试格式化
        formatted = now.strftime("%Y-%m-%d %H:%M:%S")
        print(f"✅ 格式化时间：{formatted}")
        
        print("✅ datetime 模块功能正常")
        return True
    except ImportError as e:
        print(f"❌ 错误：datetime 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：datetime 模块使用异常 - {type(e).__name__}: {e}")
        return False


def test_re_module():
    """测试 re 模块正常使用"""
    print("\n" + "=" * 60)
    print("测试用例 5: re 模块正常使用")
    print("=" * 60)
    
    try:
        import re
        
        # 测试正则匹配
        pattern = r'\d+'
        text = "abc123def456"
        matches = re.findall(pattern, text)
        print(f"✅ 正则匹配结果：{matches}")
        
        # 验证结果
        assert matches == ['123', '456'], f"匹配结果错误：{matches}"
        
        # 测试替换
        replaced = re.sub(pattern, 'X', text)
        print(f"✅ 正则替换结果：{replaced}")
        
        print("✅ re 模块功能正常")
        return True
    except ImportError as e:
        print(f"❌ 错误：re 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：re 模块使用异常 - {type(e).__name__}: {e}")
        return False


def test_collections_module():
    """测试 collections 模块正常使用"""
    print("\n" + "=" * 60)
    print("测试用例 6: collections 模块正常使用")
    print("=" * 60)
    
    try:
        from collections import defaultdict, Counter, OrderedDict
        
        # 测试 defaultdict
        dd = defaultdict(list)
        dd['key'].append(1)
        print(f"✅ defaultdict: {dict(dd)}")
        
        # 测试 Counter
        counter = Counter(['a', 'b', 'a', 'c', 'b', 'a'])
        print(f"✅ Counter: {counter}")
        
        # 测试 OrderedDict
        od = OrderedDict()
        od['first'] = 1
        od['second'] = 2
        print(f"✅ OrderedDict: {dict(od)}")
        
        print("✅ collections 模块功能正常")
        return True
    except ImportError as e:
        print(f"❌ 错误：collections 模块无法导入 - {e}")
        return False
    except Exception as e:
        print(f"❌ 错误：collections 模块使用异常 - {type(e).__name__}: {e}")
        return False


def main():
    """运行所有允许模块测试"""
    print("\n" + "#" * 60)
    print("# 允许模块测试套件")
    print("# 测试目标：验证 math/json/requests/datetime/re/collections 等安全模块可用")
    print("#" * 60)
    
    results = []
    
    results.append(("math 模块", test_math_module()))
    results.append(("json 模块", test_json_module()))
    results.append(("requests 模块", test_requests_module()))
    results.append(("datetime 模块", test_datetime_module()))
    results.append(("re 模块", test_re_module()))
    results.append(("collections 模块", test_collections_module()))
    
    # 汇总结果
    print("\n" + "=" * 60)
    print("测试结果汇总")
    print("=" * 60)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status}: {name}")
    
    print(f"\n总计：{passed}/{total} 通过")
    
    if passed == total:
        print("\n🎉 所有允许模块测试通过！")
        sys.exit(0)
    else:
        print(f"\n⚠️  {total - passed} 个模块测试失败！")
        sys.exit(1)


if __name__ == "__main__":
    main()
