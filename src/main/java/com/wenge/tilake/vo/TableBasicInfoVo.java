package com.wenge.tilake.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class TableBasicInfoVo {

    private String tableName;
    private String tableComment;
    private String tableGuid;
    private String DbType;
    private String Db;
    private Date createTime;
}

