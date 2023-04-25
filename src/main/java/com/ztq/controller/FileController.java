package com.ztq.controller;

import com.ztq.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Value("${file.base}")
    String base;


    @PostMapping("/upload")
    public Result<String> imgUpload(MultipartFile file){

        File file1=new File(base);
        if(!file1.exists()){
            file1.mkdirs();
        }
        String originalFilename = file.getOriginalFilename();
        String type = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName= UUID.randomUUID().toString()+type;

            try {
                file.transferTo(new File(base+fileName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        return Result.success(fileName);
    }

    @GetMapping("/download")
    public Result<String> downLoad(String name, HttpServletResponse response){
        String fileName=base+name;

        /*try {
            FileInputStream fileInputStream=new FileInputStream(new File(fileName));
            ServletOutputStream outputStream = response.getOutputStream();
            int len;
            byte [] bytes=new byte[1024];
            while ((len=fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
             BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {

            byte[] buffer = new byte[8192]; // or other appropriate size
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Result.success("下载成功");
    }
}
