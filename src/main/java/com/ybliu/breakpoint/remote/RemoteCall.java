package com.ybliu.breakpoint.remote;


import com.ybliu.breakpoint.domain.SiteInfo;
import com.ybliu.breakpoint.util.DownloadFile;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author linlinyeyu
 */
public class RemoteCall {
    /**
     * 测试下载
     * @param url
     * @param from
     * @param to
     * @param savePath
     * @return
     */
    public static boolean downloadFile(String url,int from,int to,String savePath) {
        try {
            URL link = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)link.openConnection();
            if (to != 0) {
                connection.setRequestProperty("Range","bytes="+from+"-"+to);
            } else {
                connection.setRequestProperty("Range","bytes="+from+"-");
            }
            if (connection.getResponseCode() == 206) {
                RandomAccessFile file = new RandomAccessFile(savePath,"rw");
                file.seek(from);
                InputStream in = connection.getInputStream();
                byte[] buffer = new byte[2014];
                int num;
                while ((num = in.read(buffer)) > 0) {
                    file.write(buffer,0,num);
                }
                file.close();
                in.close();
                return true;
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String args[]) {
        SiteInfo siteInfo = new SiteInfo("http://localhost:8080/file/download?name=V1.3.5.tgz","E:/workspace","V1.3.5.tgz",5);
        DownloadFile downloadFile = new DownloadFile(siteInfo);
        downloadFile.startDownload();
    }
}
