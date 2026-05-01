package com.springboot.datagenerator.task;

import com.springboot.datagenerator.constant.MonewDomain;
import java.time.LocalDateTime;

public record FetchTask(MonewDomain domain, String prefix, LocalDateTime cursor, String lastId) {

}
