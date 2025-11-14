package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.Config.UrlProps;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class QueryServiceCallbackUrlFactory {
    private UrlProps urlProps;

    public QueryServiceCallbackUrlFactory(UrlProps urlProps) {
        this.urlProps = urlProps;
    }

    public String buildJobCallbackUrl(String jobId) {
        return UriComponentsBuilder
                .fromUriString(urlProps.getPublicBaseUrl())
                .pathSegment("query", "jobs", "{id}", "callback")
                .buildAndExpand(jobId)
                .toUriString();
    }

}
