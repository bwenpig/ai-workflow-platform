#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试用例：模块导入限制测试
测试目标：验证 os/subprocess/socket 等危险模块被正确拦截

预期结果：所有危险模块导入都应该被阻止并抛出安全异常
"""

import sys

def test_os_module():
    """测试 os 模块导入拦截"""
    print("=" * 60)
    print("测试用例 1: os 模块导入拦截")
    print("=" * 60)
    
    try:
        import os
        # 如果导入成功，尝试使用危险功能
        result = os.system('echo "dangerous command"')
        print(f"❌ 安全漏洞：os 模块可导入并使用，system() 返回：{result}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：os 模块被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：os 模块被阻止 - {type(e).__name__}: {e}")
        return True


def test_subprocess_module():
    """测试 subprocess 模块导入拦截"""
    print("\n" + "=" * 60)
    print("测试用例 2: subprocess 模块导入拦截")
    print("=" * 60)
    
    try:
        import subprocess
        # 如果导入成功，尝试执行命令
        result = subprocess.run(['echo', 'dangerous'], capture_output=True)
        print(f"❌ 安全漏洞：subprocess 模块可导入并使用，输出：{result.stdout}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：subprocess 模块被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：subprocess 模块被阻止 - {type(e).__name__}: {e}")
        return True


def test_socket_module():
    """测试 socket 模块导入拦截"""
    print("\n" + "=" * 60)
    print("测试用例 3: socket 模块导入拦截")
    print("=" * 60)
    
    try:
        import socket
        # 如果导入成功，尝试创建网络连接
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        print(f"❌ 安全漏洞：socket 模块可导入并创建套接字")
        s.close()
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：socket 模块被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：socket 模块被阻止 - {type(e).__name__}: {e}")
        return True


def test_ctypes_module():
    """测试 ctypes 模块导入拦截（可用于绕过沙箱）"""
    print("\n" + "=" * 60)
    print("测试用例 4: ctypes 模块导入拦截")
    print("=" * 60)
    
    try:
        import ctypes
        # 如果导入成功，尝试加载 C 库
        libc = ctypes.CDLL('libc.so.6')
        print(f"❌ 安全漏洞：ctypes 模块可导入并加载 C 库")
        return False
    except (ImportError, SecurityError, OSError) as e:
        print(f"✅ 安全：ctypes 模块被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：ctypes 模块被阻止 - {type(e).__name__}: {e}")
        return True


def test_sys_module():
    """测试 sys 模块的危险功能拦截"""
    print("\n" + "=" * 60)
    print("测试用例 5: sys 模块危险功能拦截")
    print("=" * 60)
    
    try:
        import sys
        # 尝试退出程序（应该被阻止）
        sys.exit(0)
        print("❌ 安全漏洞：sys.exit() 可调用")
        return False
    except SecurityError as e:
        print(f"✅ 安全：sys.exit() 被正确拦截 - {e}")
        return True
    except SystemExit:
        print("❌ 安全漏洞：sys.exit() 可执行（程序已退出）")
        return False
    except Exception as e:
        print(f"✅ 安全：sys 危险功能被阻止 - {type(e).__name__}: {e}")
        return True


def test_platform_module():
    """测试 platform 模块导入拦截（信息泄露）"""
    print("\n" + "=" * 60)
    print("测试用例 6: platform 模块导入拦截")
    print("=" * 60)
    
    try:
        import platform
        # 如果导入成功，尝试获取系统信息
        info = platform.system()
        print(f"❌ 安全漏洞：platform 模块可导入，获取系统信息：{info}")
        return False
    except (ImportError, SecurityError) as e:
        print(f"✅ 安全：platform 模块被正确拦截 - {e}")
        return True
    except Exception as e:
        print(f"✅ 安全：platform 模块被阻止 - {type(e).__name__}: {e}")
        return True


def main():
    """运行所有模块导入限制测试"""
    print("\n" + "#" * 60)
    print("# 模块导入限制测试套件")
    print("# 测试目标：验证 os/subprocess/socket/ctypes 等危险模块被正确拦截")
    print("#" * 60)
    
    results = []
    
    results.append(("os 模块", test_os_module()))
    results.append(("subprocess 模块", test_subprocess_module()))
    results.append(("socket 模块", test_socket_module()))
    results.append(("ctypes 模块", test_ctypes_module()))
    results.append(("sys 危险功能", test_sys_module()))
    results.append(("platform 模块", test_platform_module()))
    
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
        print("\n🎉 所有模块导入限制测试通过！")
        sys.exit(0)
    else:
        print(f"\n⚠️  发现 {total - passed} 个安全漏洞！")
        sys.exit(1)


if __name__ == "__main__":
    main()
