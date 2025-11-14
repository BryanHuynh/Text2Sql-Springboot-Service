package com.text2sql.text2sql_springboot.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class UrlProps {
    private String publicBaseUrl;

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String s) {
        this.publicBaseUrl = s;
    }
}
