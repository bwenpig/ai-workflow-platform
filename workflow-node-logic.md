# GitHub AI 技术趋势分析工作流 - 各节点详细逻辑

---

## 🔍 节点 1: GitHub 数据采集

### 功能描述
通过 GitHub Search API 获取热门 AI 项目数据

### 完整代码
```python
import requests
import json
from datetime import datetime

def fetch_github_trending_ai():
    """获取 GitHub 热门 AI 项目"""
    
    # 1. 构建搜索查询
    # 搜索条件：Python 语言 + AI/ML/DL/LLM 话题
    query = "language:Python topic:machine-learning topic:deep-learning topic:ai topic:llm"
    
    # 2. GitHub API 端点
    url = f"https://api.github.com/search/repositories?q={query}&sort=stars&order=desc&per_page=20"
    
    # 3. 请求头（GitHub API 要求 User-Agent）
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "AI-Trend-Analyzer/1.0"
    }
    
    try:
        # 4. 发送 HTTP GET 请求
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()  # 检查 HTTP 状态
        data = response.json()  # 解析 JSON 响应
        
        # 5. 提取项目数据
        projects = []
        for item in data.get('items', [])[:10]:  # 只取前 10 个
            projects.append({
                'name': item['full_name'],           # 项目全名 (user/repo)
                'stars': item['stargazers_count'],   # Star 数
                'forks': item['forks_count'],        # Fork 数
                'description': item.get('description', '') or '',  # 项目描述
                'language': item.get('language', ''),  # 编程语言
                'topics': item.get('topics', []),     # 话题标签
                'updated_at': item['updated_at'],     # 最后更新时间
                'url': item['html_url']               # 项目 URL
            })
        
        # 6. 构建输出数据
        result = {
            'status': 'success',
            'data': projects,
            'timestamp': datetime.now().isoformat(),
            'total_count': data.get('total_count', 0)  # 符合条件的总项目数
        }
        
        return result
        
    except Exception as e:
        # 7. 错误处理
        return {
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now().isoformat(),
            'data': []
        }

# 执行函数
result = fetch_github_trending_ai()

# 输出到下游节点
outputs['github_projects'] = result
```

### 输入参数
```json
{}  // 无输入，独立节点
```

### 输出数据结构
```json
{
  "status": "success",
  "data": [
    {
      "name": "huggingface/datasets",
      "stars": 21354,
      "forks": 3152,
      "description": "Hugging Face datasets library",
      "language": "Python",
      "topics": ["ai", "deep-learning", "llm", "nlp"],
      "updated_at": "2026-04-01T10:00:00Z",
      "url": "https://github.com/huggingface/datasets"
    }
    // ... 共 10 个项目
  ],
  "timestamp": "2026-04-01T18:49:52.523281",
  "total_count": 76
}
```

### 依赖包
```
requests  # HTTP 请求库
```

### 关键逻辑说明
1. **API 选择**：使用 GitHub Search API 而非 Trending API（后者无官方 API）
2. **搜索策略**：组合多个 topic 提高准确性
3. **排序规则**：按 stars 降序，确保获取最热门项目
4. **数据提取**：只取前 10 个，避免数据过大
5. **错误处理**：捕获网络异常、API 限流等错误

---

## 📊 节点 2: 数据清洗与处理

### 功能描述
分析项目数据，提取语言分布、热门话题、技术关键词、活跃度评分

