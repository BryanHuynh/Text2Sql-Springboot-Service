package com.text2sql.text2sql_springboot.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class CurrentUserFilterTest {
    FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    CurrentUserFilter currentUserFilter = new CurrentUserFilter(firebaseAuth);

    @Test
    void setsUserContext_whenBearerTokenIsPresent() throws Exception {
        String uid = "user-123";
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);

        when(firebaseAuth.verifyIdToken("good.token")).thenReturn(token);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer good.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertThat(AppContext.getCurrentUserId()).isEqualTo(uid);
            assertThat(org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal()).isEqualTo(uid);
        };

        currentUserFilter.doFilter(request, response, chain);

        assertThat(AppContext.getCurrentUserId()).isNull();
        verify(firebaseAuth).verifyIdToken("good.token");
    }

    @Test
    void unAuthorized_whenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertThat(AppContext.getCurrentUserId())
                    .as("Within request, App context should not be set")
                    .isEqualTo(null);
        };

        currentUserFilter.doFilter(request, response, chain);
        assertNull(AppContext.getCurrentUserId(), "App context should be null post request completion");
        assertThat(response.getStatus())
                .as("Should have returned unauthorized exception")
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());

    }

    @Test
    void unAuthorized_whenBadFirebaseToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertThat(AppContext.getCurrentUserId()).isEqualTo(null);
        };
        when(firebaseAuth.verifyIdToken("bad.token")).thenThrow(FirebaseAuthException.class);

        currentUserFilter.doFilter(request, response, chain);
        assertThat(AppContext.getCurrentUserId()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @AfterEach
    void cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        AppContext.clear();
    }
}