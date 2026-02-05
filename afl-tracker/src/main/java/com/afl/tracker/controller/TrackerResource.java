package com.afl.tracker.controller;

import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.afl.tracker.controller.dto.DetectionResultDto;
import com.afl.tracker.domain.model.VisionInfo;
import com.afl.tracker.service.TrackerService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class TrackerResource {

    @Inject
    TrackerService trackerService;

    @GET
    @Path("/images")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImages() {
        List<String> images = trackerService.getImages();
        return Response.ok().entity(images).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/images/upload")
    public Response hello(@RestForm("file") FileUpload file) {
        try {
            BlobInfo blobInfo = trackerService.uploadFile(file);
            return Response.status(Response.Status.CREATED)
                    .entity("File uploaded successfully: " + blobInfo.getName())
                    .build();
        } catch (IllegalArgumentException e) {
            // Logo validation failed
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            // Other errors
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("File upload failed: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/images/validate")
    public Response validateLogo(@RestForm("file") FileUpload file) {
        VisionInfo visionInfo = trackerService.getVisionInfo(file);
        DetectionResultDto result = DetectionResultDto.from(visionInfo, file.fileName());
        return Response.ok().entity(result).build();
    }

    @GET
    @Path("/images/download/{uuid}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("uuid") String uuid) {
        Blob blob = trackerService.downloadFile(uuid);
        if (blob == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("File not found: " + uuid).build();
        }
        return Response.ok(blob.getContent())
                .header("Content-Disposition", "attachment; filename=\"" + blob.getName() + "\"")
                .build();
    }
}
