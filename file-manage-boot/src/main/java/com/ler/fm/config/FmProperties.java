package com.ler.fm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leron
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fm")
public class FmProperties {

    /**
     * 文件存储跟路径
     */
    private String storagePath;

}
