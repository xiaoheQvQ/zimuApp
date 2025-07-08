package com.hsx.videototext.utils;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.hsx.videototext.websocket.ProgressWebSocketHandler;

/**
 * 视频处理工具类
 */
public class VideoToWavUtil {

    /**
     * 完整的视频转字幕流程（从在线视频URL）
     */
    public static String videoUrlToVtt(String videoUrl) throws Exception {
        // 1. 下载在线视频
        File videoFile = downloadVideoFromUrl(videoUrl);

        // 2. 处理视频生成字幕
        return processVideoToVtt(videoFile);
    }

    /**
     * 从在线视频URL下载视频
     */
    public static File downloadVideoFromUrl(String videoUrl) throws IOException {
        System.out.println("开始下载在线视频: " + videoUrl);

        // 创建临时文件
        File customTempDir = new File("F:/temp_dir");
        if (!customTempDir.exists()) {
            customTempDir.mkdirs();
        }
        File tempFile = File.createTempFile("temp_video_", ".mp4", customTempDir);
        tempFile.deleteOnExit();

        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(videoUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("视频下载失败，HTTP响应码: " + responseCode);
            }

            // 获取文件大小（用于进度显示）
            long fileSize = connection.getContentLengthLong();
            System.out.println("视频文件大小: " + fileSize + " bytes");

            // 下载文件
            inputStream = connection.getInputStream();
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("视频下载完成，保存到: " + tempFile.getAbsolutePath());
            return tempFile;

        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 处理视频生成VTT字幕
     */
    public static String UploadVideo(MultipartFile file) throws Exception {
        ProgressWebSocketHandler.sendProgress("开始处理上传的视频文件...");
        ProgressWebSocketHandler.sendProgress("文件大小: " + (file.getSize() / 1024 / 1024) + " MB");

        // 创建临时目录
        File customTempDir = new File("F:/temp_dir");
        if (!customTempDir.exists()) {
            customTempDir.mkdirs();
        }

        // 将MultipartFile保存到临时文件
        File tempFile = File.createTempFile("uploaded_video_", ".mp4", customTempDir);
        tempFile.deleteOnExit();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // 处理视频生成字幕
        String result = processVideoToVtt(tempFile);

        ProgressWebSocketHandler.sendProgress("视频处理完成！");
        return result;
    }

    /**
     * 从视频文件到VTT字幕
     */
    public static String processVideoToVtt(File videoFile) throws Exception {
        ProgressWebSocketHandler.sendProgress("开始视频处理流程...");

        // 1. 提取音频
        ProgressWebSocketHandler.sendProgress("步骤1/3: 准备提取音频...");
        String audioPath = extractAudioFromVideo(videoFile.getAbsolutePath());

        // 2. 语音转文字
        ProgressWebSocketHandler.sendProgress("步骤2/3: 准备语音转文字...");
        String subtitleText = transcribeAudio(audioPath);

        // 3. 生成VTT文件
        ProgressWebSocketHandler.sendProgress("步骤3/3: 准备生成字幕文件...");
        return generateVttFile(subtitleText, audioPath);
    }

    /**
     * 从视频中提取音频
     */
    private static String extractAudioFromVideo(String videoPath) throws Exception {
        System.out.println("开始从视频中提取音频...");
        ProgressWebSocketHandler.sendProgress("开始从视频中提取音频...");

        String audioPath = videoPath.replace(".mp4", "_audio.wav");
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;

        try {
            ProgressWebSocketHandler.sendProgress("正在初始化音频提取器...");
            grabber = new FFmpegFrameGrabber(videoPath);
            grabber.start();
            grabber.setAudioStream(0); // 选择第一个音频流

            ProgressWebSocketHandler.sendProgress("正在配置音频录制器...");
            recorder = new FFmpegFrameRecorder(audioPath, 1); // 单声道
            recorder.setFormat("wav");
            recorder.setSampleRate(16000);
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_PCM_S16LE);
            recorder.setAudioOption("ar", "16000");
            recorder.setAudioOption("ac", "1");
            recorder.setAudioOption("sample_fmt", "s16");
            recorder.setAudioQuality(0);
            recorder.start();

            ProgressWebSocketHandler.sendProgress("开始提取音频数据...");
            Frame frame;
            int frameCount = 0;
            int totalFrames = 0;

            // 先计算总帧数（用于进度计算）
            while ((frame = grabber.grab()) != null) {
                if (frame.samples != null) {
                    totalFrames++;
                }
            }

            // 重新开始提取
            grabber.stop();
            grabber.start();
            grabber.setAudioStream(0);

            while ((frame = grabber.grab()) != null) {
                if (frame.samples != null) {
                    recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                    frameCount++;
                    if (frameCount % 50 == 0) {
                        int progress = (int) ((frameCount * 100.0) / totalFrames);
                        System.out.println("音频提取进度: " + progress + "% (" + frameCount + "/" + totalFrames + ")");
                        ProgressWebSocketHandler.sendProgress("音频提取进度: " + progress + "% (" + frameCount + "/" + totalFrames + ")");
                    }
                }
            }

            System.out.println("音频提取完成，共处理 " + frameCount + " 帧");
            ProgressWebSocketHandler.sendProgress("音频提取完成，共处理 " + frameCount + " 帧");

        } finally {
            if (recorder != null) {
                try { recorder.stop(); } catch (Exception e) { e.printStackTrace(); }
                try { recorder.release(); } catch (Exception e) { e.printStackTrace(); }
            }
            if (grabber != null) {
                try { grabber.stop(); } catch (Exception e) { e.printStackTrace(); }
                try { grabber.release(); } catch (Exception e) { e.printStackTrace(); }
            }
        }

        return audioPath;
    }

