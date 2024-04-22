package com.ler.fm.controller;

import com.ler.fm.config.FmProperties;
import com.ler.fm.exception.BusinessException;
import com.ler.fm.model.Response;
import com.ler.fm.request.MoveFileRequest;
import com.ler.fm.service.FileManageService;
import com.ler.fm.vo.FileSimpleDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Leron
 */
@Slf4j
@RestController
@RequestMapping("/file-manager")
public class FileMangeController {

    @Resource
    private FmProperties fmProperties;

    @Resource
    private FileManageService fileManageService;

    @GetMapping("/folder-tree")
    public Response<FileSimpleDigest> getFileTree() {
        return Response.success(fileManageService.folderTree());
    }

    @GetMapping("/list-files")
    public Response<?> listFilesByPath(String path) {
        return Response.success(fileManageService.listFileByPath(path));
    }

    @PostMapping("/multi-upload")
    public Response<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files, @RequestParam String path) {
        fileManageService.multiUpload(files, path);
        return Response.success();
    }

    @PostMapping("/move-path")
    public Response<?> movePath(@RequestBody MoveFileRequest request) {
        fileManageService.movePath(request);
        return Response.success();
    }

    @GetMapping("/create-folder")
    public Response<?> createFolder(@RequestParam String pathName) {
        fileManageService.createFolder(pathName);
        return Response.success();
    }

    @GetMapping("/remove-path")
    public Response<?> removePath(String pathName) {
        fileManageService.removePath(pathName);
        return Response.success();
    }

    @GetMapping("/rename-path")
    public Response<?> renamePath(@RequestParam String pathName, @RequestParam String newName) {
        fileManageService.renamePath(pathName, newName);
        return Response.success();
    }

    @GetMapping("/file-search/{fileName}")
    public Response<?> search(@PathVariable String fileName) {
        return Response.success(fileManageService.search(fileName));
    }

    @GetMapping("/unzip")
    public Response<?> unzip(@RequestParam String filePath) {
        fileManageService.unzip(filePath);
        return Response.success();
    }

    @GetMapping("/static/**")
    public void view(HttpServletRequest request, HttpServletResponse response) {
        String filePath = getFilePath(request);
        File file = new File(filePath);
        if (!file.exists()) {
            response.setStatus(404);
            throw new BusinessException("文件不存在..");
        }

        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(filePath)));
             OutputStream outputStream = response.getOutputStream()) {
            response.setContentType("application/force-download");
            response.addHeader("Content-Disposition", "attachment;fileName=" + new String(file.getName().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.error("预览文件失败", e);
            response.setStatus(404);
        }
    }

    @GetMapping("/download/**")
    public void download(HttpServletRequest request, HttpServletResponse response) {
        this.view(request, response);
    }

    private String getFilePath(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String decodedPath = null;
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("解码异常");
        }
        if (decodedPath == null) {
            return null;
        }
        String filePath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, decodedPath);
        return fmProperties.getStoragePath() + filePath;
    }


}
