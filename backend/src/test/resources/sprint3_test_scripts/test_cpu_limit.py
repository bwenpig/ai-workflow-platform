#!/usr/bin/env python3
"""
CPU 限制测试脚本
测试目标：验证高 CPU 负载时不会阻塞系统，且能在合理时间内完成或被限制

测试用例：
1. 正常 CPU 使用 - 应成功完成
2. 高 CPU 负载（计算密集型）- 应完成或被限制
3. 无限循环（带超时保护）- 应被超时终止
"""

import sys
import json
import time
import math

def test_normal_cpu():
    """测试 1: 正常 CPU 使用"""
    print("[TEST 1] 正常 CPU 使用测试开始...")
    start_time = time.time()
    
    result = 0
    for i in range(100000):
        result += math.sqrt(i) * math.sin(i)
    
    elapsed = time.time() - start_time
    print(f"[TEST 1] 完成，耗时：{elapsed:.3f}秒")
    return {"status": "success", "elapsed_seconds": elapsed, "result": result}

def test_high_cpu_load():
    """测试 2: 高 CPU 负载"""
    print("[TEST 2] 高 CPU 负载测试开始...")
    print("  执行 1000 万次浮点运算...")
    start_time = time.time()
    
    result = 0
    iterations = 10_000_000
    for i in range(iterations):
        result += math.sqrt(i) * math.sin(i) * math.cos(i)
        if i % 1_000_000 == 0:
            elapsed = time.time() - start_time
            print(f"  进度：{i}/{iterations}, 已耗时：{elapsed:.3f}秒")
    
    elapsed = time.time() - start_time
    print(f"[TEST 2] 完成，总耗时：{elapsed:.3f}秒")
    return {"status": "success", "elapsed_seconds": elapsed, "iterations": iterations}

def test_infinite_loop():
    """测试 3: 无限循环（应被超时终止）"""
    print("[TEST 3] 无限循环测试开始...")
    print("  警告：此测试应被超时机制终止")
    
    start_time = time.time()
    counter = 0
    
    try:
        while True:
            counter += 1
            if counter % 1_000_000 == 0:
                elapsed = time.time() - start_time
                print(f"  循环次数：{counter}, 已耗时：{elapsed:.3f}秒")
    except Exception as e:
        elapsed = time.time() - start_time
        print(f"[TEST 3] 在 {elapsed:.3f}秒后被终止：{e}")
        return {"status": "terminated", "elapsed_seconds": elapsed, "counter": counter}
    
    # 如果到这里，说明没有被终止（失败）
    elapsed = time.time() - start_time
    return {"status": "warning", "message": "未被终止", "elapsed_seconds": elapsed, "counter": counter}

def test_cpu_intensive_with_check():
    """测试 4: CPU 密集型任务（带时间检查）"""
    print("[TEST 4] CPU 密集型任务（带时间检查）...")
    print("  目标：在 5 秒内完成计算")
    
    start_time = time.time()
    max_duration = 5.0  # 秒
    result = 0
    iterations = 0
    
    while time.time() - start_time < max_duration:
        result += math.sqrt(iterations) * math.sin(iterations)
        iterations += 1
        if iterations % 1_000_000 == 0:
            elapsed = time.time() - start_time
            print(f"  进度：{iterations}次迭代，已耗时：{elapsed:.3f}秒")
    
    elapsed = time.time() - start_time
    print(f"[TEST 4] 完成，总迭代：{iterations}, 总耗时：{elapsed:.3f}秒")
    return {"status": "success", "elapsed_seconds": elapsed, "iterations": iterations}

if __name__ == "__main__":
    test_type = sys.argv[1] if len(sys.argv) > 1 else "normal"
    
    results = {
        "test_type": test_type,
        "timestamp": time.time()
    }
    
    if test_type == "normal":
        results["result"] = test_normal_cpu()
    elif test_type == "high_load":
        results["result"] = test_high_cpu_load()
    elif test_type == "infinite":
        results["result"] = test_infinite_loop()
    elif test_type == "intensive":
        results["result"] = test_cpu_intensive_with_check()
    else:
        results["error"] = f"未知测试类型：{test_type}"
    
    print(json.dumps(results, indent=2))
