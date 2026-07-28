package com.gergert.common.security;

import com.gergert.common.dto.jwt.JwtClaimsDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCommonFilter extends OncePerRequestFilter {
    private final JwtTokenValidator tokenValidator;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {

            String jwtToken = getTokenFromRequest(request);

            if (jwtToken != null && tokenValidator.validateJwtToken(jwtToken)) {

                if ("ACCESS".equals(tokenValidator.getTokenType(jwtToken))) {

                    JwtClaimsDto claims = tokenValidator.getClaimsFromToken(jwtToken);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(claims.role().name());

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            claims,
                            null,
                            Collections.singletonList(authority)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    log.debug("User authenticated: id={}, email={}, role={}", claims.userId(), claims.email(), claims.role());
                }
            }
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

}
