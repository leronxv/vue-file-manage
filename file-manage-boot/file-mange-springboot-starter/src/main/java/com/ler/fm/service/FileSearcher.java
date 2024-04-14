package com.ler.fm.service;

import com.ler.fm.model.SearchResult;

import java.util.Set;

/**
 * @author Leron
 */
public interface FileSearcher {

    /**
     * 搜索文件
     * @param fileName
     * @return
     */
    Set<SearchResult> search(String fileName);

    /**
     * 添加索引
     * @param filePath
     */
    void addIndex(String filePath);

    /**
     * 删除索引
     * @param filePath
     */
    void removeIndex(String filePath);

}
