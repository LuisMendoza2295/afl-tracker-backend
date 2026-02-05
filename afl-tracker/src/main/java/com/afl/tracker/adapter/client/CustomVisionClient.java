package com.afl.tracker.adapter.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.afl.tracker.adapter.dto.CustomVisionResponseDto;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "custom-vision")
public interface CustomVisionClient {

    @POST
    @Path("/customvision/v3.0/Prediction/{projectId}/detect/iterations/{iterationName}/image")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    CustomVisionResponseDto detectImage(
            @PathParam("projectId") String projectId,
            @PathParam("iterationName") String iterationName,
            @HeaderParam("Prediction-Key") String predictionKey,
            byte[] imageData);
}
