#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试用例：绕过尝试测试
测试目标：验证各种绕过沙箱的尝试都被正确拦截

预期结果：所有绕过尝试都应该被阻止
"""

import sys

def test_dynamic_import():
    """测试动态导入绕过"""
    print("=" * 60)
    print("测试用例 1: 动态导入绕过 (getattr(__builtins__, 'import'))")
    print("=" * 60)
    
    try:
        # 尝试通过 getattr 获取 __import__
        import_func = getattr(__builtins__, '__import__', None)
        if import_func:
            os_module = import_func('os')
            print(f"❌ 安全漏洞：通过 getattr 动态导入成功，模块：{os_module}")
            return False
        else:
            print("✅ 安全：__builtins__ 中无 __import__ 属性")
            return True
    except SecurityError as e:
        print(f"✅ 安全：动态导入被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：动态导入被阻止 - {type(e).__name__}: {e}")
        return True


def test_importlib_import_module():
    """测试 importlib.import_module 绕过"""
    print("\n" + "=" * 60)
    print("测试用例 2: importlib.import_module 绕过")
    print("=" * 60)
    
    try:
        import importlib
        # 尝试使用 importlib 导入危险模块
        os_module = importlib.import_module('os')
        print(f"❌ 安全漏洞：通过 importlib.import_module 导入成功，模块：{os_module}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：importlib.import_module 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：importlib.import_module 被阻止 - {type(e).__name__}: {e}")
        return True


def test_globals_import():
    """测试通过 globals()['__import__'] 绕过"""
    print("\n" + "=" * 60)
    print("测试用例 3: globals()['__import__'] 绕过")
    print("=" * 60)
    
    try:
        # 尝试从 globals 获取 __import__
        import_func = globals().get('__import__')
        if import_func:
            os_module = import_func('os')
            print(f"❌ 安全漏洞：通过 globals()['__import__'] 导入成功")
            return False
        else:
            print("✅ 安全：globals() 中无 __import__ 属性")
            return True
    except SecurityError as e:
        print(f"✅ 安全：globals() __import__ 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：globals() __import__ 被阻止 - {type(e).__name__}: {e}")
        return True


def test_locals_import():
    """测试通过 locals()['__import__'] 绕过"""
    print("\n" + "=" * 60)
    print("测试用例 4: locals()['__import__'] 绕过")
    print("=" * 60)
    
    try:
        # 尝试从 locals 获取 __import__
        import_func = locals().get('__import__')
        if import_func:
            os_module = import_func('os')
            print(f"❌ 安全漏洞：通过 locals()['__import__'] 导入成功")
            return False
        else:
            print("✅ 安全：locals() 中无 __import__ 属性")
            return True
    except SecurityError as e:
        print(f"✅ 安全：locals() __import__ 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：locals() __import__ 被阻止 - {type(e).__name__}: {e}")
        return True


def test_exec_string_concat():
    """测试通过字符串拼接绕过"""
    print("\n" + "=" * 60)
    print("测试用例 5: 字符串拼接绕过 (eval('im'+'port'))")
    print("=" * 60)
    
    try:
        # 尝试通过字符串拼接构造危险代码
        code = 'im' + 'port os; os.system("whoami")'
        exec(code)
        print("❌ 安全漏洞：字符串拼接绕过成功")
        return False
    except SecurityError as e:
        print(f"✅ 安全：字符串拼接绕过被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：字符串拼接绕过被阻止 - {type(e).__name__}: {e}")
        return True


def test_base64_exec():
    """测试通过 base64 编码绕过"""
    print("\n" + "=" * 60)
    print("测试用例 6: base64 编码绕过")
    print("=" * 60)
    
    try:
        import base64
        # 将危险代码 base64 编码
        dangerous_code = "import os; os.system('whoami')"
        encoded = base64.b64encode(dangerous_code.encode()).decode()
        print(f"编码后的代码：{encoded}")
        
        # 尝试解码并执行
        decoded = base64.b64decode(encoded).decode()
        exec(decoded)
        print("❌ 安全漏洞：base64 编码绕过成功")
        return False
    except SecurityError as e:
        print(f"✅ 安全：base64 编码绕过被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：base64 编码绕过被阻止 - {type(e).__name__}: {e}")
        return True


def test_file_import():
    """测试通过文件导入绕过"""
    print("\n" + "=" * 60)
    print("测试用例 7: 文件导入绕过")
    print("=" * 60)
    
    try:
        # 尝试写入并导入临时模块
        with open('/tmp/dangerous_module.py', 'w') as f:
            f.write('import os; print(os.system("whoami"))')
        
        import sys
        sys.path.insert(0, '/tmp')
        import dangerous_module
        print("❌ 安全漏洞：文件导入绕过成功")
        return False
    except SecurityError as e:
        print(f"✅ 安全：文件导入被拦截 - {e}")
        return True
    except (ImportError, FileNotFoundError, PermissionError) as e:
        print(f"✅ 安全：文件导入被阻止 - {type(e).__name__}: {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：文件导入被阻止 - {type(e).__name__}: {e}")
        return True


def test_os_popen():
    """测试 os.popen 绕过（如果 os 可导入）"""
    print("\n" + "=" * 60)
    print("测试用例 8: os.popen 绕过")
    print("=" * 60)
    
    try:
        import os
        # 尝试使用 popen 执行命令
        result = os.popen('whoami').read()
        print(f"❌ 安全漏洞：os.popen 可执行命令，输出：{result}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：os.popen 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：os.popen 被阻止 - {type(e).__name__}: {e}")
        return True


def test_subprocess_call():
    """测试 subprocess.call 绕过"""
    print("\n" + "=" * 60)
    print("测试用例 9: subprocess.call 绕过")
    print("=" * 60)
    
    try:
        import subprocess
        # 尝试使用 call 执行命令
        result = subprocess.call(['echo', 'dangerous'])
        print(f"❌ 安全漏洞：subprocess.call 可执行，返回：{result}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：subprocess.call 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：subprocess.call 被阻止 - {type(e).__name__}: {e}")
        return True


def test_reload_module():
    """测试 reload 内置函数绕过"""
    print("\n" + "=" * 60)
    print("测试用例 10: reload() 内置函数绕过")
    print("=" * 60)
    
    try:
        # 尝试使用 reload
        import sys
        reload(sys)
        print("❌ 安全漏洞：reload() 可调用")
        return False
    except (NameError, SecurityError) as e:
        print(f"✅ 安全：reload() 被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：reload() 被阻止 - {type(e).__name__}: {e}")
        return True


def main():
    """运行所有绕过尝试测试"""
    print("\n" + "#" * 60)
    print("# 绕过尝试测试套件")
    print("# 测试目标：验证各种绕过沙箱的尝试都被正确拦截")
    print("#" * 60)
    
    results = []
    
    results.append(("动态导入 getattr", test_dynamic_import()))
    results.append(("importlib.import_module", test_importlib_import_module()))
    results.append(("globals()['__import__']", test_globals_import()))
    results.append(("locals()['__import__']", test_locals_import()))
    results.append(("字符串拼接", test_exec_string_concat()))
    results.append(("base64 编码", test_base64_exec()))
    results.append(("文件导入", test_file_import()))
    results.append(("os.popen", test_os_popen()))
    results.append(("subprocess.call", test_subprocess_call()))
    results.append(("reload()", test_reload_module()))
    
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
        print("\n🎉 所有绕过尝试测试通过！沙箱安全性良好！")
        sys.exit(0)
    else:
        print(f"\n⚠️  发现 {total - passed} 个安全漏洞！需要修复！")
        sys.exit(1)


if __name__ == "__main__":
    main()
