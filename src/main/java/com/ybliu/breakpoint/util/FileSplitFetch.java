package com.ybliu.breakpoint.util;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by linlinyeyu on 2019/4/29
 */
@Slf4j
@Data
@Accessors(fluent = true,chain = true)
public class FileSplitFetch implements Runnable {
    private String url;
    private long startpos;
    private long endpos;
    private int threadId;
    private boolean downOver = false;
    private boolean stop = false;
    FileUtil fileUtil = null;

    public FileSplitFetch(String url, long startpos, long endpos, int threadId, String filename) throws IOException {
        super();
        this.url = url;
        this.startpos = startpos;
        this.endpos = endpos;
        this.threadId = threadId;
        fileUtil = new FileUtil(filename,startpos);
    }

    @Override
    public void run() {
        while (startpos < endpos && !stop) {
            log.info("threadId:{}",threadId);
            try {
                URL ourl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) ourl.openConnection();
                String prop = "bytes=" + startpos + "-";
                connection.setRequestProperty("RANGE",prop);
                InputStream input = connection.getInputStream();
                byte[] b = new byte[1024];
                int bytes = 0;
                while ((bytes = input.read(b)) > 0 && startpos < endpos && !stop) {
                    startpos += fileUtil.write(b,0,bytes);
                }
                downOver = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printResponseHeader(HttpURLConnection connection) {
        for(int i = 0; ; i++){
            String fieldsName = connection.getHeaderFieldKey(i);
            if(fieldsName != null){
                log.info(fieldsName + ":" + connection.getHeaderField(fieldsName));
            }else{
                break;
            }
        }
    }

    public void setSplitTransStop() {
        stop = true;
    }
}
