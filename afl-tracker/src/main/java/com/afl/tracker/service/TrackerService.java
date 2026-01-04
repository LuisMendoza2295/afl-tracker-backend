package com.afl.tracker.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrackerService {

    public String uploadFile(String file) {
        return "File uploaded: " + file;
    }
}
