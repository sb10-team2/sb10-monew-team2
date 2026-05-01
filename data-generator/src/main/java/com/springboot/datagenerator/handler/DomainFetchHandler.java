package com.springboot.datagenerator.handler;

import com.springboot.datagenerator.constant.MonewDomain;
import com.springboot.datagenerator.task.FetchTask;
import java.util.List;
import java.util.Map;

public interface DomainFetchHandler {

  boolean matchDomain(MonewDomain domain);

  List<Map<String, Object>> fetch(FetchTask task);

  String transform(List<Map<String, Object>> chunk);
}
