package com.wenge.tilake.template.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger2配置类！需要在启动类上面开启swagger2
 * 注意：RequestHandlerSelectors.basePackage("com.dk.controller") 为 Controller 包路径，不然生成的文档扫描不到接口
 */
@Configuration
public class Swagger2Config {

    public static final String VERSION = "1.0.0";

    @Bean
    public Docket createRestApi() {

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.wenge.tilake.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("天湖数据组件")  // 大标题
                .description("天湖数据组件 API 接口文档") //描述
                .termsOfServiceUrl("http://www.baidu.com")  //网络服务地址。公司网址
                .version(VERSION)     //自定义版本号  比如1.0.0或0.0.1 等等
                .build();
    }
}