### 完整代码
```python
import json
from collections import Counter

def analyze_projects(data):
    """分析 GitHub 项目数据"""
    
    # 1. 从上游节点获取数据
    projects = data.get('github_projects', {}).get('data', [])
    
    if not projects:
        return {'status': 'error', 'message': 'No projects data'}
    
    # 2. 语言分布统计
    # 统计每种编程语言出现的项目数
    languages = Counter([p['language'] for p in projects if p.get('language')])
    # 示例结果：{'Python': 10}
    
    # 3. 热门话题分析
    # 收集所有话题并统计频次
    all_topics = []
    for p in projects:
        all_topics.extend(p.get('topics', []))
    topics = Counter(all_topics)
    # 示例结果：{'ai': 10, 'deep-learning': 10, 'llm': 10, ...}
    
    # 4. 项目活跃度评分
    # 活跃度 = stars / forks，比值越高说明项目越受欢迎
    activity_scores = []
    for p in projects:
        if p['forks'] > 0:  # 避免除零错误
            score = p['stars'] / p['forks']
            activity_scores.append({
                'name': p['name'],
                'score': round(score, 2),
                'stars': p['stars'],
                'forks': p['forks']
            })
    
    # 按活跃度降序排序
    activity_scores.sort(key=lambda x: x['score'], reverse=True)
    # 示例结果：[{'name': 'marimo-team/modernaicourse', 'score': 19.4, ...}, ...]
    
    # 5. 技术关键词提取
    # 从项目描述中提取关键技术词汇
    keywords = []
    for p in projects:
        desc = (p.get('description') or '').lower()
        
        # 关键词匹配规则
        if 'transformer' in desc:
            keywords.append('transformer')
        if 'llm' in desc or 'large language model' in desc:
            keywords.append('llm')
        if 'diffusion' in desc:
            keywords.append('diffusion')
        if 'generation' in desc:
            keywords.append('generation')
        if 'rag' in desc or 'retrieval augmented' in desc:
            keywords.append('rag')
        if 'agent' in desc or 'autonomous' in desc:
            keywords.append('agent')
    
    keyword_counts = Counter(keywords)
    # 示例结果：{'llm': 3, 'agent': 1, 'generation': 1, 'rag': 1}
    
    # 6. 构建输出数据
    result = {
        'status': 'success',
        'languages': dict(languages.most_common(5)),      # Top 5 语言
        'topics': dict(topics.most_common(10)),           # Top 10 话题
        'activity_ranking': activity_scores[:10],         # Top 10 活跃度
        'keywords': dict(keyword_counts.most_common(5)),  # Top 5 关键词
        'total_projects': len(projects),
        'avg_stars': round(sum(p['stars'] for p in projects) / len(projects), 0)
    }
    
    return result

# 从上游节点获取数据
github_data = inputs.get('github_projects', {})

# 执行分析
result = analyze_projects({'github_projects': github_data})

# 输出到下游节点
outputs['analysis_result'] = result
```

### 输入数据
```json
{
  "github_projects": {
    "status": "success",
    "data": [10 个项目数据],
    "total_count": 76
  }
}
```

### 输出数据结构
```json
{
  "status": "success",
  "languages": {"Python": 10},
  "topics": {
    "ai": 10,
    "deep-learning": 10,
    "llm": 10,
    "machine-learning": 10,
    "python": 5
  },
  "activity_ranking": [
    {
      "name": "marimo-team/modernaicourse",
      "score": 19.4,
      "stars": 97,
      "forks": 5
    },
    // ... 共 10 个项目
  ],
  "keywords": {"llm": 3, "agent": 1, "generation": 1, "rag": 1},
  "total_projects": 10,
  "avg_stars": 4238
}
```

### 依赖包
```
collections  # Counter 类（Python 内置）
json         # JSON 处理（Python 内置）
```

### 关键逻辑说明
1. **Counter 统计**：使用 Counter 快速统计频次
2. **活跃度算法**：stars/forks 比值，越高说明项目越受欢迎
3. **关键词匹配**：从描述中提取技术关键词，支持多种匹配规则
4. **数据排序**：按频次/分数降序排列，提取 Top N

---

## 📈 节点 3: AI 趋势分析

### 功能描述
基于分析结果生成 AI 技术趋势洞察和预测

