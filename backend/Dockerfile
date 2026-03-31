# Python 沙箱执行镜像
# 用于安全执行用户提供的 Python 脚本

FROM python:3.11-slim

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libc-dev \
    && rm -rf /var/lib/apt/lists/*

# 预装常用 Python 库
RUN pip install --no-cache-dir \
    requests==2.31.0 \
    pandas==2.1.4 \
    numpy==1.26.2 \
    Pillow==10.1.0 \
    beautifulsoup4==4.12.2 \
    lxml==4.9.3 \
    python-dateutil==2.8.2 \
    mutagen==1.47.0 \
    ffmpeg-python==0.2.0

# 创建非 root 用户
RUN useradd -m -u 1000 sandbox && \
    mkdir /sandbox && chown sandbox:sandbox /sandbox && \
    mkdir /tmp/sandbox && chmod 1777 /tmp/sandbox

# 切换到非 root 用户
USER sandbox
WORKDIR /sandbox

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python3 -c "print('OK')" || exit 1

# 默认命令
CMD ["python3"]
