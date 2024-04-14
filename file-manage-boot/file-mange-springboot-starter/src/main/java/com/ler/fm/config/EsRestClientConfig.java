package com.ler.fm.config;

import org.elasticsearch.client.RequestOptions;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leron
 */
@Configuration
public class EsRestClientConfig {

    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder  = RequestOptions.DEFAULT.toBuilder();
        COMMON_OPTIONS = builder.build();
    }

}

