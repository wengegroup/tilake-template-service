package com.wenge.tilake.config;

import org.apache.atlas.AtlasClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasConfig {

    @Value("${atlas.address}")
    private String address;
    @Value("${atlas.name}")
    private String name;
    @Value("${atlas.password}")
    private String password;

    @Bean
    public AtlasClientV2 getAtlasClientV2(){
        return new AtlasClientV2(new String[]{address}, new String[]{name, password});
    }

}
