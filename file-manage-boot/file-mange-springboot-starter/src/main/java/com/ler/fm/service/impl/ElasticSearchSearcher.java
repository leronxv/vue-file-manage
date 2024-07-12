package com.ler.fm.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ler.fm.config.FmProperties;
import com.ler.fm.exception.BusinessException;
import com.ler.fm.model.EsFileDigest;
import com.ler.fm.model.SearchResult;
import com.ler.fm.service.FileSearcher;
import com.ler.fm.vo.FileSimpleDigest;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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
        sourceBuilder.query(QueryBuilders.matchQuery("fileName", fileName).analyzer("ik_max_word"));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("fileName");
        sourceBuilder.highlighter(highlightBuilder);

        SearchRequest searchRequest = new SearchRequest(fmProperties.getEsIndex());
        searchRequest.source(sourceBuilder);
        try {
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            Set<SearchResult> results = new HashSet<>();
            for (SearchHit item : hits) {
                ObjectMapper objectMapper = SpringUtil.getBean(ObjectMapper.class);
                EsFileDigest esFileDigest = objectMapper.readValue(item.getSourceAsString(), EsFileDigest.class);
                SearchResult searchResult = new SearchResult();
                HighlightField highlightField = item.getHighlightFields().get("fileName");
                searchResult.setFileName(highlightField.getFragments()[0].string().replace("<em>", "<span style='color:red'>").replace("</em>", "</span>"));
                searchResult.setFilePath(esFileDigest.getFilePath());
                results.add(searchResult);
            }
            return results;
        } catch (Exception ex) {
            log.error("文件搜索失败", ex);
            throw new BusinessException("文件搜索失败!");
        }
    }

    @Override
    public void addIndex(String filePath) {
        File file = new File(filePath);
        IndexRequest request = new IndexRequest(fmProperties.getEsIndex());
        request.id(filePath);
        EsFileDigest esFileDigest = new EsFileDigest();
        esFileDigest.setFileName(file.getName());
        esFileDigest.setFilePath(file.getPath());
        ObjectMapper objectMapper = SpringUtil.getBean(ObjectMapper.class);
        try {
            request.source(objectMapper.writeValueAsBytes(esFileDigest), XContentType.JSON);
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (Exception ex) {
            log.error("es 索引更新失败", ex);
        }
    }

    @Override
    public void removeIndex(String filePath) {
        DeleteRequest request = new DeleteRequest(fmProperties.getEsIndex(),filePath);
        try {
            restHighLevelClient.delete(request,RequestOptions.DEFAULT);
        } catch (Exception ex) {
            log.error("es 索引更新失败", ex);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ObjectMapper objectMapper = SpringUtil.getBean(ObjectMapper.class);
        log.info("starts to initialize the elasticsearch index");
        if (!fmProperties.isForceInitIndex()) {
            GetIndexRequest request = new GetIndexRequest(fmProperties.getEsIndex());
            if (restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT)) {
                log.info("es index already exists");
                return;
            }
        }
        this.initIndex();
        Map<String, EsFileDigest> filesMap = new HashMap<>();
        this.buildEsFileDigest(fmProperties.getStoragePath(), filesMap);
        if (filesMap.isEmpty()) return;
        BulkRequest bulkRequest = new BulkRequest();
        filesMap.forEach((k, v) -> {
            try {
                IndexRequest indexRequest = new IndexRequest(fmProperties.getEsIndex());
                indexRequest.id(v.getFilePath());
                indexRequest.source(objectMapper.writeValueAsBytes(v), XContentType.JSON);
                bulkRequest.add(indexRequest);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });
        BulkResponse bulk;
        try {
            bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulk.hasFailures()) {
                throw new RuntimeException();
            }
            log.info("elasticsearch index is initialized");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void buildEsFileDigest(String path, Map<String, EsFileDigest> filesMap) {
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
                buildEsFileDigest(file.getPath(), filesMap);
            }
        }
    }

    private void initIndex() {
        DeleteIndexRequest delReq = new DeleteIndexRequest(fmProperties.getEsIndex());
        try {
            restHighLevelClient.indices().delete(delReq, RequestOptions.DEFAULT);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        CreateIndexRequest request = new CreateIndexRequest(fmProperties.getEsIndex());
        try {
            Resource resource = new ClassPathResource("esMap.json");
            StringBuilder jsonMapBuilder = new StringBuilder();
            try (InputStream inputStream = resource.getInputStream();
                 Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    jsonMapBuilder.append(scanner.nextLine());
                }
            }
            String jsonMap = jsonMapBuilder.toString();
            request.mapping(jsonMap, XContentType.JSON);
            CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            boolean acknowledged = createIndexResponse.isAcknowledged();
            if (!acknowledged) {
                throw new RuntimeException();
            }
        } catch (Exception ex) {
           throw new RuntimeException(ex);
        }
    }
}