    /**
     * 语音转文字
     */
    private static String transcribeAudio(String audioPath) throws Exception {
        System.out.println("开始语音转文字...");
        ProgressWebSocketHandler.sendProgress("开始语音转文字...");

        String pythonScriptPath = "F:\\test\\fasterWhisper.py";
        ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath, audioPath);
        pb.redirectErrorStream(true);

        ProgressWebSocketHandler.sendProgress("正在启动Whisper语音识别引擎...");
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                lineCount++;
                System.out.println("[Whisper] " + line); // 实时输出转写进度

                // 智能解析Whisper输出
                if (line.contains("Loading model")) {
                    ProgressWebSocketHandler.sendProgress("正在加载Whisper模型...");
                } else if (line.contains("Transcribing")) {
                    ProgressWebSocketHandler.sendProgress("正在转写音频内容...");
                } else if (line.contains("Processing")) {
                    ProgressWebSocketHandler.sendProgress("正在处理音频片段...");
                } else if (line.contains("Detected language")) {
                    ProgressWebSocketHandler.sendProgress("检测到语言: " + line);
                } else if (line.contains("Transcription completed")) {
                    ProgressWebSocketHandler.sendProgress("转写完成！");
                } else if (lineCount % 10 == 0) {
                    // 每10行输出一次进度
                    ProgressWebSocketHandler.sendProgress("语音转写进行中... (已处理 " + lineCount + " 行输出)");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("语音转文字失败，退出码: " + exitCode);
        }

        System.out.println("语音转文字完成");
        ProgressWebSocketHandler.sendProgress("语音转文字完成，共处理 " + lineCount + " 行输出");
        return output.toString();
    }

    /**
     * 生成VTT字幕文件（确保UTF-8无BOM编码）
     */
    private static String generateVttFile(String subtitleText, String audioPath) throws IOException {
        System.out.println("开始生成VTT字幕文件...");
        ProgressWebSocketHandler.sendProgress("开始生成VTT字幕文件...");

        String vttPath = audioPath.replace("_audio.wav", ".vtt");

        // 使用FileOutputStream直接写入字节，避免BOM问题
        try (OutputStream out = new FileOutputStream(vttPath);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            // 写入VTT文件头
            writer.write("WEBVTT");
            writer.newLine();
            writer.newLine();
            ProgressWebSocketHandler.sendProgress("已写入VTT文件头");

            String[] blocks = subtitleText.split("\n\n");
            int totalBlocks = blocks.length;
            ProgressWebSocketHandler.sendProgress("共需要处理 " + totalBlocks + " 个字幕块");

            for(int i = 0; i < blocks.length; i++) {
                String block = blocks[i].trim();
                if (block.isEmpty()) {
                    continue;
                }

                // 报告当前处理进度
                int progress = (int) ((i + 1) * 100.0 / totalBlocks);
                String progressMsg = String.format("正在处理字幕块 %d/%d (%.1f%%)",
                        i + 1, totalBlocks, (i + 1) * 100.0 / totalBlocks);
                ProgressWebSocketHandler.sendProgress(progressMsg);

                String[] lines = block.split("\n");
                if (lines.length >= 3) {
                    // 序号
                    writer.write(String.valueOf(i + 1));
                    writer.newLine();
                    // 时间轴（VTT格式与SRT相同）
                    writer.write(lines[1].replace(",", ".")); // VTT使用点作为毫秒分隔符
                    writer.newLine();
                    // 内容（添加前缀）
                    writer.write("=== " + lines[2]);
                    writer.newLine();
                    // 空行分隔
                    writer.newLine();

                    // 每处理10个块报告一次详细进度
                    if ((i + 1) % 10 == 0 || (i + 1) == totalBlocks) {
                        ProgressWebSocketHandler.sendProgress(
                                String.format("已处理 %d/%d 个字幕块", i + 1, totalBlocks));
                    }
                }
            }
        }

        String completionMsg = "VTT字幕文件已生成(UTF-8无BOM): " + vttPath;
        System.out.println(completionMsg);
        ProgressWebSocketHandler.sendProgress(completionMsg);
        return vttPath;
    }

    /**
     * 清理临时文件
     */
    public static void cleanTempFiles(String... filePaths) {
        for (String path : filePaths) {
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    if (file.delete()) {
                        System.out.println("已删除临时文件: " + path);
                    } else {
                        System.out.println("删除临时文件失败: " + path);
                    }
                }
            }
        }
    }
}