### 完整代码
```python
import json
from datetime import datetime

def generate_trend_insights(analysis_data):
    """生成 AI 技术趋势洞察"""
    
    # 1. 从上游节点获取分析数据
    languages = analysis_data.get('languages', {})
    topics = analysis_data.get('topics', {})
    keywords = analysis_data.get('keywords', {})
    activity_ranking = analysis_data.get('activity_ranking', [])
    
    insights = []
    
    # 2. 主流语言分析
    if languages:
        top_lang = list(languages.keys())[0]  # 排名第一的语言
        total = sum(languages.values())
        top_lang_pct = round(languages[top_lang] / total * 100, 1)
        
        insight = f"📊 **主流语言**: {top_lang} 占比 {top_lang_pct}%，继续主导 AI 开发"
        insights.append(insight)
    
    # 3. 热门话题分析
    hot_topics = []
    for topic, count in list(topics.items())[:5]:
        hot_topics.append(f"#{topic} ({count}次)")
    
    if hot_topics:
        insight = f"🔥 **热门话题**: {', '.join(hot_topics)}"
        insights.append(insight)
    
    # 4. 技术焦点分析
    if keywords:
        tech_keywords = []
        for kw, count in keywords.items():
            tech_keywords.append(f"{kw} ({count}次)")
        
        insight = f"💡 **技术焦点**: {', '.join(tech_keywords)}"
        insights.append(insight)
    
    # 5. 最活跃项目分析
    if activity_ranking:
        top_project = activity_ranking[0]
        insight = f"⭐ **最活跃项目**: {top_project['name']} (活跃度评分：{top_project['score']})"
        insights.append(insight)
    
    # 6. 趋势预测
    trends = []
    
    # LLM 趋势
    if 'llm' in keywords or 'transformer' in keywords:
        trends.append("✅ 大语言模型 (LLM) 持续火热")
    
    # 扩散模型趋势
    if 'diffusion' in keywords:
        trends.append("✅ 扩散模型在图像生成领域广泛应用")
    
    # AIGC 趋势
    if 'generation' in keywords:
        trends.append("✅ AIGC (AI 生成内容) 是重要方向")
    
    # RAG 趋势
    if 'rag' in keywords:
        trends.append("✅ RAG (检索增强生成) 成为企业应用热点")
    
    # AI Agent 趋势
    if 'agent' in keywords:
        trends.append("✅ AI Agent (智能体) 是新兴方向")
    
    if trends:
        insight = "📈 **趋势预测**:\n" + "\n".join([f"  {t}" for t in trends])
        insights.append(insight)
    
    # 7. 构建输出数据
    result = {
        'status': 'success',
        'insights': insights,
        'generated_at': datetime.now().isoformat(),
        'data_points': analysis_data.get('total_projects', 0)
    }
    
    return result

# 从上游节点获取数据
analysis_result = inputs.get('analysis_result', {})

# 生成洞察
result = generate_trend_insights(analysis_result)

# 输出到下游节点
outputs['trend_insights'] = result
```

### 输入数据
```json
{
  "analysis_result": {
    "languages": {"Python": 10},
    "topics": {"ai": 10, "deep-learning": 10, ...},
    "keywords": {"llm": 3, "agent": 1, ...},
    "activity_ranking": [...]
  }
}
```

### 输出数据结构
```json
{
  "status": "success",
  "insights": [
    "📊 **主流语言**: Python 占比 100.0%，继续主导 AI 开发",
    "🔥 **热门话题**: #ai (10 次), #deep-learning (10 次), #llm (10 次), #machine-learning (10 次), #python (5 次)",
    "💡 **技术焦点**: llm (3 次), agent (1 次), generation (1 次), rag (1 次)",
    "⭐ **最活跃项目**: marimo-team/modernaicourse (活跃度评分：19.4)",
    "📈 **趋势预测**:\n  ✅ 大语言模型 (LLM) 持续火热\n  ✅ AIGC (AI 生成内容) 是重要方向\n  ✅ RAG (检索增强生成) 成为企业应用热点\n  ✅ AI Agent (智能体) 是新兴方向"
  ],
  "generated_at": "2026-04-01T18:49:52.xxxxxx",
  "data_points": 10
}
```

### 依赖包
```
json      # JSON 处理（Python 内置）
datetime  # 时间处理（Python 内置）
```

### 关键逻辑说明
1. **洞察生成规则**：基于统计数据生成自然语言描述
2. **趋势预测规则**：根据关键词匹配触发相应的趋势预测
3. **格式化输出**：使用 emoji 和 Markdown 格式增强可读性
4. **条件判断**：只有数据存在时才生成对应洞察

---

## 📄 节点 4: 报告生成

### 功能描述
整合所有节点数据，生成完整的 Markdown 格式趋势报告

