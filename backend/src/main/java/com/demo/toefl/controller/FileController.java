package com.demo.toefl.controller;

import com.demo.toefl.vo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    // 音频保存目录，默认是「项目根目录/upload/audio/」
    @Value("${upload.audio.path:upload/audio/}")
    private String uploadPath;

    @PostMapping("/uploadAudio")
    public Result<String> uploadAudio(@RequestParam("file") MultipartFile file,
                                      // ext：录音格式后缀。Chrome/Edge 录出来是 webm，Safari 是 mp4；
                                      // 后缀和真实内容对不上时浏览器会拒绝播放，所以让前端把真实后缀传上来
                                      @RequestParam(value = "ext", required = false, defaultValue = "webm") String ext) {
        try {
            // 只保留字母数字，避免用 ext 拼出路径穿越（如 ../../xxx）
            String safeExt = ext.replaceAll("[^A-Za-z0-9]", "");
            if (safeExt.isEmpty() || safeExt.length() > 5) {
                safeExt = "webm";
            }

            File folder = resolveAudioFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                return Result.fail("录音目录创建失败：" + folder.getAbsolutePath());
            }

            // 重命名，避免同名覆盖
            String fileName = UUID.randomUUID() + "." + safeExt.toLowerCase();
            // 注意：MultipartFile.transferTo(相对路径) 会被写到 Tomcat 临时目录，
            // 必须先把目标解析成绝对路径，再交给 transferTo
            File dest = new File(folder, fileName);
            file.transferTo(dest);

            // 返回访问路径给前端存进数据库（WebConfig 已把 /upload/audio/** 映射到同一个目录）
            return Result.success("/upload/audio/" + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("音频上传失败");
        }
    }

    /**
     * 把配置里的相对路径（upload/audio/）解析成绝对路径。
     * 开发时工作目录是 backend/，打包运行时是 jar 所在目录，都能落在同一个位置，
     * 这样 WebConfig 的静态资源映射和这里写文件才对得上。
     */
    private File resolveAudioFolder() {
        String path = uploadPath == null ? "upload/audio/" : uploadPath.trim();
        File folder = new File(path);
        if (!folder.isAbsolute()) {
            folder = new File(System.getProperty("user.dir"), path);
        }
        return folder.getAbsoluteFile();
    }
}
