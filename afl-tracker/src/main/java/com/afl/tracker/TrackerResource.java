package com.afl.tracker;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.afl.tracker.service.TrackerService;
import com.google.cloud.storage.BlobInfo;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class TrackerResource {

    @Inject
    TrackerService trackerService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/upload")
    public Response hello(@RestForm("file") FileUpload file) {
        BlobInfo blobInfo = trackerService.uploadFile(file);
        return Response.ok("File uploaded successfully: " + blobInfo.getName()).build();
    }
}