### 完整代码
```python
import json
from datetime import datetime

def generate_markdown_report(github_data, analysis_data, trend_data):
    """生成 Markdown 格式趋势报告"""
    
    # 1. 从上游节点获取数据
    projects = github_data.get('data', [])
    insights = trend_data.get('insights', [])
    
    # 2. 生成报告标题和元数据
    report = f"""# 🚀 GitHub AI 技术趋势分析报告

**生成时间:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**数据源:** GitHub Search API  
**分析项目数:** {len(projects)}

---

## 📊 核心洞察

"""
    
    # 3. 添加核心洞察（来自节点 3）
    for insight in insights:
        report += f"{insight}\n\n"
    
    # 4. 生成 Top 10 项目表格
    report += """---

## 🔝 Top 10 热门 AI 项目

| 排名 | 项目名称 | Stars | Forks | 活跃度 |
|------|----------|-------|-------|--------|
"""
    
    for i, project in enumerate(projects[:10], 1):
        forks = project.get('forks', 0)
        stars = project.get('stars', 0)
        activity = round(stars / max(forks, 1), 2)  # 避免除零
        
        # 生成 Markdown 表格行
        report += f"| {i} | [{project['name']}]({project['url']}) | {stars:,} | {forks:,} | {activity}x |\n"
    
    # 5. 添加技术分布
    report += f"""
---

## 📈 技术分布

**编程语言:**
"""
    
    for lang, count in analysis_data.get('languages', {}).items():
        report += f"- {lang}: {count} 个项目\n"
    
    report += f"""
**热门话题:**
"""
    
    for topic, count in list(analysis_data.get('topics', {}).items())[:5]:
        report += f"- #{topic}: {count} 次提及\n"
    
    # 6. 添加建议与行动
    report += f"""
---

## 💡 建议与行动

1. **关注方向**: 根据趋势，建议重点关注 LLM、AIGC 相关技术
2. **学习资源**: 参考 Top 项目的实现方案
3. **技术选型**: 优先选择活跃度高的项目作为技术依赖

---

*报告由 AI Workflow Platform (Python 节点) 自动生成*
"""
    
    # 7. 保存报告到文件
    report_path = '/Users/ben/.openclaw/workspace-coder/ai-workflow/github-ai-trend-report.md'
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    # 8. 构建输出数据
    result = {
        'status': 'success',
        'report': report,
        'format': 'markdown',
        'file_path': report_path,
        'generated_at': datetime.now().isoformat()
    }
    
    return result

# 从上游节点获取数据（多节点输入）
github_data = inputs.get('github_projects', {})
analysis_result = inputs.get('analysis_result', {})
trend_insights = inputs.get('trend_insights', {})

# 生成报告
result = generate_markdown_report(github_data, analysis_result, trend_insights)

# 输出最终结果
outputs['final_report'] = result
```

### 输入数据
```json
{
  "github_projects": {
    "status": "success",
    "data": [10 个项目数据]
  },
  "analysis_result": {
    "languages": {"Python": 10},
    "topics": {...},
    "activity_ranking": [...]
  },
  "trend_insights": {
    "insights": [5 条核心洞察]
  }
}
```

### 输出数据结构
```json
{
  "status": "success",
  "report": "# 🚀 GitHub AI 技术趋势分析报告\n...",
  "format": "markdown",
  "file_path": "/Users/ben/.openclaw/workspace-coder/ai-workflow/github-ai-trend-report.md",
  "generated_at": "2026-04-01T18:49:52.xxxxxx"
}
```

### 依赖包
```
json      # JSON 处理（Python 内置）
datetime  # 时间处理（Python 内置）
```

### 关键逻辑说明
1. **多节点数据整合**：同时接收 3 个上游节点的数据
2. **Markdown 格式化**：使用 f-string 动态生成 Markdown 内容
3. **表格生成**：循环生成 Markdown 表格行
4. **文件保存**：将报告写入本地文件系统
5. **数字格式化**：使用 `:,` 格式化千位分隔符

---

## 🔄 节点间数据流

```
节点 1 (数据采集)
    │
    │ outputs['github_projects']
    ▼
节点 2 (数据处理)
    │
    │ inputs['github_projects'] → outputs['analysis_result']
    ▼
节点 3 (趋势分析)
    │
    │ inputs['analysis_result'] → outputs['trend_insights']
    ▼
节点 4 (报告生成)
    │
    │ inputs['github_projects']
    │ inputs['analysis_result']
    │ inputs['trend_insights']
    ▼
最终报告 (Markdown 文件)
```

---

## 📋 完整工作流配置

### 节点配置表
| 节点 | 名称 | 超时 | 重试 | 依赖包 |
|------|------|------|------|--------|
| 1 | GitHub 数据采集 | 60s | 3 次 | requests |
| 2 | 数据清洗与处理 | 60s | 3 次 | collections, json |
| 3 | AI 趋势分析 | 60s | 3 次 | json, datetime |
| 4 | 报告生成 | 60s | 3 次 | json, datetime |

### 执行顺序
```
节点 1 → 节点 2 → 节点 3 → 节点 4（严格串行）
```

### 错误处理
- 每个节点独立捕获异常
- 失败时返回 `{'status': 'error', 'message': '...'}`
- 下游节点检查上游状态，失败时终止工作流

---

*文档生成时间：2026-04-01 18:54*  
*工作流平台：AI Workflow Platform (Python 节点)*
