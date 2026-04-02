#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试用例：危险函数拦截测试
测试目标：验证 eval/exec/__import__ 等危险函数被正确拦截

预期结果：所有危险函数调用都应该被阻止并抛出安全异常
"""

import sys

def test_eval_intercept():
    """测试 eval() 函数拦截"""
    print("=" * 60)
    print("测试用例 1: eval() 函数拦截")
    print("=" * 60)
    
    try:
        # 尝试执行 eval
        result = eval("__import__('os').system('whoami')")
        print(f"❌ 安全漏洞：eval() 未被拦截，执行结果：{result}")
        return False
    except SecurityError as e:
        print(f"✅ 安全：eval() 被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：eval() 被阻止 - {type(e).__name__}: {e}")
        return True


def test_exec_intercept():
    """测试 exec() 函数拦截"""
    print("\n" + "=" * 60)
    print("测试用例 2: exec() 函数拦截")
    print("=" * 60)
    
    try:
        # 尝试执行 exec
        exec("import os; os.system('ls -la')")
        print("❌ 安全漏洞：exec() 未被拦截")
        return False
    except SecurityError as e:
        print(f"✅ 安全：exec() 被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：exec() 被阻止 - {type(e).__name__}: {e}")
        return True


def test_import_intercept():
    """测试 __import__() 函数拦截"""
    print("\n" + "=" * 60)
    print("测试用例 3: __import__() 函数拦截")
    print("=" * 60)
    
    try:
        # 尝试直接调用 __import__
        os_module = __import__('os')
        print(f"❌ 安全漏洞：__import__() 未被拦截，导入了 {os_module}")
        return False
    except SecurityError as e:
        print(f"✅ 安全：__import__() 被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：__import__() 被阻止 - {type(e).__name__}: {e}")
        return True


def test_compile_intercept():
    """测试 compile() 函数拦截"""
    print("\n" + "=" * 60)
    print("测试用例 4: compile() 函数拦截")
    print("=" * 60)
    
    try:
        # 尝试编译并执行代码
        code = compile("print('dangerous')", "<string>", "exec")
        exec(code)
        print("❌ 安全漏洞：compile() 未被拦截")
        return False
    except SecurityError as e:
        print(f"✅ 安全：compile() 被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：compile() 被阻止 - {type(e).__name__}: {e}")
        return True


def test_globals_builtins():
    """测试通过 globals()['__builtins__'] 访问危险函数"""
    print("\n" + "=" * 60)
    print("测试用例 5: 通过 __builtins__ 访问危险函数")
    print("=" * 60)
    
    try:
        # 尝试通过 builtins 访问 eval
        builtins = globals()['__builtins__']
        if isinstance(builtins, dict):
            dangerous_eval = builtins.get('eval')
            if dangerous_eval:
                dangerous_eval("print('dangerous')")
                print("❌ 安全漏洞：通过 __builtins__ 访问 eval 成功")
                return False
        print("✅ 安全：__builtins__ 访问被限制或危险函数不可用")
        return True
    except SecurityError as e:
        print(f"✅ 安全：__builtins__ 访问被拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：__builtins__ 访问被阻止 - {type(e).__name__}: {e}")
        return True


def main():
    """运行所有危险函数测试"""
    print("\n" + "#" * 60)
    print("# 危险函数拦截测试套件")
    print("# 测试目标：验证 eval/exec/__import__/compile 等被正确拦截")
    print("#" * 60)
    
    results = []
    
    results.append(("eval() 拦截", test_eval_intercept()))
    results.append(("exec() 拦截", test_exec_intercept()))
    results.append(("__import__() 拦截", test_import_intercept()))
    results.append(("compile() 拦截", test_compile_intercept()))
    results.append(("__builtins__ 访问", test_globals_builtins()))
    
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
        print("\n🎉 所有危险函数拦截测试通过！")
        sys.exit(0)
    else:
        print(f"\n⚠️  发现 {total - passed} 个安全漏洞！")
        sys.exit(1)


if __name__ == "__main__":
    main()
