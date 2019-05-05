package com.ybliu.breakpoint.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * @author linlinyeyu
 */
@Data
@Accessors(fluent = true,chain = true)
public class MultipartFileParam implements Serializable {
    private static final long serialVersionUID = -4945649681932523734L;
    /**
     * 文件传输任务id
     */
    private String taskId;
    /**
     * 当前第几分片
     */
    private Integer chunk;
    /**
     * 每个分块大小
     */
    private Long size;
    /**
     * 分片总数
     */
    private Integer chunkTotal;
    /**
     * 文件对象
     */
    private MultipartFile file;
}
