package com.ler.fm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Leron
 */
@Data
@ConfigurationProperties(prefix = "fm")
public class FmProperties {

    /**
     * 文件存储跟路径
     */
    private String storagePath;

    /**
     * 文件索引类型
     */
    private String fileIndex;

    /**
     * es 配置
     */
    private ElasticsearchConf elasticsearch;

    /**
     * es 文件索引名称
     */
    private String esIndex = "fm_mange";

    @Data
    public static class ElasticsearchConf {

        private String host;

        private Integer port;

    }

}
