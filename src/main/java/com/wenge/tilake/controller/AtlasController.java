package com.wenge.tilake.controller;

import com.alibaba.fastjson.JSONObject;
import com.wenge.tilake.exception.SystemErrorType;
import com.wenge.tilake.vo.Result;
import com.wenge.tilake.vo.TableBasicInfoVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
@Api(value = "atlas-Api", tags = "atlas-Api",description="Hive的类型名称，库名称，以及表名称务必要存在且对应。")
@RequestMapping("/atlas/api/v1")
public class AtlasController {

    private AtlasClientV2 atlasClientV2 = new AtlasClientV2(new String[]{"http://gzslave1:21000/"}, new String[]{"admin", "admin%2021"});

    /**
     * 获取Hive指定类型指定库下的所有表的基本信息
     * @return
     */
    @GetMapping("/getTableBasicInfo")
    @ApiOperation("获取Hive指定类型指定库下的所有表的基本信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "typeName",value = "Hive的类型名称(非必填，默认为hive_db)",required = false,dataTypeClass = String.class,paramType = "query"),
            @ApiImplicitParam(name = "DbName",value = "数据库名称(非必填，默认为weather_data)",required = false,dataTypeClass = String.class,paramType = "query")
    })
    public Result getTableBasicInfo(@RequestParam(value = "typeName",required = false,defaultValue = "hive_db") String typeName,
                                    @RequestParam(value = "DbName",required = false,defaultValue = "weather_data")String DbName) {

        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = this.getTableFromTypeAndDb(typeName, DbName);
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
            return Result.success(this.getTableAttributesByGuids(guidList));
        }
        return Result.fail(SystemErrorType.FAIL);
    }

    //获取Hive指定类型指定库下指定表的guid
    @GetMapping("/getTableGuid")
    @ApiOperation("获取Hive指定类型指定库下指定表的guid")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "typeName",value = "Hive的类型名称(非必填，默认为hive_db)",required = false,dataTypeClass = String.class,paramType = "query"),
            @ApiImplicitParam(name = "DbName",value = "数据库名称(非必填，默认为weather_data)",required = false,dataTypeClass = String.class,paramType = "query"),
            @ApiImplicitParam(name = "tableName",value = "表名称(非必填，默认为early_warn_source)",required = false,dataTypeClass = String.class,paramType = "query")
    })
    public Result getTableGuid(@RequestParam(value = "typeName",required = false,defaultValue = "hive_db") String typeName,
                               @RequestParam(value = "DbName",required = false,defaultValue = "weather_data")String DbName,
                               @RequestParam(value = "tableName",required = false,defaultValue = "early_warn_source")String tableName) {

        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = this.getTableFromTypeAndDb(typeName, DbName);
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

    /**
     * 根据表的Guid查询表血缘
     * @return
     */
    @GetMapping("/getTableLineageByGuid")
    @ApiOperation("根据表的Guid查询表血缘，传参-guid（表的guid，String）")
    public Result getTableLineageByGuid(@RequestParam("guid") String guid){
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

    /**
     * 根据表的Guid查询对应实体信息
     * @returnq
     */
    @GetMapping("/getEntityByGuid")
    @ApiOperation("根据表的Guid查询对应实体信息，传参-guid（表的guid，String）")
    public Result getEntityByGuid(@RequestParam("guid") String guid){
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
    private List<TableBasicInfoVo> getTableAttributesByGuids(List<String> tableGuidList) {
        if(tableGuidList==null || tableGuidList.size()<=0){
            return null;
        }
        List<TableBasicInfoVo> tableList = new ArrayList<>();
        try {
            AtlasEntity.AtlasEntitiesWithExtInfo entitiesByGuids = atlasClientV2.getEntitiesByGuids(tableGuidList);
            List<AtlasEntity> entityList = entitiesByGuids.getEntities();
            for (AtlasEntity atlasEntity : entityList) {
                TableBasicInfoVo tableBasicInfoVo = new TableBasicInfoVo();
                Map<String, Object> relationshipAttributesMap = atlasEntity.getRelationshipAttributes();
                Map<String, Object> attributesMap = atlasEntity.getAttributes();
                String guid = atlasEntity.getGuid();
                Date createTime = atlasEntity.getCreateTime();
                String comment = String.valueOf(attributesMap.get("comment"));
                String tableName = String.valueOf(attributesMap.get("name"));
                Map map= (Map) relationshipAttributesMap.get("db");
                String db = String.valueOf(map.get("displayText"));
                tableBasicInfoVo.setTableGuid(guid).setTableName(tableName).setTableComment(comment).setDb(db).setCreateTime(createTime);
                tableList.add(tableBasicInfoVo);
            }
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        return tableList;
    }





}

