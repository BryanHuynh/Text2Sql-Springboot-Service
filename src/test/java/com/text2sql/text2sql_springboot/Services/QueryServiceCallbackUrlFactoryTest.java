package com.text2sql.text2sql_springboot.Services;

import com.text2sql.text2sql_springboot.Config.UrlProps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class QueryServiceCallbackUrlFactoryTest {

    private QueryServiceCallbackUrlFactory factory;
    private UrlProps urlProps;

    @BeforeEach
    void setUp() {
        urlProps = new UrlProps();
        urlProps.setPublicBaseUrl("http://localhost:8080");
        factory = new QueryServiceCallbackUrlFactory(urlProps);
    }


    @Test
    @DisplayName("Should build callback URL with UUID job ID")
    void testBuildJobCallbackUrl_WithUuidJobId() {
        // Given
        String jobId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        String result = factory.buildJobCallbackUrl(jobId);

        // Then
        assertEquals("http://localhost:8080/query/jobs/550e8400-e29b-41d4-a716-446655440000/callback", result);
    }

    @Test
    @DisplayName("Should build callback URL with different base URL")
    void testBuildJobCallbackUrl_WithDifferentBaseUrl() {
        // Given
        urlProps.setPublicBaseUrl("https://api.example.com");
        factory = new QueryServiceCallbackUrlFactory(urlProps);
        String jobId = "456";

        // When
        String result = factory.buildJobCallbackUrl(jobId);

        // Then
        assertEquals("https://api.example.com/query/jobs/456/callback", result);
    }

    @Test
    @DisplayName("Should build callback URL with base URL containing trailing slash")
    void testBuildJobCallbackUrl_WithTrailingSlashInBaseUrl() {
        // Given
        urlProps.setPublicBaseUrl("http://localhost:8080/");
        factory = new QueryServiceCallbackUrlFactory(urlProps);
        String jobId = "789";

        // When
        String result = factory.buildJobCallbackUrl(jobId);

        // Then
        // UriComponentsBuilder should handle trailing slash properly
        String expected = "http://localhost:8080/query/jobs/789/callback";
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should build callback URL with base URL containing path")
    void testBuildJobCallbackUrl_WithBaseUrlContainingPath() {
        // Given
        urlProps.setPublicBaseUrl("http://localhost:8080/api/v1");
        factory = new QueryServiceCallbackUrlFactory(urlProps);
        String jobId = "999";

        // When
        String result = factory.buildJobCallbackUrl(jobId);

        // Then
        assertEquals("http://localhost:8080/api/v1/query/jobs/999/callback", result);
    }


}