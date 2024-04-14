package com.ler.fm.service.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.ler.fm.config.FmProperties;
import com.ler.fm.exception.BusinessException;
import com.ler.fm.request.MoveFileRequest;
import com.ler.fm.service.FileManageService;
import com.ler.fm.vo.FileSimpleDigest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Leron
 */
@Slf4j
@Service
public class FileManageServiceImpl implements FileManageService, InitializingBean {

    @Resource
    private FmProperties fmProperties;

    private final Set<String> localIndex = new ConcurrentHashSet<>();

    @Override
    public FileSimpleDigest folderTree() {
        FileSimpleDigest fileSimpleInfo = new FileSimpleDigest();
        fileSimpleInfo.setFileName("/");
        fileSimpleInfo.setFilePath(fmProperties.getStoragePath());
        this.traverseFolder(fileSimpleInfo, false);
        return fileSimpleInfo;
    }

    @Override
    public List<FileSimpleDigest> listFileByPath(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files == null) return Collections.emptyList();
        List<File> fileList = Arrays.asList(files);
        Set<File> fileSet = new HashSet<>(fileList);
        List<FileSimpleDigest> fileInfoList = new ArrayList<>();
        if (fileSet.isEmpty()) return Collections.emptyList();
        for (File file : fileSet) {
            String fileName = file.getName();
            String fileExtension = "";
            int lastIndex = fileName.lastIndexOf('.');
            if (lastIndex > 0) {
                fileExtension = fileName.substring(lastIndex + 1);
            }
            FileSimpleDigest fileSimpleInfo = new FileSimpleDigest();
            fileSimpleInfo.setFilePath(file.getPath());
            fileSimpleInfo.setFileName(fileName);
            fileSimpleInfo.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()));
            fileSimpleInfo.setFileSize(formatFileSize(file.length()));
            if (file.isDirectory()) {
                fileSimpleInfo.setFileType("path");
            } else {
                fileSimpleInfo.setFileType(fileExtension);
            }
            fileInfoList.add(fileSimpleInfo);
        }
        return fileInfoList;
    }

    @Override
    public void multiUpload(MultipartFile[] files, String path) {
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            try {
                String filePath = fmProperties.getStoragePath() + path;
                String fileName = file.getOriginalFilename();
                Path targetPath = Paths.get(filePath, fileName);
                Files.createDirectories(targetPath.getParent());
                file.transferTo(targetPath);
                this.localIndex.add(filePath);
            } catch (IOException e) {
                log.error("文件{}保存失败:", file.getName(), e);
                throw new BusinessException("文件" + file.getName() + "保存失败");
            }
        }
    }

    @Override
    public void movePath(MoveFileRequest request) {
        String originPath = this.fmProperties.getStoragePath() + request.getPath();
        Path source = Paths.get(originPath);
        String targetPath = this.fmProperties.getStoragePath() + request.getTargetPath() + "/" + request.getFileName();
        Path target = Paths.get(targetPath);
        try {
            Files.move(source, target);
            this.localIndex.remove(originPath);
            this.localIndex.add(targetPath);
        } catch (FileAlreadyExistsException aex) {
            throw new BusinessException("文件已存在!");
        } catch (IOException e) {
            log.error("文件移动失败:path:{},targetPath:{}", request.getPath(), targetPath, e);
            throw new BusinessException("文件移动失败，文件或文件夹不存在");
        }
    }

    @Override
    public void createFolder(String pathName) {
        String folderPath = this.fmProperties.getStoragePath() + pathName;
        try {
            Path path = Paths.get(folderPath);
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException aex) {
            throw new BusinessException("文件夹已存在");
        } catch (IOException e) {
            log.error("文件夹:{}创建失败", folderPath, e);
            throw new BusinessException("文件夹创建失败");
        }
    }

    @Override
    public void removePath(String pathName) {
        try {
            pathName = this.fmProperties.getStoragePath() + pathName;
            Path path = Paths.get(pathName);
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } else {
                this.localIndex.remove(pathName);
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.error("无法删除路径:{}", pathName, e);
            throw new BusinessException("无法删除路径:" + pathName + "文件或文件夹可能被占用，请稍后再试");
        }
    }

    @Override
    public void renamePath(String pathName, String newName) {
        Path path = Paths.get(this.fmProperties.getStoragePath() + pathName);
        try {
            Path targetPath = Files.move(path, path.resolveSibling(newName));
            File file = path.toFile();
            if (file.isFile()) {
                this.localIndex.remove(pathName);
                this.localIndex.add(targetPath.toString());
            }
        } catch (FileAlreadyExistsException aex) {
            throw new BusinessException("文件或文件夹已存在");
        } catch (IOException e) {
            log.error("名称更改失败:path:{},newName:{}", path, newName, e);
            throw new BusinessException("名称更改失败");
        }
    }

    @Override
    public List<FileSimpleDigest> search(String fileName) {
        Pattern pattern = Pattern.compile(".*" + fileName + ".*", Pattern.CASE_INSENSITIVE);
        List<FileSimpleDigest> list = new ArrayList<>();
        localIndex.stream().filter(item -> pattern.matcher(item).matches()).collect(Collectors.toList()).forEach(item -> {
            File file = new File(item);
            String fileExtension = "";
            int lastIndex = file.getName().lastIndexOf('.');
            if (lastIndex > 0) {
                fileExtension = file.getName().substring(lastIndex + 1);
            }
            FileSimpleDigest fileSimpleDigest = new FileSimpleDigest();
            fileSimpleDigest.setFileName(file.getName());
            fileSimpleDigest.setFilePath(file.getPath());
            fileSimpleDigest.setFileType(fileExtension);
            fileSimpleDigest.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()));
            fileSimpleDigest.setFileSize(formatFileSize(file.length()));
            list.add(fileSimpleDigest);
        });
        return list;
    }

    private void traverseFolder(FileSimpleDigest fileSimpleInfo, boolean isInit) {
        File folder = new File(fileSimpleInfo.getFilePath());
        File[] files = folder.listFiles();
        if (files == null) return;
        fileSimpleInfo.setChild(new ArrayList<>());
        for (File file : files) {
            if (file.isDirectory()) {
                FileSimpleDigest child = new FileSimpleDigest();
                child.setFilePath(file.getPath());
                child.setFileName(file.getName());
                fileSimpleInfo.getChild().add(child);
                traverseFolder(child, isInit);
            } else {
                if (isInit) {
                    localIndex.add(file.getPath());
                }
            }
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @Override
    public void afterPropertiesSet() {
        log.info("starts to initialize the local index");
        FileSimpleDigest fileSimpleInfo = new FileSimpleDigest();
        fileSimpleInfo.setFilePath(fmProperties.getStoragePath());
        this.traverseFolder(fileSimpleInfo, true);
        log.info("local index is initialized");
    }
}
