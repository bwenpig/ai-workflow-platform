#!/usr/bin/env python3
"""
预装库验证测试脚本
测试目标：验证预装的第三方库（numpy/pandas/Pillow）可正常导入和使用

测试用例：
1. numpy - 数值计算库
2. pandas - 数据处理库
3. Pillow - 图像处理库
4. 其他常用库（requests, beautifulsoup4 等）
"""

import sys
import json
import time
import traceback

def test_numpy():
    """测试 numpy 库"""
    print("[TEST 1] numpy 库测试...")
    try:
        import numpy as np
        print(f"  numpy 版本：{np.__version__}")
        
        # 创建数组
        arr = np.array([1, 2, 3, 4, 5])
        print(f"  数组：{arr}")
        
        # 基本运算
        result = np.sum(arr)
        print(f"  求和：{result}")
        
        # 矩阵运算
        matrix = np.random.rand(3, 3)
        print(f"  3x3 随机矩阵形状：{matrix.shape}")
        
        # 统计计算
        mean_val = np.mean(matrix)
        std_val = np.std(matrix)
        print(f"  矩阵均值：{mean_val:.4f}, 标准差：{std_val:.4f}")
        
        return {
            "status": "success",
            "version": np.__version__,
            "tests_passed": ["array_creation", "sum", "matrix", "statistics"]
        }
    except ImportError as e:
        print(f"  ❌ numpy 导入失败：{e}")
        return {"status": "failed", "error": str(e)}
    except Exception as e:
        print(f"  ❌ numpy 测试失败：{e}")
        return {"status": "failed", "error": str(e), "traceback": traceback.format_exc()}

def test_pandas():
    """测试 pandas 库"""
    print("[TEST 2] pandas 库测试...")
    try:
        import pandas as pd
        print(f"  pandas 版本：{pd.__version__}")
        
        # 创建 DataFrame
        df = pd.DataFrame({
            'name': ['Alice', 'Bob', 'Charlie'],
            'age': [25, 30, 35],
            'city': ['Beijing', 'Shanghai', 'Guangzhou']
        })
        print(f"  DataFrame 形状：{df.shape}")
        print(f"  列名：{list(df.columns)}")
        
        # 基本统计
        age_mean = df['age'].mean()
        print(f"  平均年龄：{age_mean}")
        
        # 数据筛选
        filtered = df[df['age'] > 28]
        print(f"  年龄>28 的记录数：{len(filtered)}")
        
        return {
            "status": "success",
            "version": pd.__version__,
            "tests_passed": ["dataframe_creation", "statistics", "filtering"]
        }
    except ImportError as e:
        print(f"  ❌ pandas 导入失败：{e}")
        return {"status": "failed", "error": str(e)}
    except Exception as e:
        print(f"  ❌ pandas 测试失败：{e}")
        return {"status": "failed", "error": str(e), "traceback": traceback.format_exc()}

def test_pillow():
    """测试 Pillow 库"""
    print("[TEST 3] Pillow 库测试...")
    try:
        from PIL import Image
        import io
        
        # 创建空白图像
        img = Image.new('RGB', (100, 100), color='red')
        print(f"  创建图像：{img.size}, 模式：{img.mode}")
        
        # 图像操作
        img_rotated = img.rotate(45)
        print(f"  旋转后图像：{img_rotated.size}")
        
        # 保存为字节
        buffer = io.BytesIO()
        img.save(buffer, format='PNG')
        img_bytes = buffer.getvalue()
        print(f"  PNG 大小：{len(img_bytes)} bytes")
        
        # 从字节加载
        img_loaded = Image.open(buffer)
        print(f"  加载图像：{img_loaded.size}")
        
        return {
            "status": "success",
            "tests_passed": ["image_creation", "rotation", "save_load"]
        }
    except ImportError as e:
        print(f"  ❌ Pillow 导入失败：{e}")
        return {"status": "failed", "error": str(e)}
    except Exception as e:
        print(f"  ❌ Pillow 测试失败：{e}")
        return {"status": "failed", "error": str(e), "traceback": traceback.format_exc()}

def test_requests():
    """测试 requests 库（网络访问可能被禁用）"""
    print("[TEST 4] requests 库测试...")
    try:
        import requests
        print(f"  requests 版本：{requests.__version__}")
        
        # 注意：网络访问可能被沙箱禁用
        # 这里只测试导入和基本功能
        print(f"  requests 模块成功导入")
        
        return {
            "status": "success",
            "version": requests.__version__,
            "note": "网络访问测试跳过（沙箱环境可能禁用）"
        }
    except ImportError as e:
        print(f"  ❌ requests 导入失败：{e}")
        return {"status": "failed", "error": str(e)}

def test_beautifulsoup4():
    """测试 beautifulsoup4 库"""
    print("[TEST 5] beautifulsoup4 库测试...")
    try:
        from bs4 import BeautifulSoup
        
        # 解析 HTML
        html = """
        <html>
            <head><title>Test Page</title></head>
            <body>
                <h1>Hello World</h1>
                <p class="content">This is a test.</p>
            </body>
        </html>
        """
        
        soup = BeautifulSoup(html, 'lxml')
        title = soup.find('title').text
        heading = soup.find('h1').text
        paragraph = soup.find('p', class_='content').text
        
        print(f"  标题：{title}")
        print(f"  标题：{heading}")
        print(f"  段落：{paragraph}")
        
        return {
            "status": "success",
            "tests_passed": ["html_parsing", "find_element", "class_selector"]
        }
    except ImportError as e:
        print(f"  ❌ beautifulsoup4 导入失败：{e}")
        return {"status": "failed", "error": str(e)}
    except Exception as e:
        print(f"  ❌ beautifulsoup4 测试失败：{e}")
        return {"status": "failed", "error": str(e), "traceback": traceback.format_exc()}

if __name__ == "__main__":
    # 可以指定测试单个库或全部测试
    test_target = sys.argv[1] if len(sys.argv) > 1 else "all"
    
    results = {
        "timestamp": time.time(),
        "tests": {}
    }
    
    if test_target == "all":
        results["tests"]["numpy"] = test_numpy()
        results["tests"]["pandas"] = test_pandas()
        results["tests"]["pillow"] = test_pillow()
        results["tests"]["requests"] = test_requests()
        results["tests"]["beautifulsoup4"] = test_beautifulsoup4()
    elif test_target == "numpy":
        results["tests"]["numpy"] = test_numpy()
    elif test_target == "pandas":
        results["tests"]["pandas"] = test_pandas()
    elif test_target == "pillow":
        results["tests"]["pillow"] = test_pillow()
    elif test_target == "requests":
        results["tests"]["requests"] = test_requests()
    elif test_target == "beautifulsoup4":
        results["tests"]["beautifulsoup4"] = test_beautifulsoup4()
    else:
        results["error"] = f"未知测试目标：{test_target}"
    
    # 统计结果
    total = len(results["tests"])
    passed = sum(1 for t in results["tests"].values() if t.get("status") == "success")
    failed = total - passed
    
    results["summary"] = {
        "total": total,
        "passed": passed,
        "failed": failed,
        "success_rate": f"{(passed/total*100):.1f}%" if total > 0 else "N/A"
    }
    
    print(json.dumps(results, indent=2, ensure_ascii=False))
