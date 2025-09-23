package com.text2sql.text2sql_springboot.security;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;

@Component
public class CurrentUserFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;

    public CurrentUserFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        try {

            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String idToken = auth.substring(7);
                // Firebase: verify and get UID
                var decoded = firebaseAuth.verifyIdToken(idToken);
                String uid = decoded.getUid();
                // 1) Store for RLS
                AppContext.setCurrentUserId(uid);

                // 2) Mark request authenticated (avoid 401s)
                var authentication = new UsernamePasswordAuthenticationToken(uid, null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                throw new Exception("No Authorization header found");
            }

            chain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } finally {
            AppContext.clear();
        }
    }
}
