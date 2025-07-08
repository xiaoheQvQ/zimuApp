package com.hsx.videototext.controller;

import com.hsx.videototext.model.dto.UploadVideoDTO;
import com.hsx.videototext.utils.R;
import com.hsx.videototext.utils.SpeechToWavUtil;
import com.hsx.videototext.utils.VideoToWavUtil;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @description:
 * @author：何世兴，微信：MrHe1006
 * @date: 2025-06-30
 * @author:2372781727@qq.com
 */
@RestController
@RequestMapping("/video")
@CrossOrigin(origins = {"http://localhost:8081", "http://127.0.0.1:8081"}, allowCredentials = "true")
public class ziMuController {

    @Autowired
    private TaskExecutor taskExecutor;

    //统一临时目录路径
    private static final String TEMP_DIR = "F:/temp_dir";
    @PostConstruct
    public void init() {
        //确保临时目录存在
        File tempDir = new File(TEMP_DIR);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R WavSub(@RequestParam("videoFile") MultipartFile videoFile,
                   @RequestParam(value = "model", required = false, defaultValue = "whisper-large") String model) {
        try {
            // 验证文件
            if (videoFile == null || videoFile.isEmpty()) {
                return R.failure("请选择视频文件");
            }

            // 检查文件类型
            String contentType = videoFile.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return R.failure("请上传有效的视频文件");
            }

            // 检查文件大小 (500MB)
            if (videoFile.getSize() > 500 * 1024 * 1024) {
                return R.failure("视频文件大小不能超过500MB");
            }

            // 生成唯一的任务ID
            String taskId = UUID.randomUUID().toString();

            // 立即返回响应，告知前端任务已开始
            R response = R.success().setData(taskId);
            response.setMsg("视频上传成功，开始处理...");

            // 异步处理视频
            taskExecutor.execute(() -> {
                try {
                    // 发送开始处理的消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("开始处理上传的视频文件...\n选择的模型: " + model);
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("文件大小: " + (videoFile.getSize() / 1024 / 1024) + "MB");

                    // 处理视频生成字幕
                    String result = VideoToWavUtil.UploadVideo(videoFile);

                    // 发送完成消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("视频处理完成！生成的字幕文件：" + result);

                } catch (Exception e) {
                    // 发送错误消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("处理失败：" + e.getMessage());
                    e.printStackTrace();
                }
            });

            return response;

        } catch (Exception e) {
            return R.failure("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadSubtitle(@RequestParam String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(file.getAbsolutePath());
        Resource resource = new InputStreamResource(Files.newInputStream(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("text/vtt"))
                .body(resource);
    }

    @PostMapping(value = "/upload/english", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R englishVideoToText(@RequestParam("videoFile") MultipartFile videoFile) {
        try {
            // 验证文件
            if (videoFile == null || videoFile.isEmpty()) {
                return R.failure("请选择视频文件");
            }

            // 检查文件类型
            String contentType = videoFile.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return R.failure("请上传有效的视频文件");
            }

            // 检查文件大小 (500MB)
            if (videoFile.getSize() > 500 * 1024 * 1024) {
                return R.failure("视频文件大小不能超过500MB");
            }

            // 生成唯一的任务ID
            String taskId = UUID.randomUUID().toString();

            // 立即返回响应，告知前端任务已开始
            R response = R.success().setData(taskId);
            response.setMsg("英语视频上传成功，开始处理...");

            // 异步处理视频
            taskExecutor.execute(() -> {
                try {
                    // 发送开始处理的消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("开始处理上传的英语视频文件...");

                    // 创建临时目录
                    File customTempDir = new File("F:/temp_dir");
                    if (!customTempDir.exists()) {
                        customTempDir.mkdirs();
                    }

                    // 将MultipartFile保存到临时文件
                    File tempFile = File.createTempFile("uploaded_english_video_", ".mp4", customTempDir);
                    tempFile.deleteOnExit();

                    try (InputStream inputStream = videoFile.getInputStream()) {
                        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    // 处理视频生成英语文本
                    String result = SpeechToWavUtil.videoToEnglishText(tempFile.getAbsolutePath());

                    // 发送完成消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("英语视频处理完成！生成的文本文件：" + result);

                } catch (Exception e) {
                    // 发送错误消息
                    com.hsx.videototext.websocket.ProgressWebSocketHandler.sendProgress("处理失败：" + e.getMessage());
                    e.printStackTrace();
                }
            });

            return response;

        } catch (Exception e) {
            return R.failure("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/download/text")
    public ResponseEntity<Resource> downloadTextFile(@RequestParam String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(file.getAbsolutePath());
        Resource resource = new InputStreamResource(Files.newInputStream(path));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(resource);
    }
}
