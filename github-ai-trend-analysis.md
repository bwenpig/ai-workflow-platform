# GitHub AI 技术趋势分析工作流

## 工作流设计

### 目标
利用 Python 节点能力，通过多节点编排实现：
1. 获取 GitHub 热门 AI 项目
2. 分析 AI 技术趋势
3. 生成趋势报告

---

## 工作流节点编排

```
┌─────────────────┐
│  节点 1: GitHub  │
│  数据采集       │
│  (Python 脚本)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  节点 2: 数据   │
│  清洗与处理     │
│  (Python 脚本)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  节点 3: AI 趋势 │
│  分析           │
│  (Python 脚本)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  节点 4: 报告   │
│  生成           │
│  (Python 脚本)   │
└─────────────────┘
```

---

## 节点 1: GitHub 数据采集

### Python 代码
```python
import requests
import json
from datetime import datetime

# GitHub Trending API (通过 GitHub Search API)
def fetch_github_trending_ai():
    """获取 GitHub 热门 AI 项目"""
    
    # 搜索 AI 相关项目，按 stars 排序
    query = "language:Python topic:machine-learning topic:deep-learning topic:ai"
    url = f"https://api.github.com/search/repositories?q={query}&sort=stars&order=desc&per_page=20"
    
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "AI-Trend-Analyzer"
    }
    
    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        data = response.json()
        
        projects = []
        for item in data.get('items', [])[:10]:
            projects.append({
                'name': item['full_name'],
                'stars': item['stargazers_count'],
                'forks': item['forks_count'],
                'description': item.get('description', ''),
                'language': item.get('language', ''),
                'topics': item.get('topics', []),
                'updated_at': item['updated_at'],
                'url': item['html_url']
            })
        
        return {
            'status': 'success',
            'data': projects,
            'timestamp': datetime.now().isoformat(),
            'total_count': data.get('total_count', 0)
        }
    except Exception as e:
        return {
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now().isoformat()
        }

# 执行
result = fetch_github_trending_ai()
print(json.dumps(result, indent=2, ensure_ascii=False))

# 输出到下游节点
outputs['github_projects'] = result
```

### 依赖包
```
requests
```

---

## 节点 2: 数据清洗与处理

### Python 代码
```python
import json
from collections import Counter

def analyze_projects(data):
    """分析 GitHub 项目数据"""
    
    projects = data.get('github_projects', {}).get('data', [])
    
    if not projects:
        return {'status': 'error', 'message': 'No projects data'}
    
    # 分析技术栈
    languages = Counter([p['language'] for p in projects if p.get('language')])
    
    # 分析热门话题
    all_topics = []
    for p in projects:
        all_topics.extend(p.get('topics', []))
    topics = Counter(all_topics)
    
    # 分析项目活跃度（stars/forks 比率）
    activity_scores = []
    for p in projects:
        if p['forks'] > 0:
            score = p['stars'] / p['forks']
            activity_scores.append({
                'name': p['name'],
                'score': round(score, 2),
                'stars': p['stars'],
                'forks': p['forks']
            })
    
    # 按活跃度排序
    activity_scores.sort(key=lambda x: x['score'], reverse=True)
    
    # 提取项目关键词
    keywords = []
    for p in projects:
        desc = (p.get('description') or '').lower()
        if 'transformer' in desc:
            keywords.append('transformer')
        if 'llm' in desc or 'large language model' in desc:
            keywords.append('llm')
        if 'diffusion' in desc:
            keywords.append('diffusion')
        if 'generation' in desc:
            keywords.append('generation')
    
    keyword_counts = Counter(keywords)
    
    return {
        'status': 'success',
        'languages': dict(languages.most_common(5)),
        'topics': dict(topics.most_common(10)),
        'activity_ranking': activity_scores[:10],
        'keywords': dict(keyword_counts.most_common(5)),
        'total_projects': len(projects),
        'avg_stars': round(sum(p['stars'] for p in projects) / len(projects), 0)
    }

# 从上游获取数据
github_data = inputs.get('github_projects', {})

# 执行分析
result = analyze_projects({'github_projects': github_data})
print(json.dumps(result, indent=2, ensure_ascii=False))

# 输出到下游节点
outputs['analysis_result'] = result
```

### 依赖包
```
collections
json
```

---

## 节点 3: AI 趋势分析

