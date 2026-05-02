package com.springboot.datagenerator.task;

import com.springboot.datagenerator.constant.MonewApi;
import java.time.LocalDateTime;

public record FetchTask(MonewApi domain, String prefix, LocalDateTime cursor, String lastId) {

}
