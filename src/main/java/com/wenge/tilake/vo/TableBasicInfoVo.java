package com.wenge.tilake.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class TableBasicInfoVo {

    @ApiModelProperty(name = "tableName", value = "表英文名")
    private String tableName;

    @ApiModelProperty(name = "tableComment", value = "表中文名")
    private String tableComment;

    @ApiModelProperty(name = "id", value = "表的guid")
    private String tableGuid;

    @ApiModelProperty(name = "id", value = "数据源类型")
    private String DbType;

    @ApiModelProperty(name = "id", value = "数据库")
    private String Db;

    @ApiModelProperty(name = "id", value = "创建时间")
    private Date createTime;

}

