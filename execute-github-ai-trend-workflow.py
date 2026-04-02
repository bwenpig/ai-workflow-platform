#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub AI 技术趋势分析工作流执行脚本
模拟 4 个 Python 节点的串行执行
"""

import json
import requests
from collections import Counter
from datetime import datetime
import os

# ==================== 节点 1: GitHub 数据采集 ====================
def node1_github_data_collection():
    """节点 1: 获取 GitHub 热门 AI 项目"""
    print("=" * 60)
    print("🔍 节点 1: GitHub 数据采集")
    print("=" * 60)
    
    # GitHub Search API
    query = "language:Python topic:machine-learning topic:deep-learning topic:ai topic:llm"
    url = f"https://api.github.com/search/repositories?q={query}&sort=stars&order=desc&per_page=20"
    
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "AI-Trend-Analyzer/1.0"
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
                'description': item.get('description', '') or '',
                'language': item.get('language', ''),
                'topics': item.get('topics', []),
                'updated_at': item['updated_at'],
                'url': item['html_url']
            })
        
        result = {
            'status': 'success',
            'data': projects,
            'timestamp': datetime.now().isoformat(),
            'total_count': data.get('total_count', 0)
        }
        
        print(f"✅ 成功获取 {len(projects)} 个 AI 项目")
        print(f"📊 总项目数：{result['total_count']}")
        print(f"⏰ 时间戳：{result['timestamp']}")
        
        return result
    except Exception as e:
        print(f"❌ 错误：{str(e)}")
        return {
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now().isoformat(),
            'data': []
        }

# ==================== 节点 2: 数据清洗与处理 ====================
def node2_data_processing(github_data):
    """节点 2: 数据清洗与分析"""
    print("\n" + "=" * 60)
    print("📊 节点 2: 数据清洗与处理")
    print("=" * 60)
    
    projects = github_data.get('data', [])
    
    if not projects:
        print("❌ 没有项目数据")
        return {'status': 'error', 'message': 'No projects data'}
    
    # 分析技术栈
    languages = Counter([p['language'] for p in projects if p.get('language')])
    
    # 分析热门话题
    all_topics = []
    for p in projects:
        all_topics.extend(p.get('topics', []))
    topics = Counter(all_topics)
    
    # 分析项目活跃度
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
    
    activity_scores.sort(key=lambda x: x['score'], reverse=True)
    
    # 提取技术关键词
    keywords = []
    for p in projects:
        desc = p.get('description', '').lower()
        if 'transformer' in desc:
            keywords.append('transformer')
        if 'llm' in desc or 'large language model' in desc:
            keywords.append('llm')
        if 'diffusion' in desc:
            keywords.append('diffusion')
        if 'generation' in desc:
            keywords.append('generation')
        if 'rag' in desc:
            keywords.append('rag')
        if 'agent' in desc:
            keywords.append('agent')
    
    keyword_counts = Counter(keywords)
    
    result = {
        'status': 'success',
        'languages': dict(languages.most_common(5)),
        'topics': dict(topics.most_common(10)),
        'activity_ranking': activity_scores[:10],
        'keywords': dict(keyword_counts.most_common(5)),
        'total_projects': len(projects),
        'avg_stars': round(sum(p['stars'] for p in projects) / len(projects), 0)
    }
    
    print(f"✅ 分析完成")
    print(f"📊 语言分布：{result['languages']}")
    print(f"🔥 热门话题：{list(result['topics'].keys())[:5]}")
    print(f"💡 技术关键词：{result['keywords']}")
    
    return result

# ==================== 节点 3: AI 趋势分析 ====================
def node3_trend_analysis(analysis_data):
    """节点 3: 生成 AI 技术趋势洞察"""
    print("\n" + "=" * 60)
    print("📈 节点 3: AI 趋势分析")
    print("=" * 60)
    
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
    if 'rag' in keywords:
        trends.append("✅ RAG (检索增强生成) 成为企业应用热点")
    if 'agent' in keywords:
        trends.append("✅ AI Agent (智能体) 是新兴方向")
    
    if trends:
        insights.append("📈 **趋势预测**:\n" + "\n".join([f"  {t}" for t in trends]))
    
    result = {
        'status': 'success',
        'insights': insights,
        'generated_at': datetime.now().isoformat(),
        'data_points': analysis_data.get('total_projects', 0)
    }
    
    print(f"✅ 生成 {len(insights)} 条核心洞察")
    for insight in insights[:3]:
        print(f"  • {insight[:80]}...")
    
    return result

# ==================== 节点 4: 报告生成 ====================
def node4_report_generation(github_data, analysis_data, trend_data):
    """节点 4: 生成 Markdown 格式趋势报告"""
    print("\n" + "=" * 60)
    print("📄 节点 4: 报告生成")
    print("=" * 60)
    
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

*报告由 AI Workflow Platform (Python 节点) 自动生成*
"""
    
    # 保存报告
    report_path = '/Users/ben/.openclaw/workspace-coder/ai-workflow/github-ai-trend-report.md'
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    result = {
        'status': 'success',
        'report': report,
        'format': 'markdown',
        'file_path': report_path,
        'generated_at': datetime.now().isoformat()
    }
    
    print(f"✅ 报告已生成")
    print(f"📁 保存位置：{report_path}")
    print(f"📊 报告长度：{len(report)} 字符")
    
    return result

# ==================== 执行工作流 ====================
def main():
    """执行完整的 4 节点工作流"""
    print("\n" + "=" * 60)
    print("🚀 GitHub AI 技术趋势分析工作流")
    print("=" * 60)
    print(f"⏰ 开始时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # 节点 1: 数据采集
    github_data = node1_github_data_collection()
    if github_data.get('status') == 'error':
        print("❌ 节点 1 执行失败，终止工作流")
        return
    
    # 节点 2: 数据处理
    analysis_result = node2_data_processing(github_data)
    if analysis_result.get('status') == 'error':
        print("❌ 节点 2 执行失败，终止工作流")
        return
    
    # 节点 3: 趋势分析
    trend_insights = node3_trend_analysis(analysis_result)
    if trend_insights.get('status') == 'error':
        print("❌ 节点 3 执行失败，终止工作流")
        return
    
    # 节点 4: 报告生成
    final_report = node4_report_generation(github_data, analysis_result, trend_insights)
    if final_report.get('status') == 'error':
        print("❌ 节点 4 执行失败，终止工作流")
        return
    
    # 完成
    print("\n" + "=" * 60)
    print("✅ 工作流执行完成！")
    print("=" * 60)
    print(f"⏰ 结束时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"📁 报告位置：{final_report['file_path']}")
    print("\n📊 报告预览:")
    print("-" * 60)
    print(final_report['report'][:2000] + "...")

if __name__ == '__main__':
    main()
