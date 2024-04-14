package com.ler.fm.autoconfigure;

import com.ler.fm.config.FmProperties;
import com.ler.fm.service.FileSearcher;
import com.ler.fm.service.impl.DefaultSearcher;
import com.ler.fm.service.impl.ElasticSearchSearcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Leron
 */
@Configuration
@Import({FmManageRegistry.class})
@EnableConfigurationProperties(FmProperties.class)
public class FileMangeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fm", name = "file-index", havingValue = "local")
    public FileSearcher defaultFileSearcher() {
        return new DefaultSearcher();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fm", name = "file-index", havingValue = "elasticSearch")
    public FileSearcher fileSearcher() {
        return new ElasticSearchSearcher();
    }

}
