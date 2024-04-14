package com.ler.fm.service.impl;

import com.ler.fm.service.FileSearcher;
import com.ler.fm.vo.FileSimpleDigest;

import java.util.Collections;
import java.util.Set;

/**
 * @author Leron
 */
public class DefaultSearcher implements FileSearcher {


    @Override
    public Set<FileSimpleDigest> search(String fileName) {
        return Collections.emptySet();
    }

    @Override
    public void addIndex(String filePath) {

    }

    @Override
    public void removeIndex(String filePath) {

    }
}
