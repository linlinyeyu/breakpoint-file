package com.ybliu.breakpoint.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by linlinyeyu on 2019/4/29
 * @author linlinyeyu
 */
@Data
public class SiteInfo implements Serializable {
    private static final int SPLIT_COUNT = 5;

    private String url;
    private String filePath;
    private String fileName;
    /**
     * 分段下载文件次数
     */
    private int splits;

    public SiteInfo() {
        this("","","",SPLIT_COUNT);
    }

    public SiteInfo(String url, String filePath, String fileName, int splits) {
        this.url = url;
        this.filePath = filePath;
        this.fileName = fileName;
        this.splits = splits;
    }

    public String getSimpleName() {
        String[] names = fileName.split("\\.");
        return names[0];
    }
}
