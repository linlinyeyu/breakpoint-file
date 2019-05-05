package com.ybliu.breakpoint.util;

import com.ybliu.breakpoint.domain.SiteInfo;
import com.ybliu.breakpoint.pool.FileThreadPool;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by linlinyeyu on 2019/4/29
 * @author linlinyeyu
 */
@Slf4j
public class DownloadFile {
    private static final int NOACCESS = -2;
    private SiteInfo siteInfo;
    private long[] startpos;
    private long[] endpos;
    private FileSplitFetch[] fileSplitFetches;
    /**
     * 是否第一次下载
     */
    private boolean firstDown = true;
    private File infoFile;

    public DownloadFile(SiteInfo siteInfo) {
        this.siteInfo = siteInfo;
        infoFile = new File(siteInfo.getFilePath()+File.separator+siteInfo.getSimpleName()+".tmp");
        if (infoFile.exists()) {
            firstDown = false;
            readInfo();
        } else {
            startpos = new long[siteInfo.getSplits()];
            endpos = new long[siteInfo.getSplits()];
        }
    }

    /**
     * 多线程客户端下载
     */
    public void startDownload() {
        if (firstDown) {
            long fileLen = getFileSize();
            if (fileLen == -1 || fileLen == -2) {
                return;
            } else {
                //设置每次下载开始位置
                for (int i = 0;i<startpos.length;i++) {
                    startpos[i] = i*(fileLen /startpos.length);
                }
                //设置每次下载结束位置
                System.arraycopy(startpos, 1, endpos, 0, endpos.length - 1);
                endpos[endpos.length - 1] = fileLen;
            }
        }

        fileSplitFetches = new FileSplitFetch[startpos.length];
        FileThreadPool fileThreadPool = FileThreadPool.getInstance();
        try {
            for (int i = 0;i<startpos.length;i++) {
                log.info(startpos[i] + " " + endpos[i]);
                fileSplitFetches[i] = new FileSplitFetch(siteInfo.getUrl(), startpos[i], endpos[i], i, siteInfo.getFilePath() + File.separator + siteInfo.getFileName());
                fileThreadPool.submit(fileSplitFetches[i]);
            }
            saveInfo();
            boolean breakWhile;
            do {
                breakWhile = true;
                for (int i = 0; i < startpos.length; i++) {
                    if (!fileSplitFetches[i].downOver()) {
                        breakWhile = false;
                        break;
                    }
                }
            } while (!breakWhile);
            fileThreadPool.shutdown();
            fileThreadPool.awaitTermination(1000,TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
                e.printStackTrace();
        }
    }

    /**
     * 保存文件下载信息
     */
    private void saveInfo() {
        try {
            DataOutputStream output = new DataOutputStream(new FileOutputStream(infoFile));
            output.writeInt(startpos.length);
            for(int i = 0; i < startpos.length; i++){
                output.writeLong(fileSplitFetches[i].startpos());
                output.writeLong(fileSplitFetches[i].endpos());
            }
            output.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private long getFileSize() {
        int len = -1;
        try {
            URL url = new URL("http://localhost:8080/file/size?name="+siteInfo.getFileName());
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent","custom");
            int respCode = connection.getResponseCode();
            if (respCode >= 400) {
               log.error("errcode :{}",respCode);
               return NOACCESS;
            }

            len = connection.getContentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return len;
    }

    /**
     * 读取文件下载保存的信息
     */
    private void readInfo() {
        try {
            DataInputStream input = new DataInputStream(new FileInputStream(infoFile));
            int count = input.readInt();
            startpos = new long[count];
            endpos = new long[count];
            for(int i = 0; i < count; i++){
                startpos[i] = input.readLong();
                endpos[i] = input.readLong();
            }

            input.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
