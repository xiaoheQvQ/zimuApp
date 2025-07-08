package com.hsx.videototext.model.dto;

import com.hsx.videototext.utils.UploadFile;
import lombok.Data;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class UploadVideoDTO {


    private String videoId;


    @UploadFile(max = 500, message = "视频文件不能超过 500 M")
    private MultipartFile videoFile;


    @UploadFile(max = 5, message = "视频封面不能超过 5 M")
    private MultipartFile coverFile;


    private String cover;


    @NotNull(message = "用户信息不能为空")
    private Long userId;


    @NotBlank(message = "视频标题不能为空")
    private String title;


    private String description;


    @NotBlank(message = "视频数据缺失")
    private String md5;

    private Float duration;
}
