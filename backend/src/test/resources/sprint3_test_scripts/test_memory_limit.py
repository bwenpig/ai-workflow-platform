#!/usr/bin/env python3
"""
内存限制测试脚本
测试目标：验证内存超过 128MB 时进程应被终止

测试用例：
1. 正常内存使用（<128MB）- 应成功
2. 内存超限（>128MB）- 应被终止
3. 内存接近限制（~120MB）- 应成功但可能有警告
"""

import sys
import json
import time

def get_memory_usage_mb():
    """获取当前进程内存使用量（MB）"""
    import resource
    usage = resource.getrusage(resource.RUSAGE_SELF)
    # ru_maxrss 单位是 KB (Linux) 或 bytes (macOS)
    if sys.platform == 'darwin':
        return usage.ru_maxrss / (1024 * 1024)
    else:
        return usage.ru_maxrss / 1024

def test_normal_memory():
    """测试 1: 正常内存使用（约 50MB）"""
    print("[TEST 1] 正常内存使用测试开始...")
    data = []
    target_mb = 50
    chunk_size = 1024 * 1024  # 1MB chunks
    
    for i in range(target_mb):
        data.append([0] * 1000)
        if i % 10 == 0:
            mem = get_memory_usage_mb()
            print(f"  已分配 {i}MB, 当前内存：{mem:.2f}MB")
    
    final_mem = get_memory_usage_mb()
    print(f"[TEST 1] 完成，最终内存：{final_mem:.2f}MB")
    return {"status": "success", "memory_mb": final_mem}

def test_memory_limit_exceeded():
    """测试 2: 内存超限（目标 200MB，应被终止）"""
    print("[TEST 2] 内存超限测试开始...")
    print("  目标：分配 200MB 内存（应触发限制）")
    data = []
    target_mb = 200
    chunk_size = 1024 * 1024
    
    try:
        for i in range(target_mb):
            data.append([0] * 1000)
            if i % 10 == 0:
                mem = get_memory_usage_mb()
                print(f"  已分配 {i}MB, 当前内存：{mem:.2f}MB")
            
            # 检查是否超过限制
            if mem > 128:
                print(f"[TEST 2] 内存超限！当前：{mem:.2f}MB > 128MB")
                break
        
        print(f"[TEST 2] 警告：未被终止，最终内存：{get_memory_usage_mb():.2f}MB")
        return {"status": "warning", "message": "未被终止，需检查资源限制配置"}
    except MemoryError as e:
        print(f"[TEST 2] 捕获 MemoryError: {e}")
        return {"status": "terminated", "reason": "MemoryError"}
    except Exception as e:
        print(f"[TEST 2] 进程被终止：{e}")
        return {"status": "terminated", "reason": str(e)}

def test_memory_near_limit():
    """测试 3: 内存接近限制（约 120MB）"""
    print("[TEST 3] 内存接近限制测试开始...")
    print("  目标：分配约 120MB 内存")
    data = []
    target_mb = 120
    
    for i in range(target_mb):
        data.append([0] * 1000)
        if i % 10 == 0:
            mem = get_memory_usage_mb()
            print(f"  已分配 {i}MB, 当前内存：{mem:.2f}MB")
    
    final_mem = get_memory_usage_mb()
    print(f"[TEST 3] 完成，最终内存：{final_mem:.2f}MB")
    return {"status": "success", "memory_mb": final_mem, "warning": "接近内存限制"}

if __name__ == "__main__":
    # 从命令行参数获取测试类型
    test_type = sys.argv[1] if len(sys.argv) > 1 else "normal"
    
    results = {
        "test_type": test_type,
        "timestamp": time.time()
    }
    
    if test_type == "normal":
        results["result"] = test_normal_memory()
    elif test_type == "exceeded":
        results["result"] = test_memory_limit_exceeded()
    elif test_type == "near_limit":
        results["result"] = test_memory_near_limit()
    else:
        results["error"] = f"未知测试类型：{test_type}"
    
    # 输出 JSON 结果
    print(json.dumps(results, indent=2))
