package com.wenge.tilake.template.exception;

import lombok.Getter;

@Getter
public enum SystemErrorType implements ErrorType {

    SYSTEM_ERROR("-1", "系统异常"),
    SYSTEM_BUSY("000001", "系统繁忙,请稍候再试"),
    NOT_LOGIN("000401", "请求未认证，跳转登录页"),

    GATEWAY_NOT_FOUND_SERVICE("010404", "服务未找到"),
    GATEWAY_ERROR("010500", "网关异常"),
    GATEWAY_CONNECT_TIME_OUT("010002", "网关超时"),

    ARGUMENT_NOT_VALID("020000", "请求参数校验不通过"),
    INVALID_TOKEN("020001", "无效token"),
    UPLOAD_FILE_SIZE_LIMIT("020010", "上传文件大小超过限制"),

    DUPLICATE_PRIMARY_KEY("030000", "唯一键冲突"),
    DATA_NOT_EXIST("040000", "数据不存在"),
    DATA_EXIST("040001", "数据已存在"),

    DATA_ILLEGAL("040002", "数据不合法"),

    REMOTE_CALL_FAILED("050000", "远程调用失败"),
    INVALID_DATA_SOURCE("050001","无效的数据源"),

    REQUEST_FAILED("060000","请求错误，请填写正确的数据！"),

    FAIL("070000","Hive的类型和库名不存在或不匹配"),
    TABLE_NOT_EXIST("080000","该库下不存在该表"),
    GUID_NOT_EXIST("090000","Guid不存在")

    ;
    /**
     * 错误类型码
     */
    private String code;
    /**
     * 错误类型描述信息
     */
    private String mesg;

    SystemErrorType(String code, String mesg) {
        this.code = code;
        this.mesg = mesg;
    }
}
