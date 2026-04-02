#!/usr/bin/env python3
"""
代码模板测试脚本
测试目标：验证代码模板的加载与执行

测试用例：
1. 数据转换模板（JSON ↔ 文本）
2. API 调用模板（HTTP Request 封装）
3. 数据处理模板（图片/视频信息提取）
4. 条件分支模板
5. 自定义模板执行
"""

import sys
import json
import time
import base64
from io import BytesIO

# ==================== 模板 1: JSON 转文本 ====================
def template_json_to_text(inputs):
    """JSON 转文本模板"""
    print("[TEMPLATE 1] JSON 转文本模板...")
    
    data = inputs.get('json_data', {})
    format_type = inputs.get('format', 'summary')
    
    if format_type == 'summary':
        # 生成摘要
        text = f"数据包含 {len(data)} 个字段\n"
        for key, value in data.items():
            text += f"  - {key}: {type(value).__name__}\n"
    elif format_type == 'full':
        # 完整输出
        text = json.dumps(data, indent=2, ensure_ascii=False)
    else:
        text = str(data)
    
    return {
        "status": "success",
        "template": "json_to_text",
        "output_text": text
    }

# ==================== 模板 2: 文本转 JSON ====================
def template_text_to_json(inputs):
    """文本转 JSON 模板"""
    print("[TEMPLATE 2] 文本转 JSON 模板...")
    
    text = inputs.get('text', '')
    parse_type = inputs.get('parse_type', 'auto')
    
    try:
        if parse_type == 'json':
            data = json.loads(text)
        elif parse_type == 'auto':
            # 尝试 JSON 解析
            try:
                data = json.loads(text)
            except:
                # 回退到键值对解析
                data = {}
                for line in text.strip().split('\n'):
                    if ':' in line:
                        key, value = line.split(':', 1)
                        data[key.strip()] = value.strip()
        else:
            data = {"raw": text}
        
        return {
            "status": "success",
            "template": "text_to_json",
            "output_json": data
        }
    except Exception as e:
        return {
            "status": "failed",
            "template": "text_to_json",
            "error": str(e)
        }

# ==================== 模板 3: 数据聚合 ====================
def template_data_aggregation(inputs):
    """数据聚合模板"""
    print("[TEMPLATE 3] 数据聚合模板...")
    
    items = inputs.get('items', [])
    aggregation_type = inputs.get('aggregation_type', 'merge')
    
    if aggregation_type == 'merge':
        # 合并多个字典
        result = {}
        for item in items:
            if isinstance(item, dict):
                result.update(item)
    elif aggregation_type == 'collect':
        # 收集所有值到列表
        result = {"collected": items}
    elif aggregation_type == 'count':
        # 统计数量
        result = {"count": len(items), "types": {}}
        for item in items:
            t = type(item).__name__
            result["types"][t] = result["types"].get(t, 0) + 1
    
    return {
        "status": "success",
        "template": "data_aggregation",
        "output": result
    }

# ==================== 模板 4: 条件分支 ====================
def template_conditional_branch(inputs):
    """条件分支模板"""
    print("[TEMPLATE 4] 条件分支模板...")
    
    value = inputs.get('value')
    condition = inputs.get('condition', 'truthy')
    
    branch = "default"
    
    if condition == 'truthy':
        branch = "true" if value else "false"
    elif condition == 'positive':
        branch = "positive" if (isinstance(value, (int, float)) and value > 0) else "non_positive"
    elif condition == 'non_empty':
        branch = "non_empty" if value else "empty"
    elif condition == 'type_check':
        expected_type = inputs.get('expected_type', 'str')
        if expected_type == 'str' and isinstance(value, str):
            branch = "match"
        elif expected_type == 'int' and isinstance(value, int):
            branch = "match"
        elif expected_type == 'dict' and isinstance(value, dict):
            branch = "match"
        else:
            branch = "mismatch"
    
    return {
        "status": "success",
        "template": "conditional_branch",
        "branch": branch,
        "output": {"selected_branch": branch}
    }

