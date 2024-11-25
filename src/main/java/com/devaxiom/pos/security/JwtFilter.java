package com.devaxiom.pos.security;

import com.devaxiom.pos.services.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String userName = null;
        String jwtToken = null;

        logger.info("JwtFilter: Starting filter for request URI: {}", request.getRequestURI());

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7);
            logger.info("JwtFilter: Extracted JWT Token: {}", jwtToken);
            userName = jwtService.extractUserNameFromJwt(jwtToken);
            logger.info("JwtFilter: Extracted User Name from JWT: {}", userName);
        } else logger.warn("JwtFilter: No Authorization header or it doesn't start with Bearer");

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            logger.info("JwtFilter: Loaded UserDetails for user: {}", userName);

            if (jwtService.validateJwtToken(jwtToken, userDetails)) {
                logger.info("JwtFilter: JWT Token is valid");
                logger.info("User Authorities: {}", userDetails.getAuthorities());
                UsernamePasswordAuthenticationToken UPAToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                UPAToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(UPAToken);
                logger.info("JwtFilter: Authentication set in SecurityContext for user: {}", userName);
            } else
                logger.warn("JwtFilter: JWT Token is invalid for user: {}", userName);

        } else {
            if (userName == null)
                logger.warn("JwtFilter: userName is null, skipping authentication.");
            else
                logger.warn("JwtFilter: SecurityContext already contains authentication.");
        }

        logger.info("JwtFilter: Proceeding with the filter chain.");
        filterChain.doFilter(request, response);
    }
}
