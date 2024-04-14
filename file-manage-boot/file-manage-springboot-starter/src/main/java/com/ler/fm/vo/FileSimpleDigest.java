package com.ler.fm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Leron
 */
@Data
public class FileSimpleDigest {

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型
     * md、png、txt (如果是路径则为 path)
     */
    private String fileType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 文件大小
     */
    private String fileSize;

    /**
     * 子目录
     */
    private List<FileSimpleDigest> child;

}