# ==================== 模板 5: 图像处理 ====================
def template_image_processing(inputs):
    """图像处理模板（需要 Pillow）"""
    print("[TEMPLATE 5] 图像处理模板...")
    
    try:
        from PIL import Image
        
        image_data = inputs.get('image_data')  # base64 编码
        operation = inputs.get('operation', 'info')
        
        if not image_data:
            # 创建测试图像
            img = Image.new('RGB', (200, 200), color='blue')
            print("  创建测试图像 200x200")
        else:
            # 解码图像
            img_bytes = base64.b64decode(image_data)
            img = Image.open(BytesIO(img_bytes))
            print(f"  加载图像：{img.size}")
        
        if operation == 'info':
            result = {
                "size": img.size,
                "mode": img.mode,
                "format": img.format or "PIL"
            }
        elif operation == 'resize':
            new_size = inputs.get('new_size', (100, 100))
            img_resized = img.resize(new_size)
            result = {"original_size": img.size, "new_size": new_size}
        elif operation == 'rotate':
            angle = inputs.get('angle', 90)
            img_rotated = img.rotate(angle)
            result = {"original_size": img.size, "rotation": angle}
        else:
            result = {"operation": operation, "status": "unknown"}
        
        return {
            "status": "success",
            "template": "image_processing",
            "output": result
        }
    except ImportError:
        return {
            "status": "failed",
            "template": "image_processing",
            "error": "Pillow not installed"
        }
    except Exception as e:
        return {
            "status": "failed",
            "template": "image_processing",
            "error": str(e)
        }

# ==================== 模板 6: 数据过滤 ====================
def template_data_filter(inputs):
    """数据过滤模板"""
    print("[TEMPLATE 6] 数据过滤模板...")
    
    items = inputs.get('items', [])
    filter_type = inputs.get('filter_type', 'none')
    filter_value = inputs.get('filter_value')
    
    filtered = items
    
    if filter_type == 'contains':
        filtered = [item for item in items if filter_value in str(item)]
    elif filter_type == 'greater_than':
        filtered = [item for item in items if isinstance(item, (int, float)) and item > filter_value]
    elif filter_type == 'less_than':
        filtered = [item for item in items if isinstance(item, (int, float)) and item < filter_value]
    elif filter_type == 'type':
        filtered = [item for item in items if type(item).__name__ == filter_value]
    
    return {
        "status": "success",
        "template": "data_filter",
        "output": {
            "original_count": len(items),
            "filtered_count": len(filtered),
            "filtered_items": filtered[:10]  # 限制输出大小
        }
    }

# ==================== 主执行函数 ====================
def run_template(template_name, inputs):
    """运行指定模板"""
    templates = {
        "json_to_text": template_json_to_text,
        "text_to_json": template_text_to_json,
        "data_aggregation": template_data_aggregation,
        "conditional_branch": template_conditional_branch,
        "image_processing": template_image_processing,
        "data_filter": template_data_filter
    }
    
    if template_name not in templates:
        return {
            "status": "failed",
            "error": f"未知模板：{template_name}",
            "available_templates": list(templates.keys())
        }
    
    return templates[template_name](inputs)

if __name__ == "__main__":
    # 从命令行或 stdin 获取输入
    if len(sys.argv) > 1:
        template_name = sys.argv[1]
    else:
        template_name = "all"
    
    # 读取输入（从 JSON 文件或 stdin）
    inputs_file = sys.argv[2] if len(sys.argv) > 2 else None
    
    if inputs_file:
        with open(inputs_file, 'r') as f:
            inputs = json.load(f)
    else:
        # 默认测试输入
        inputs = {
            "json_data": {"name": "test", "value": 123},
            "text": '{"key": "value"}',
            "items": [1, 2, 3, 4, 5],
            "value": True,
            "format": "summary"
        }
    
    results = {
        "timestamp": time.time(),
        "templates_tested": []
    }
    
    if template_name == "all":
        # 测试所有模板
        for name in ["json_to_text", "text_to_json", "data_aggregation", 
                     "conditional_branch", "data_filter"]:
            print(f"\n{'='*50}")
            result = run_template(name, inputs)
            results["templates_tested"].append({
                "name": name,
                "result": result
            })
    else:
        result = run_template(template_name, inputs)
        results["templates_tested"].append({
            "name": template_name,
            "result": result
        })
    
    # 统计
    total = len(results["templates_tested"])
    passed = sum(1 for t in results["templates_tested"] if t["result"].get("status") == "success")
    
    results["summary"] = {
        "total": total,
        "passed": passed,
        "failed": total - passed
    }
    
    print(f"\n{'='*50}")
    print(json.dumps(results, indent=2, ensure_ascii=False))
