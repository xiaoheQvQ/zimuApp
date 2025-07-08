package com.hsx.videototext.utils;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * 英语语音转文字工具类
 * @author 何世兴
 */
public class SpeechToWavUtil {

    /**
     * 从视频中提取音频并保存为WAV文件
     */
    public static String extractAudioToWav(String videoPath) throws Exception {
        File file = new File(videoPath);
        if (!file.exists()) {
            throw new IOException("视频文件不存在: " + videoPath);
        }

        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoPath);
        Frame frame = null;
        FFmpegFrameRecorder recorder = null;
        String fileName = null;

        try {
            frameGrabber.start();

            Random random = new Random();
            fileName = videoPath.substring(0, videoPath.lastIndexOf('.')) + "_audio_" + random.nextInt(100) + ".wav";
            System.out.println("生成的音频文件名: " + fileName);

            recorder = new FFmpegFrameRecorder(fileName, frameGrabber.getAudioChannels());
            recorder.setFormat("wav");
            recorder.setSampleRate(frameGrabber.getSampleRate());
            recorder.setTimestamp(frameGrabber.getTimestamp());
            recorder.setAudioQuality(0);
            recorder.start();

            int index = 0;
            while (true) {
                frame = frameGrabber.grab();
                if (frame == null) {
                    System.out.println("视频处理完成");
                    break;
                }
                if (frame.samples != null) {
                    recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                }
                System.out.println("处理帧: " + index);
                index++;
            }

            return fileName;
        } finally {
            if (recorder != null) {
                try { recorder.stop(); } catch (Exception e) { e.printStackTrace(); }
                try { recorder.release(); } catch (Exception e) { e.printStackTrace(); }
            }
            if (frameGrabber != null) {
                try { frameGrabber.stop(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * 将英语音频转换为文字
     */
    public static String transcribeEnglishAudio(String audioFilePath) throws IOException {
        Configuration configuration = new Configuration();

        // 设置英语模型路径
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        StringBuilder resultText = new StringBuilder();
        
        try (InputStream stream = new FileInputStream(new File(audioFilePath))) {
            recognizer.startRecognition(stream);
            SpeechResult result;
            
            while ((result = recognizer.getResult()) != null) {
                String hypothesis = result.getHypothesis();
                resultText.append(hypothesis).append(" ");
                System.out.println("识别结果: " + hypothesis);
            }
        } finally {
            recognizer.stopRecognition();
        }
        
        return resultText.toString().trim();
    }

    /**
     * 完整的英语视频转文字流程
     */
    public static String videoToEnglishText(String videoPath) throws Exception {
        // 1. 提取音频
        String audioPath = extractAudioToWav(videoPath);
        
        // 2. 语音转文字
        String text = transcribeEnglishAudio(audioPath);
        
        // 3. 生成文本文件
        String textFilePath = videoPath.substring(0, videoPath.lastIndexOf('.')) + "_transcription.txt";
        try (java.io.FileWriter writer = new java.io.FileWriter(textFilePath)) {
            writer.write(text);
        }
        
        return textFilePath;
    }
}