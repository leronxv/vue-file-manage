package com.ler.fm.service;

import com.ler.fm.request.MoveFileRequest;
import com.ler.fm.vo.FileSimpleDigest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Leron
 */
public interface FileManageService {

    /**
     * 获取文件夹树形列表
     * @return
     */
    FileSimpleDigest folderTree();

    /**
     * 获取目录下的文件/文件夹
     * @param path
     * @return
     */
    List<FileSimpleDigest> listFileByPath(String path);

    /**
     * 多文件上传
     * @param files
     * @param path
     */
    void multiUpload(MultipartFile[] files, String path);

    /**
     * 移动文件路径
     * @param request
     * @return
     */
    void movePath(MoveFileRequest request);

    /**
     * 创建文件夹
     * @param pathName
     */
    void createFolder(String pathName);

    /**
     * 删除文件/文件夹
     * @param pathName
     */
    void removePath(String pathName);

    /**
     * 重命名文件/文件夹
     * @param pathName
     * @param newName
     */
    void renamePath(String pathName, String newName);
}
