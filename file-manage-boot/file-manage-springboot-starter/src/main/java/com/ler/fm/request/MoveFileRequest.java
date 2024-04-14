package com.ler.fm.request;

import lombok.Data;

/**
 * @author Leron
 */
@Data
public class MoveFileRequest {

    private String fileName;

    private String path;

    private String targetPath;

}
