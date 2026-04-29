package com.itheima.controller;


import com.itheima.pojo.Result;
import com.itheima.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final AliOssUtil aliOssUtil;
//本地存储方法
//    @PostMapping("/upload")
//    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
//        //把文件的内容存储到本地磁盘上
//        String originalFilename = file.getOriginalFilename();
//        //使用UUID生成文件名,防止文件名重复发生覆盖问题
//        String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));
//        file.transferTo(new File("E:\\JAVA\\Files\\images\\" + fileName));
//        return Result.success("url访问地址。。。");
//    }


    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        //把文件的内容存储到本地磁盘上
        String originalFilename = file.getOriginalFilename();
        //使用UUID生成文件名,防止文件名重复发生覆盖问题
        String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));
        //调用OssUtil上传文件,获取文件地址链接
        String url = aliOssUtil.uploadFile(fileName, file.getInputStream());
        return Result.success(url);
    }
}
