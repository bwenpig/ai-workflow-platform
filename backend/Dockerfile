# ============================================
# Python 沙箱执行镜像 - 多阶段构建
# 用于安全执行用户提供的 Python 脚本
# ============================================

# ---------- 阶段 1: 构建依赖 ----------
FROM python:3.11-slim as builder

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统编译依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    make \
    cmake \
    pkg-config \
    libjpeg-dev \
    zlib1g-dev \
    libpng-dev \
    libtiff-dev \
    libopenjp2-7-dev \
    libpoppler-cpp-dev \
    && rm -rf /var/lib/apt/lists/*

# 创建虚拟环境
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# ---------- 阶段 2: 基础层 - 核心库 ----------
FROM python:3.11-slim as base-layer

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装基础运行时依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libjpeg62-turbo \
    zlib1g \
    libpng16-16 \
    libtiff6 \
    libopenjp2-7 \
    libpoppler-cpp0v5 \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制虚拟环境
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 安装基础层 Python 库（最常用）
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir \
    requests==2.31.0 \
    numpy==1.26.2 \
    pandas==2.1.4

# ---------- 阶段 3: 图像层 - 图像处理库 ----------
FROM base-layer as image-layer

# 安装图像处理库
RUN pip install --no-cache-dir \
    Pillow==10.1.0 \
    opencv-python-headless==4.8.1.78 \
    imageio==2.33.0

# ---------- 阶段 4: 数据层 - 数据可视化库 ----------
FROM image-layer as data-layer

# 安装数据可视化库
RUN pip install --no-cache-dir \
    matplotlib==3.8.2 \
    seaborn==0.13.0 \
    scipy==1.11.4

# ---------- 阶段 5: 工具层 - 常用工具库 ----------
FROM data-layer as tools-layer

# 安装工具库
RUN pip install --no-cache-dir \
    beautifulsoup4==4.12.2 \
    lxml==4.9.3 \
    python-dateutil==2.8.2 \
    PyYAML==6.0.1 \
    openpyxl==3.1.2 \
    python-docx==1.1.0 \
    fpdf2==2.7.7 \
    reportlab==4.0.7

# ---------- 阶段 6: 最终镜像 ----------
FROM tools-layer as final

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
