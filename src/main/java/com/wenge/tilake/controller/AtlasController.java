package com.wenge.tilake.controller;

import com.alibaba.fastjson.JSONObject;
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
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
@Api(value = "atlas-Api", tags = "atlas-Api")
@RequestMapping("/atlas/api/v1")
public class AtlasController {

    private AtlasClientV2 atlasClientV2 = new AtlasClientV2(new String[]{"http://gzslave1:21000/"}, new String[]{"admin", "admin%2021"});

    /**
     * 查询表名以及guid
     * @return
     */
    @GetMapping("/getTableBasicInfo")
    @ApiOperation("查询表的基本信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "typeName",value = "Hive的类型名称(非必填)",required = false,dataTypeClass = String.class,paramType = "path"),
            @ApiImplicitParam(name = "DbName",value = "数据库名称(非必填)",required = false,dataTypeClass = String.class,paramType = "path")
    })
    public Result getTableBasicInfo(@RequestParam(value = "typeName",required = false) String typeName,@RequestParam(value = "DbName",required = false)String DbName) {

        if(typeName==null || StringUtils.equals(typeName,"")){
            typeName = "hive_db";
        }
        if(DbName==null || StringUtils.equals(DbName,"")){
            DbName = "weather_data";
        }
        //获取名称为 weather_data 的库相关信息
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = null;
        try {
            AtlasSearchResult atlasSearchResultDB = atlasClientV2.basicSearch(typeName, "", "name="+DbName, true,1000,0);
            //获取 weather_data 库的 guid
            String guid = atlasSearchResultDB.getEntities().get(0).getGuid();
            //根据 guid  获取 weather_data库里的所有表
            entityByGuid = atlasClientV2.getEntityByGuid(guid);
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        //获取表的一些attributes
        List<String> guidList = new ArrayList<>();
        List tables = (List) entityByGuid.getEntity().getRelationshipAttributes().get("tables");
        for (Object table : tables) {
            Map map = JSONObject.parseObject(JSONObject.toJSONString(table), Map.class);
            if(map.get("entityStatus").equals("ACTIVE")){
                String tableGuid = (String)map.get("guid");
                guidList.add(tableGuid);
            }
        }
        return Result.success(this.getTableAttributesByGuids(guidList));
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
        return Result.success(lineageInfo);
    }

    /**
     * 根据表的Guid查询对应实体信息
     * @returnq
     */
    @GetMapping("/getEntityByGuid")
    @ApiOperation("根据表的Guid查询对应实体信息，传参-guid（表的guid，String）")
    public Result getEntityByGuid(@RequestParam("guid") String guid){
        AtlasEntity.AtlasEntityWithExtInfo entityByGuid = null;
        AtlasRelationship.AtlasRelationshipWithExtInfo relationshipByGuid = null;
        try {
            entityByGuid = atlasClientV2.getEntityByGuid(guid);
            //atlasClientV2.
        } catch (AtlasServiceException e) {
            e.printStackTrace();
        }
        return Result.success(entityByGuid);
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

