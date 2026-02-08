package com.afl.tracker.adapter.auth;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.afl.tracker.adapter.auth.client.GoogleTokenInfoClient;
import com.afl.tracker.adapter.auth.client.GoogleUserInfoClient;
import com.afl.tracker.domain.model.UserInfo;
import com.afl.tracker.domain.model.type.UserOrigin;
import com.afl.tracker.domain.port.AuthPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AuthAdapter implements AuthPort {

  @Inject
    @RestClient
    GoogleTokenInfoClient tokenInfoClient;
    
    @Inject
    @RestClient
    GoogleUserInfoClient userInfoClient;
    
    /**
     * Validates the OAuth access token
     * @param token OAuth access token
     * @throws WebApplicationException if token is invalid
     */
    public void validateToken(String token) {
        GoogleTokenInfoClient.TokenInfo tokenInfo = tokenInfoClient.getTokenInfo(token);
        
        if (tokenInfo.error_description != null) {
            throw new WebApplicationException("Invalid token: " + tokenInfo.error_description, 
                    Response.Status.UNAUTHORIZED);
        }
    }
    
    /**
     * Retrieves user information from OAuth token
     * Tries to get YouTube channel info first, falls back to Google account info
     * @param token OAuth access token
     * @return FirestoreUserInfo with user details
     */
    public UserInfo getUserInfo(String token) {
        // Try YouTube channel info first
        try {
            GoogleUserInfoClient.YouTubeChannelResponse youtubeResponse = 
                    userInfoClient.getYouTubeChannel("Bearer " + token);
            
            if (youtubeResponse.items != null && !youtubeResponse.items.isEmpty()) {
                GoogleUserInfoClient.YouTubeChannel channel = youtubeResponse.items.get(0);
                GoogleUserInfoClient.ChannelSnippet snippet = channel.snippet;
                
                // Get email from Google userinfo
                GoogleUserInfoClient.UserInfo googleUser = userInfoClient.getUserInfo("Bearer " + token);
                
                return new UserInfo(
                    channel.id,
                    snippet.title,
                    googleUser.email,
                    UserOrigin.YOUTUBE
                );
            }
        } catch (Exception e) {
            // Fall back to Google account info
        }
        
        // Fallback to Google account info
        GoogleUserInfoClient.UserInfo googleUser = userInfoClient.getUserInfo("Bearer " + token);
        return new UserInfo(
            googleUser.id,
            googleUser.name,
            googleUser.email,
            UserOrigin.GOOGLE
        );
    }
}
