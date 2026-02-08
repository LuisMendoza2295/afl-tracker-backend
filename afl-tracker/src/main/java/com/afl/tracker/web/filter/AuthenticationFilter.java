package com.afl.tracker.web.filter;

import java.io.IOException;

import com.afl.tracker.domain.model.UserInfo;
import com.afl.tracker.domain.port.AuthPort;
import com.afl.tracker.web.annotation.Authenticated;
import com.afl.tracker.web.context.AuthContext;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that intercepts requests to @Authenticated endpoints.
 * Validates the Bearer token and populates the AuthContext with user information.
 */
@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Inject
    AuthPort authPort;

    @Inject
    AuthContext authContext;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Missing or invalid Authorization header")
                    .build()
            );
            return;
        }

        try {
            String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
            
            // Validate token
            authPort.validateToken(token);
            
            // Get and store user info in context
            UserInfo userInfo = authPort.getUserInfo(token);
            authContext.setUserInfo(userInfo);
            authContext.setToken(token);
            
        } catch (Exception e) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Authentication failed: " + e.getMessage())
                    .build()
            );
        }
    }
}
