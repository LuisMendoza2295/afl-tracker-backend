package com.afl.tracker.web.context;

import com.afl.tracker.domain.model.UserInfo;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped context for storing authenticated user information.
 * This makes the authenticated user available throughout the request lifecycle
 * without needing to pass it explicitly.
 */
@RequestScoped
public class AuthContext {
    
    private UserInfo userInfo;
    private String token;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
