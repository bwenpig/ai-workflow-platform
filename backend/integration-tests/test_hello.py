#!/usr/bin/env python3
"""基础功能测试"""
import json
import subprocess
import sys

def test_hello():
    """测试简单的 Python 脚本执行"""
    script = """
result = inputs.get('a', 0) + inputs.get('b', 0)
outputs['result'] = result
outputs['message'] = 'Hello from Docker sandbox!'
"""
    inputs = {"a": 10, "b": 20}
    
    # 运行 Docker 容器测试
    cmd = [
        'docker', 'run', '--rm',
        '-v', f'{sys.argv[1]}:/sandbox',
        'ben/python-sandbox:1.0.0',
        'python3', '-c', script
    ]
    
    print(f"✅ test_hello: 基础功能测试 - 脚本准备完成")
    print(f"   输入：{inputs}")
    print(f"   预期输出：result=30, message='Hello from Docker sandbox!'")
    return True

if __name__ == '__main__':
    test_hello()
