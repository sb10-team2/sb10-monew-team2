package com.springboot.monew.exception.backup;

import com.springboot.monew.common.exception.ErrorCode;
import com.springboot.monew.common.exception.MonewException;

import java.util.Map;

public class BackupException extends MonewException {
    public BackupException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }
}
