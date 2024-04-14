package com.ler.fm.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ler.fm.config.EsRestClientConfig;
import com.ler.fm.config.FmProperties;
import com.ler.fm.exception.BusinessException;
import com.ler.fm.model.EsFileDigest;
import com.ler.fm.model.SearchResult;
import com.ler.fm.service.FileSearcher;
import com.ler.fm.vo.FileSimpleDigest;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Leron
 */
@Slf4j
public class ElasticSearchSearcher implements FileSearcher, InitializingBean {

    private final FmProperties fmProperties;

    private final RestHighLevelClient restHighLevelClient;

    public ElasticSearchSearcher(FmProperties fmProperties, RestHighLevelClient restHighLevelClient) {
        this.fmProperties = fmProperties;
        this.restHighLevelClient = restHighLevelClient;
    }

    @Override
    public Set<SearchResult> search(String fileName) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchPhraseQuery("fileName", fileName));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("fileName");
        sourceBuilder.highlighter(highlightBuilder);

        SearchRequest searchRequest = new SearchRequest(fmProperties.getEsIndex());
        searchRequest.source(sourceBuilder);
        try {
            SearchResponse response = restHighLevelClient.search(searchRequest, EsRestClientConfig.COMMON_OPTIONS);
            SearchHits hits = response.getHits();
            Set<SearchResult> results = new HashSet<>();
            for (SearchHit item : hits) {
                ObjectMapper objectMapper = SpringUtil.getBean(ObjectMapper.class);
                EsFileDigest esFileDigest = objectMapper.readValue(item.getSourceAsString(), EsFileDigest.class);
                SearchResult searchResult = new SearchResult();
                HighlightField highlightField = item.getHighlightFields().get("fileName");
                searchResult.setFileName(highlightField.getFragments()[0].string().replace("<em>", "<span style='color:red'>").replace("</em>","</span>"));
                searchResult.setFilePath(esFileDigest.getFilePath());
                results.add(searchResult);
            }
            return results;
        } catch (Exception ex) {
            log.error("文件搜索失败",ex);
            throw new BusinessException("文件搜索失败!");
        }
    }

    @Override
    public void addIndex(String filePath) {

    }

    @Override
    public void removeIndex(String filePath) {

    }

    private void initElasticIndex(String path, Map<String, EsFileDigest> filesMap) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            EsFileDigest esFileDigest = new EsFileDigest();
            esFileDigest.setFileName(file.getName());
            esFileDigest.setFilePath(file.getPath());
            esFileDigest.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()));
            filesMap.put(file.getName(), esFileDigest);
            if (file.isDirectory()) {
                FileSimpleDigest child = new FileSimpleDigest();
                child.setFilePath(file.getPath());
                child.setFileName(file.getName());
                initElasticIndex(file.getPath(), filesMap);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ObjectMapper objectMapper = SpringUtil.getBean(ObjectMapper.class);
        log.info("starts to initialize the elasticsearch index");
        GetIndexRequest request = new GetIndexRequest(fmProperties.getEsIndex());
        if (restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT)) {
            log.info("es index already exists");
            return;
        }
        Map<String, EsFileDigest> filesMap = new HashMap<>();
        this.initElasticIndex(fmProperties.getStoragePath(), filesMap);
        BulkRequest bulkRequest = new BulkRequest();
        filesMap.forEach((k, v) -> {
            try {
                IndexRequest indexRequest = new IndexRequest(fmProperties.getEsIndex());
                indexRequest.id(v.getFilePath());
                indexRequest.source(objectMapper.writeValueAsBytes(v), XContentType.JSON);
                bulkRequest.add(indexRequest);
            } catch (JsonProcessingException ex) {
                log.error("索引添加失败", ex);
            }
        });
        BulkResponse bulk;
        try {
            bulk = restHighLevelClient.bulk(bulkRequest, EsRestClientConfig.COMMON_OPTIONS);
            if (bulk.hasFailures()) {
                // 降级为本地缓存 TODO
                log.error("elasticsearch index initialization failed, downgrade to local cache");
            }
        } catch (IOException e) {
            // 降级为本地缓存 TODO
            log.error("elasticsearch index initialization failed, downgrade to local cache");
        }
        log.info("elasticsearch index is initialized");
    }
}
