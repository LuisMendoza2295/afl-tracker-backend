package com.afl.tracker.web;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import com.afl.tracker.domain.aggregate.TrackerService;
import com.afl.tracker.domain.model.ImageInfo;
import com.afl.tracker.domain.model.VisionInfo;
import com.afl.tracker.domain.model.valueobj.Location;
import com.afl.tracker.web.annotation.Authenticated;
import com.afl.tracker.web.context.AuthContext;
import com.afl.tracker.web.dto.DetectionResultDto;
import com.afl.tracker.web.dto.ImageResponseDto;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class TrackerResource {

    @Inject
    TrackerService trackerService;
    
    @Inject
    AuthContext authContext;

    @GET
    @Path("/images")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImages() {
        List<ImageResponseDto> images = trackerService.getAllImages()
                .stream()
                .map(ImageResponseDto::from)
                .collect(Collectors.toList());
        return Response.ok().entity(images).build();
    }

    @POST
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/images/upload")
    public Response uploadImage(
            @RestForm("file") FileUpload file,
            @RestForm("latitude") String latitude,
            @RestForm("longitude") String longitude) {
        try {
            Location location = new Location(Double.parseDouble(latitude), Double.parseDouble(longitude));
            ImageInfo imageInfo = trackerService.uploadFile(authContext.getUserInfo(), location, file);
            
            return Response.status(Response.Status.CREATED)
                    .entity(ImageResponseDto.from(imageInfo))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
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
}
