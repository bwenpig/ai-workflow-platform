package com.ben.workflow.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Python 脚本执行的通用工具方法
 * <p>
 * 提取 PythonScriptExecutor 和 PythonDockerExecutor 的共享逻辑：
 * - wrapScript: 为用户脚本注入 I/O 框架
 * - cleanupTempDir: 安全清理临时目录
 * 
 * @author 龙傲天 (refactoring)
 */
public final class PythonScriptUtils {

    private PythonScriptUtils() {}

    /**
     * 包装用户脚本，注入 inputs/outputs 的 JSON I/O 框架。
     * <p>
     * 用户代码运行在 try 块内，可直接访问 inputs (dict) 和 outputs (dict)。
     * 
     * @param userScript 用户编写的 Python 代码
     * @return 完整的可执行脚本
     */
    public static String wrapScript(String userScript) {
        return """
import json
import sys
import os

# 读取输入
inputs_file = sys.argv[1] if len(sys.argv) > 1 else 'inputs.json'
try:
    with open(inputs_file, 'r', encoding='utf-8') as f:
        inputs = json.load(f)
except Exception as e:
    inputs = {}
    print(f"ERROR loading inputs: {e}", file=sys.stderr)

# 执行用户脚本
outputs = {}
try:
%s

except Exception as e:
    import traceback
    error_info = {'error': str(e), 'traceback': traceback.format_exc()}
    with open('outputs.json', 'w', encoding='utf-8') as f:
        json.dump({'_error': error_info}, f, ensure_ascii=False, indent=2)
    sys.exit(1)

# 写入输出
with open('outputs.json', 'w', encoding='utf-8') as f:
    json.dump(outputs, f, ensure_ascii=False, indent=2)
sys.exit(0)
""".formatted(userScript.indent(4));
    }

    /**
     * 安全清理临时目录（递归删除所有文件和子目录）
     * 
     * @param tempDir 要清理的临时目录
     */
    public static void cleanupTempDir(Path tempDir) {
        if (tempDir == null) return;
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.out.println("[PythonScriptUtils] 清理文件失败：" + path);
                        }
                    });
        } catch (IOException e) {
            System.err.println("[PythonScriptUtils] 清理临时目录失败：" + e.getMessage());
        }
    }
}
