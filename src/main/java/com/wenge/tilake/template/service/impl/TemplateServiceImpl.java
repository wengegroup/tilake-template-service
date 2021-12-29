package com.wenge.tilake.template.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.wenge.tilake.template.exception.SystemErrorType;
import com.wenge.tilake.template.service.TemplateService;
import com.wenge.tilake.template.vo.Result;
import com.wenge.tilake.template.vo.TableBasicInfoVo;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private AtlasClientV2 atlasClientV2;

    @Override
    public Result getTableBasicInfo(String typeName, String dbName) {
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = this.getTableFromTypeAndDb(typeName, dbName);
        if(entityByGuid!=null){
            //获取表的一些attributes
            List<String> guidList = new ArrayList<>();
            List tables = (List) entityByGuid.getEntity().getRelationshipAttributes().get("tables");
            for (Object table : tables) {
                Map map = JSONObject.parseObject(JSONObject.toJSONString(table), Map.class);
                if(StringUtils.equals((String) map.get("entityStatus"),"ACTIVE")){
                    guidList.add((String) map.get("guid"));
                }
            }
            return this.getTableAttributesByGuids(guidList);
        }
        return Result.fail(SystemErrorType.FAIL);
    }

    @Override
    public Result getTableGuid(String typeName, String dbName, String tableName) {
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = this.getTableFromTypeAndDb(typeName, dbName);
        //获取表的一些attributes
        if(entityByGuid!=null){
            List tables = (List) entityByGuid.getEntity().getRelationshipAttributes().get("tables");
            for (Object table : tables) {
                Map map = JSONObject.parseObject(JSONObject.toJSONString(table), Map.class);
                if(StringUtils.equals( (String) map.get("entityStatus"),"ACTIVE") ){
                    if(StringUtils.equals((String) map.get("displayText"),tableName)){
                        return Result.success(map.get("guid"));
                    }
                }
            }
            return Result.fail(SystemErrorType.TABLE_NOT_EXIST);
        }
        return Result.fail(SystemErrorType.FAIL);
    }

    @Override
    public Result getTableLineageByGuid(String guid) {
        AtlasLineageInfo lineageInfo = null;
        try {
            lineageInfo = atlasClientV2.getLineageInfo(guid, AtlasLineageInfo.LineageDirection.BOTH, 100);
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }

        if(lineageInfo!=null){
            return Result.success(lineageInfo);
        }else {
            return Result.fail(SystemErrorType.GUID_NOT_EXIST);
        }
    }

    @Override
    public Result getEntityByGuid(String guid) {
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = null;
        try {
            entityByGuid = atlasClientV2.getEntityByGuid(guid);
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        if(entityByGuid!=null){
            return Result.success(entityByGuid);
        }else {
            return Result.fail(SystemErrorType.GUID_NOT_EXIST);
        }
    }


    /**
     * 从Hive中获取指定类型(type)指定库(Db)下的所有表(table)
     * @returnq
     * @Param typeName：Hive的类型  DbName：库名
     */
    private AtlasEntity.AtlasEntityWithExtInfo getTableFromTypeAndDb(String typeName,String DbName){
        //获取名称为 weather_data 的库相关信息
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = null;
        try {
            AtlasSearchResult atlasSearchResultDB = atlasClientV2.basicSearch(typeName, "", "name="+DbName, true,1000,0);
            //获取 weather_data 库的 guid
            if(atlasSearchResultDB.getEntities()!=null){
                String guid = atlasSearchResultDB.getEntities().get(0).getGuid();
                //根据 guid  获取 weather_data库里的所有表
                entityByGuid = atlasClientV2.getEntityByGuid(guid);
            }
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        return entityByGuid;
    }

    //根据表的guid获取表的一些attributes
    private Map<String,Object> getTableAttributes(String tableGuid) {
        Map<String,Object> resMap = new HashMap<>();
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = null;
        String comment = null;
        String db = null;
        Date createTime = null;
        try {
            entityByGuid = atlasClientV2.getEntityByGuid(tableGuid);
            Map<String, Object> attributesMap = entityByGuid.getEntity().getAttributes();
            Map<String, Object> relationshipAttributesMap = entityByGuid.getEntity().getRelationshipAttributes();
            createTime = entityByGuid.getEntity().getCreateTime();
            comment = String.valueOf(attributesMap.get("comment"));
            Map map= (Map) relationshipAttributesMap.get("db");
            db = String.valueOf(map.get("displayText"));
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        resMap.put("comment",comment);
        resMap.put("db",db);
        resMap.put("createTime",createTime);
        return resMap;
    }

    //封装表的基本信息
    private Result<List<TableBasicInfoVo>> getTableAttributesByGuids(List<String> tableGuidList) {
        if(tableGuidList==null || tableGuidList.size()<=0){
            return Result.fail(SystemErrorType.DATA_NOT_EXIST);
        }
        List<TableBasicInfoVo> tableList = new ArrayList<>();
        try {
            AtlasEntity.AtlasEntitiesWithExtInfo entitiesByGuids = atlasClientV2.getEntitiesByGuids(tableGuidList);
            List<AtlasEntity> entityList = entitiesByGuids.getEntities();
            for (AtlasEntity atlasEntity : entityList) {
                TableBasicInfoVo tableBasicInfoVo = new TableBasicInfoVo();
                Map<String, Object> relationshipAttributesMap = atlasEntity.getRelationshipAttributes();
                Map<String, Object> attributesMap = atlasEntity.getAttributes();
                Map<String, Object> map= (Map) relationshipAttributesMap.get("db");
                tableBasicInfoVo.setTableGuid(atlasEntity.getGuid())
                        .setTableName(String.valueOf(attributesMap.get("name")))
                        .setTableComment(String.valueOf(attributesMap.get("comment")))
                        .setDb(String.valueOf(map.get("displayText"))).setDbType("Hive")
                        .setCreateTime(atlasEntity.getCreateTime()).setUpdateTime(atlasEntity.getUpdateTime());
                tableList.add(tableBasicInfoVo);
            }
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        return Result.success(tableList);
    }
}
