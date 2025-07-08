
## 📁 项目结构
```
code/
├── videoToText/          # 视频字幕生成系统
├── vue2-demo/           # 前端Vue.js应用
└── README.md           # 项目说明文档
```

---

# 🎥 视频字幕生成系统

基于Spring Boot + Vue.js的视频字幕生成系统，能够自动从视频中提取音频并生成字幕文件。

## ✨ 功能特性

- 🎥 支持多种视频格式上传
- 🔊 自动音频提取
- 🗣️ 基于Whisper的语音识别
- 📝 自动生成VTT字幕文件
- 📊 实时进度显示
- 🌐 WebSocket实时通信
- 🎨 现代化UI界面

## 🏗️ 系统架构

### 后端 (Spring Boot)
- **控制器**: `ziMuController` - 处理视频上传请求
- **工具类**: `VideoToWavUtil` - 视频处理核心逻辑
- **WebSocket**: `ProgressWebSocketHandler` - 实时进度推送
- **异步处理**: 使用线程池处理视频，避免阻塞

### 前端 (Vue.js)
- **组件**: `UploadVideo.vue` - 视频上传和进度显示
- **WebSocket**: 实时接收后端进度信息
- **智能进度解析**: 根据消息内容动态更新进度条

## 🛠️ 技术栈

### 后端
- Spring Boot 2.3.12
- JavaCV (FFmpeg)
- Whisper (Python脚本)
- WebSocket
- Maven

### 前端
- Vue.js 2.x
- Axios (HTTP请求)
- WebSocket (实时通信)

## 🚀 快速开始

### 环境要求
- Java 8+
- Node.js 12+
- Python 3.7+ (用于Whisper)
- FFmpeg

### 启动步骤

1. **启动后端服务**
```bash
cd videoToText
mvn spring-boot:run
```

2. **启动前端应用**
```bash
cd vue2-demo
npm install
npm run serve
```

3. **访问系统**
打开浏览器访问: `http://localhost:8080`

## 📋 使用说明

1. **上传视频**: 点击"选择视频文件"按钮，选择要处理的视频文件
2. **开始处理**: 点击"开始处理"按钮，系统将开始处理视频
3. **查看进度**: 实时查看处理进度和详细日志
4. **获取结果**: 处理完成后，字幕文件将保存在指定目录

## ⚙️ 配置说明

### 后端配置
- **端口**: 8080 (application.yaml)
- **文件大小限制**: 2048MB
- **临时目录**: F:/temp_dir
- **Whisper脚本路径**: F:\test\fasterWhisper.py

### 前端配置
- **后端API地址**: http://localhost:8080
- **WebSocket地址**: ws://localhost:8080/ws/progress

---
## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！
