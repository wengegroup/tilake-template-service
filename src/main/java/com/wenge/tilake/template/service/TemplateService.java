package com.wenge.tilake.template.service;

import com.wenge.tilake.template.vo.Result;

public interface TemplateService {

    Result getTableBasicInfo(String typeName, String dbName);

    Result getTableGuid(String typeName, String dbName, String tableName);

    Result getTableLineageByGuid(String guid);

    Result getEntityByGuid(String guid);
}
