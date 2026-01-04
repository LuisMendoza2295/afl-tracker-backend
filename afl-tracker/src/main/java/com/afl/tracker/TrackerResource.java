package com.afl.tracker;

import com.afl.tracker.service.TrackerService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class TrackerResource {

    @Inject
    TrackerService trackerService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{fileName}")
    public String hello(String fileName) {
        return trackerService.uploadFile(fileName);
    }
}
