package com.ler.fm.service.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.ler.fm.config.FmProperties;
import com.ler.fm.model.SearchResult;
import com.ler.fm.service.FileSearcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Leron
 */
@Slf4j
public class DefaultSearcher implements FileSearcher, InitializingBean {

    private final FmProperties fmProperties;

    public DefaultSearcher(FmProperties fmProperties) {
        this.fmProperties = fmProperties;
    }

    private final Set<String> localIndex = new ConcurrentHashSet<>();

    @Override
    public Set<SearchResult> search(String fileName) {
        Pattern pattern = Pattern.compile(".*" + fileName + ".*", Pattern.CASE_INSENSITIVE);
        return localIndex.stream().filter(item -> pattern.matcher(item).matches()).map(item -> {
            SearchResult searchResult = new SearchResult();
            searchResult.setFilePath(item);
            return searchResult;
        }).collect(Collectors.toSet());
    }

    @Override
    public void addIndex(String filePath) {
        this.localIndex.add(filePath);
    }

    @Override
    public void removeIndex(String filePath) {
        this.localIndex.remove(filePath);
    }

    private void initLocalIndex(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            localIndex.add(file.getPath());
            if (file.isDirectory()) {
                initLocalIndex(file.getPath());
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("starts to initialize the local index");
        this.initLocalIndex(fmProperties.getStoragePath());
        log.info("local index is initialized");
    }
}
