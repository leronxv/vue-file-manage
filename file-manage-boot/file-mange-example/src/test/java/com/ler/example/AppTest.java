package com.ler.example;

import cn.hutool.json.JSONUtil;
import com.ler.fm.model.SearchResult;
import com.ler.fm.service.FileSearcher;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Set;

/**
 * Unit test for simple App.
 */
@Slf4j
@SpringBootTest
public class AppTest {

    @Resource
    private FileSearcher fileSearcher;

    @Test
    public void search() {
        Set<SearchResult> set = fileSearcher.search("文档");
        log.info(JSONUtil.toJsonStr(set));
    }

}
