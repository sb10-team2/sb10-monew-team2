package com.springboot.monew.exception.backup;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class BackupNotFoundException extends BackupException {
    public BackupNotFoundException(String backupId) {
        super(ErrorCode.BACKUP_NOT_FOUND, Map.of("backupId", backupId));
    }
}
