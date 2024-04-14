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

}
