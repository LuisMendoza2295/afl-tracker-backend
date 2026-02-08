package com.afl.tracker.adapter.auth.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@RegisterRestClient(configKey = "google-user-info")
public interface GoogleUserInfoClient {
    
    @GET
    @Path("/oauth2/v2/userinfo")
    UserInfo getUserInfo(@HeaderParam("Authorization") String authorization);
    
    @GET
    @Path("/youtube/v3/channels")
    YouTubeChannelResponse getYouTubeChannel(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("part") String part,
            @QueryParam("mine") boolean mine);
    
    default YouTubeChannelResponse getYouTubeChannel(String authorization) {
        return getYouTubeChannel(authorization, "snippet", true);
    }
    
    class UserInfo {
        public String id;
        public String name;
        public String email;
        public String picture;
    }
    
    class YouTubeChannelResponse {
        public List<YouTubeChannel> items;
    }
    
    class YouTubeChannel {
        public String id;
        public ChannelSnippet snippet;
    }
    
    class ChannelSnippet {
        public String title;
        public Thumbnails thumbnails;
    }
    
    class Thumbnails {
        public Thumbnail defaultThumbnail;
        
        public static class Thumbnail {
            public String url;
        }
    }
}
