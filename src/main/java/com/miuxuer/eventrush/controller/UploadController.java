package com.miuxuer.eventrush.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 图片上传接口。
 *
 * <p>文件不存数据库，直接落到本地上传目录，由 Nginx 挂载该目录对外提供访问，
 * 接口只返回相对路径给前端拼接。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    /**
     * 上传笔记配图。
     *
     * @param image 上传的文件
     * @return 文件的相对访问路径，形如 /posts/3/7/xxxx.jpg
     */
    @PostMapping("image")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            String originalFilename = image.getOriginalFilename();
            // 生成新的文件名和存放目录
            String fileName = createNewFileName(originalFilename);
            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
            log.debug("文件上传成功，{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除已上传的图片。
     *
     * @param filename 上传接口返回的相对路径
     * @return 无
     */
    @GetMapping("/image/delete")
    public Result deleteImage(@RequestParam("name") String filename) {
        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
        // 防止传入一个目录路径把整个目录删掉
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

    /**
     * 生成不冲突的存放路径。
     *
     * <p>用文件名的 hashCode 取两位十六进制，拆成两级子目录，避免所有图片都堆在同一个目录下。
     * 文件名本身用 UUID 重命名，防止同名覆盖，也避免泄露用户原始文件名。</p>
     *
     * @param originalFilename 用户上传时的原始文件名
     * @return 形如 /posts/{d1}/{d2}/{uuid}.{后缀} 的相对路径
     */
    private String createNewFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 目录不存在就先建出来
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/posts/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return StrUtil.format("/posts/{}/{}/{}.{}", d1, d2, name, suffix);
    }
}
