package com.springboot.monew.exception.backup;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class BackupSerializationFailedException extends BackupException {
    public BackupSerializationFailedException(String reason) {
        super(ErrorCode.BACKUP_SERIALIZATION_FAILED, Map.of("cause", "SERIALIZATION_FAILED"));
    }
}
