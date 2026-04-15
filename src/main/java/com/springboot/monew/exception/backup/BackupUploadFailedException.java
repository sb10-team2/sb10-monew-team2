package com.springboot.monew.exception.backup;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class BackupUploadFailedException extends BackupException {
    public BackupUploadFailedException(String fileName) {
        super(ErrorCode.BACKUP_UPLOAD_FAILED, Map.of("fileName", fileName));
    }
}
