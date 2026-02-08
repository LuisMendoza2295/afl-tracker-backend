package com.afl.tracker.adapter.auth.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RegisterRestClient(configKey = "google-token-info")
public interface GoogleTokenInfoClient {
    
    @GET
    @Path("/tokeninfo")
    TokenInfo getTokenInfo(@QueryParam("access_token") String accessToken);
    
    class TokenInfo {
        public String user_id;
        public String email;
        public String verified_email;
        public Integer expires_in;
        public String scope;
        public String error_description;
    }
}
