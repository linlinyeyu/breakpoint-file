package com.ybliu.breakpoint.controller;

import com.ybliu.breakpoint.domain.MultipartFileParam;
import com.ybliu.breakpoint.handler.FileHandler;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

/**
 * @author linlinyeyu
 */
@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileHandler fileHandler;

    public static final String BASE_PATH = "E:/workspace";
    /**
     * 断点下载文件
     * @param name
     * @param request
     * @param response
     * @throws IOException
     */
    @GetMapping(value = "/download",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadObject(@RequestParam String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        fileHandler.downloadFile(name,request,response);
    }

    /**
     * 获取文件大小
     * @param name
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping(value = "/size",produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Object> fileSize(@RequestParam String name,HttpServletResponse response) throws IOException {
        long size = fileHandler.getFileSize(name);
        return ResponseEntity.ok().contentLength(size).build();
    }

    /**
     * 上传多个文件
     * @param request
     * @return
     */
    @PostMapping(value = "/multi/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String multiFileUpload(HttpServletRequest request) {
        List<MultipartFile> files = ((MultipartHttpServletRequest)request).getFiles("file");
        files.forEach(f -> {
            if (!f.isEmpty()) {
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(BASE_PATH+"/"+f.getOriginalFilename())))){
                    byte[] bytes = f.getBytes();
                    out.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return "success";
    }

    /**
     * 上传文件
     * @param file
     * @return
     */
    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String fileUpload(@RequestParam("file")MultipartFile file) {
        if (!file.isEmpty()) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(BASE_PATH+"/"+file.getOriginalFilename())))){
                byte[] bytes = file.getBytes();
                out.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "success";
    }

    /**
     * 分块上传
     * @param param
     * @param request
     * @param response
     * @return
     */
    @PostMapping(value = "/chunk/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String fileChunkUpload(MultipartFileParam param, HttpServletRequest request, HttpServletResponse response) {
        //判断前端是否支持文件上传
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {
            try {
                fileHandler.chunkFileUpload(param);
            } catch (IOException e) {
                e.printStackTrace();
                return "fail";
            }
        }
        return "success";
    }

}
