package com.springboot.monew.exception.backup;

import com.springboot.monew.exception.ErrorCode;

import java.util.Map;

public class RecoveryFailedException extends BackupException {
    public RecoveryFailedException(String reason) {
        super(ErrorCode.RECOVERY_FAILED, Map.of("reason", reason));
    }
}
