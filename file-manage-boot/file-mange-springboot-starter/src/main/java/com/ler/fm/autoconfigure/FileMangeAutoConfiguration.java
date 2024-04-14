package com.ler.fm.autoconfigure;

import com.ler.fm.config.FmProperties;
import com.ler.fm.service.FileSearcher;
import com.ler.fm.service.impl.DefaultSearcher;
import com.ler.fm.service.impl.ElasticSearchSearcher;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
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
    public FileSearcher defaultFileSearcher(FmProperties fmProperties) {
        return new DefaultSearcher(fmProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fm", name = "file-index", havingValue = "elasticsearch")
    public FileSearcher fileSearcher(FmProperties fmProperties, RestHighLevelClient restHighLevelClient) {
        return new ElasticSearchSearcher(fmProperties, restHighLevelClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "fm", name = "file-index", havingValue = "elasticsearch")
    public RestHighLevelClient esRestClient(FmProperties fmProperties) {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(fmProperties.getElasticsearch().getHost(), fmProperties.getElasticsearch().getPort(), "http"))
        );
    }
}
