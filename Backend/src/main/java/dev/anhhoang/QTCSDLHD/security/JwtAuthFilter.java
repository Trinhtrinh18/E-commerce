package dev.anhhoang.QTCSDLHD.security;

import dev.anhhoang.QTCSDLHD.services.JwtUtil;
import dev.anhhoang.QTCSDLHD.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // This makes it a Spring bean
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get the Authorization header from the request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out.println("JwtAuthFilter: Processing request for URI: " + request.getRequestURI());
        System.out.println("JwtAuthFilter: Authorization header: " + authHeader);

        // 2. Check if the header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JwtAuthFilter: No Bearer token found or invalid header. Passing to next filter.");
            filterChain.doFilter(request, response); // If not, pass the request to the next filter
            return;
        }

        // 3. Extract the token from the header (remove "Bearer ")
        jwt = authHeader.substring(7);
        System.out.println("JwtAuthFilter: Extracted JWT: " + jwt);

        // 4. Extract the user's email from the token
        userEmail = jwtUtil.extractUsername(jwt);
        System.out.println("JwtAuthFilter: Extracted user email: " + userEmail);

        // 5. Check if the user is not already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println(
                    "JwtAuthFilter: User email is not null and no existing authentication. Loading user details.");
            // 6. Load user details from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            System.out.println("JwtAuthFilter: UserDetails loaded for: " + userDetails.getUsername());

            // 7. Validate the token
            if (jwtUtil.validateToken(jwt, userDetails)) {
                System.out.println("JwtAuthFilter: JWT is valid. Setting authentication in SecurityContextHolder.");
                // 8. If the token is valid, create an authentication token and set it in the
                // security context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("JwtAuthFilter: JWT validation failed.");
            }
        } else if (userEmail == null) {
            System.out.println("JwtAuthFilter: User email extracted from JWT is null.");
        } else {
            System.out.println(
                    "JwtAuthFilter: User is already authenticated or userEmail is null. Current authentication: "
                            + SecurityContextHolder.getContext().getAuthentication());
        }
        // Pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}