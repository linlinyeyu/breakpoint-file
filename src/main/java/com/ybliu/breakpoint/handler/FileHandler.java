package com.ybliu.breakpoint.handler;

import com.ybliu.breakpoint.domain.MultipartFileParam;
import com.ybliu.breakpoint.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * @author linlinyeyu
 */
@Service
@Slf4j
public class FileHandler {
    private static final String BASE_PATH = "E:/workspace";
    /**
     * 断点续传下载服务端
     * @param name
     * @param request
     * @param response
     * @throws FileNotFoundException
     */
    public void downloadFile(String name, HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException {
        //获取所需文件资源
        String path = ResourceUtils.getURL("classpath:").getPath() + "static/"+name;
        File downloadFile = new File(path);
        String headerValue = String.format("attachment; filename=\"%s\"",downloadFile.getName());
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION,headerValue);
        response.addHeader(HttpHeaders.ACCEPT_RANGES,"bytes");
        //获取文件大小
        long downloadSize = downloadFile.length();
        long fromPos = 0,toPos = 0;
        if (request.getHeader("Range") == null) {
            response.addHeader(HttpHeaders.CONTENT_LENGTH,downloadSize + "");
        } else {
            log.info("range:{}",response.getHeader("Range"));
            //如果为持续下载
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            String range = request.getHeader("Range");
            String bytes = range.replaceAll("bytes=","");
            String[] ary = bytes.split("-");
            fromPos = Long.parseLong(ary[0]);
            log.info("fronPos:{}",fromPos);
            if (ary.length == 2) {
                toPos = Long.parseLong(ary[1]);
            }
            int size;
            if (toPos > fromPos) {
                size = (int) (toPos - fromPos);
            } else {
                size = (int) (downloadSize - fromPos);
            }
            response.addHeader(HttpHeaders.CONTENT_LENGTH,size + "");
            downloadSize = size;
        }

        try (RandomAccessFile in = new RandomAccessFile(downloadFile, "rw");
             OutputStream out = response.getOutputStream()) {
            if (fromPos > 0) {
                in.seek(fromPos);
            }
            int bufLen = (int) (downloadSize < 2048 ? downloadSize : 2048);
            byte[] buffer = new byte[bufLen];
            int num;
            //当前写入客户端大小
            int count = 0;
            while ((num = in.read(buffer)) != -1) {
                out.write(buffer, 0, num);
                count += num;
                if (downloadSize - count < bufLen) {
                    bufLen = (int) (downloadSize - count);
                    if (bufLen == 0) {
                        break;
                    }
                    buffer = new byte[bufLen];
                }
            }
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getFileSize(String name) throws FileNotFoundException {
        String path = ResourceUtils.getURL("classpath:").getPath() + "static/"+name;
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return file.length();
        }
        return -1;
    }

    /**
     * 分块上传
     * 第一步：获取RandomAccessFile,随机访问文件类的对象
     * 第二步：调用RandomAccessFile的getChannel()方法，打开文件通道 FileChannel
     * 第三步：获取当前是第几个分块，计算文件的最后偏移量
     * 第四步：获取当前文件分块的字节数组，用于获取文件字节长度
     * 第五步：使用文件通道FileChannel类的 map（）方法创建直接字节缓冲器  MappedByteBuffer
     * 第六步：将分块的字节数组放入到当前位置的缓冲区内  mappedByteBuffer.put(byte[] b);
     * 第七步：释放缓冲区
     * 第八步：检查文件是否全部完成上传
     * @param param
     * @return
     * @throws IOException
     */
    public void chunkFileUpload(MultipartFileParam param) throws IOException {
        if (param.taskId() == null || "".equals(param.taskId())) {
            param.taskId(UUID.randomUUID().toString());
        }
        /**
         * 1：原文件名改为UUID
         * 2：创建临时文件，和源文件一个路径
         * 3：如果文件路径不存在重新创建
         */
        String fileName = param.file().getOriginalFilename();
        String tempFileName= param.taskId() + fileName.substring(fileName.lastIndexOf(".")) + "_tmp";
        File fileDir = new File(BASE_PATH);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File tempFile = new File(BASE_PATH,tempFileName);
        //第一步
        RandomAccessFile raf = new RandomAccessFile(tempFile,"rw");
        //第二步
        FileChannel channel = raf.getChannel();
        //第三步
        long offset = param.chunk() * param.size();
        //第四步
        byte[] fileData = param.file().getBytes();
        //第五步
        MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE,offset,fileData.length);
        //第六步
        mappedByteBuffer.put(fileData);
        //第七步
        FileUtil.freeMappedByteBuffer(mappedByteBuffer);
        channel.close();
        raf.close();
        //第八步
        boolean isComplete = checkUploadStatus(param,fileName,BASE_PATH);
        if (isComplete) {
            renameFile(tempFile,fileName);
        }
    }

    private boolean checkUploadStatus(MultipartFileParam param,String fileName,String filePath) {
        File confFile = new File(filePath,fileName+".conf");
        try {
            RandomAccessFile confAccessFile = new RandomAccessFile(confFile,"rw");
            //设置文件长度
            confAccessFile.setLength(param.chunkTotal());
            //设置起始偏移量
            confAccessFile.setLength(param.chunk());
            //将指定的一个字节写入文件127
            confAccessFile.write(Byte.MAX_VALUE);
            byte[] completeStatusList = FileUtils.readFileToByteArray(confFile);
            byte isComplete = Byte.MAX_VALUE;
            //这一段逻辑有点复杂，看的时候思考了好久，创建conf文件文件长度为总分片数，每上传一个分块即向conf文件中写入一个127，那么没上传的位置就是默认的0,已上传的就是Byte.MAX_VALUE 127
            for (int i = 0;i< completeStatusList.length && isComplete == Byte.MAX_VALUE;i++) {
                isComplete = (byte)(isComplete & completeStatusList[i]);
            }
            if (isComplete == Byte.MAX_VALUE) {
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 文件重命名
     * @param toBeRenamed   将要修改名字的文件
     * @param toFileNewName 新的名字
     * @return
     */
    public boolean renameFile(File toBeRenamed, String toFileNewName) {
        //检查要重命名的文件是否存在，是否是文件
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            return false;
        }
        String p = toBeRenamed.getParent();
        File newFile = new File(p + File.separatorChar + toFileNewName);
        //修改文件名
        return toBeRenamed.renameTo(newFile);
    }
}