### Python 代码
```python
import json
from datetime import datetime

def generate_trend_insights(analysis_data):
    """生成 AI 技术趋势洞察"""
    
    languages = analysis_data.get('languages', {})
    topics = analysis_data.get('topics', {})
    keywords = analysis_data.get('keywords', {})
    activity_ranking = analysis_data.get('activity_ranking', [])
    
    insights = []
    
    # 语言趋势
    if languages:
        top_lang = list(languages.keys())[0]
        top_lang_pct = round(languages[top_lang] / sum(languages.values()) * 100, 1)
        insights.append(f"📊 **主流语言**: {top_lang} 占比 {top_lang_pct}%，继续主导 AI 开发")
    
    # 技术热点
    hot_topics = []
    for topic, count in list(topics.items())[:5]:
        hot_topics.append(f"#{topic} ({count}次)")
    if hot_topics:
        insights.append(f"🔥 **热门话题**: {', '.join(hot_topics)}")
    
    # 技术关键词
    if keywords:
        tech_keywords = []
        for kw, count in keywords.items():
            tech_keywords.append(f"{kw} ({count}次)")
        insights.append(f"💡 **技术焦点**: {', '.join(tech_keywords)}")
    
    # 活跃项目分析
    if activity_ranking:
        top_project = activity_ranking[0]
        insights.append(f"⭐ **最活跃项目**: {top_project['name']} (活跃度评分：{top_project['score']})")
    
    # 趋势预测
    trends = []
    if 'llm' in keywords or 'transformer' in keywords:
        trends.append("✅ 大语言模型 (LLM) 持续火热")
    if 'diffusion' in keywords:
        trends.append("✅ 扩散模型在图像生成领域广泛应用")
    if 'generation' in keywords:
        trends.append("✅ AIGC (AI 生成内容) 是重要方向")
    
    if trends:
        insights.append("📈 **趋势预测**:\n" + "\n".join([f"  {t}" for t in trends]))
    
    return {
        'status': 'success',
        'insights': insights,
        'generated_at': datetime.now().isoformat(),
        'data_points': analysis_data.get('total_projects', 0)
    }

# 从上游获取数据
analysis_result = inputs.get('analysis_result', {})

# 生成洞察
result = generate_trend_insights(analysis_result)
print(json.dumps(result, indent=2, ensure_ascii=False))

# 输出到下游节点
outputs['trend_insights'] = result
```

### 依赖包
```
json
datetime
```

---

## 节点 4: 报告生成

### Python 代码
```python
import json
from datetime import datetime

def generate_markdown_report(github_data, analysis_data, trend_data):
    """生成 Markdown 格式趋势报告"""
    
    projects = github_data.get('data', [])
    insights = trend_data.get('insights', [])
    
    report = f"""# 🚀 GitHub AI 技术趋势分析报告

**生成时间:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  
**数据源:** GitHub Search API  
**分析项目数:** {len(projects)}

---

## 📊 核心洞察

"""
    
    for insight in insights:
        report += f"{insight}\n\n"
    
    report += """---

## 🔝 Top 10 热门 AI 项目

| 排名 | 项目名称 | Stars | Forks | 活跃度 |
|------|----------|-------|-------|--------|
"""
    
    for i, project in enumerate(projects[:10], 1):
        forks = project.get('forks', 0)
        stars = project.get('stars', 0)
        activity = round(stars / max(forks, 1), 2)
        report += f"| {i} | [{project['name']}]({project['url']}) | {stars:,} | {forks:,} | {activity}x |\n"
    
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
    
    report += f"""
---

## 💡 建议与行动

1. **关注方向**: 根据趋势，建议重点关注 LLM、AIGC 相关技术
2. **学习资源**: 参考 Top 项目的实现方案
3. **技术选型**: 优先选择活跃度高的项目作为技术依赖

---

*报告由 AI Workflow Platform 自动生成*
"""
    
    return {
        'status': 'success',
        'report': report,
        'format': 'markdown',
        'generated_at': datetime.now().isoformat()
    }

# 从上游获取数据
github_data = inputs.get('github_projects', {})
analysis_result = inputs.get('analysis_result', {})
trend_insights = inputs.get('trend_insights', {})

# 生成报告
result = generate_markdown_report(github_data, analysis_result, trend_insights)
print(result['report'])

# 输出最终结果
outputs['final_report'] = result
```

### 依赖包
```
json
datetime
```

---

## 工作流配置

### 执行顺序
1. 节点 1 → 节点 2 → 节点 3 → 节点 4（串行执行）

### 数据传递
- 节点 1 输出 → `outputs['github_projects']`
- 节点 2 输入 → `inputs['github_projects']`，输出 → `outputs['analysis_result']`
- 节点 3 输入 → `inputs['analysis_result']`，输出 → `outputs['trend_insights']`
- 节点 4 输入 → `inputs['github_projects']`, `inputs['analysis_result']`, `inputs['trend_insights']`

### 超时配置
- 每个节点：60 秒
- 总超时：300 秒

---

## 预期输出

```markdown
# 🚀 GitHub AI 技术趋势分析报告

**生成时间:** 2026-04-01 18:00:00
**数据源:** GitHub Search API
**分析项目数:** 10

---

## 📊 核心洞察

📊 **主流语言**: Python 占比 85.5%，继续主导 AI 开发
🔥 **热门话题**: #machine-learning (15 次), #deep-learning (12 次), #llm (10 次)
💡 **技术焦点**: transformer (8 次), llm (6 次), generation (5 次)
⭐ **最活跃项目**: huggingface/transformers (活跃度评分：15.8)
📈 **趋势预测**:
  ✅ 大语言模型 (LLM) 持续火热
  ✅ AIGC (AI 生成内容) 是重要方向

---

## 🔝 Top 10 热门 AI 项目
...
```

---

## 下一步

1. 在 AI Workflow Platform 中创建此工作流
2. 配置 4 个 Python 节点
3. 连接节点间的数据流
4. 执行工作流并验证输出
5. 导出报告为 PDF/Markdown
