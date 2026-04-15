package com.springboot.monew.exception.backup;

import com.springboot.monew.exception.ErrorCode;
import com.springboot.monew.exception.MonewException;

import java.util.Map;

public abstract class BackupException extends MonewException {
    public BackupException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
