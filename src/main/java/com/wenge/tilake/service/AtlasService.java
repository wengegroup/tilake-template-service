package com.wenge.tilake.service;

import com.wenge.tilake.vo.Result;

public interface AtlasService {

    Result getTableBasicInfo(String typeName, String dbName);

    Result getTableGuid(String typeName, String dbName, String tableName);

    Result getTableLineageByGuid(String guid);

    Result getEntityByGuid(String guid);
}
