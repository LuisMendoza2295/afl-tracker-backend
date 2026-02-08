package com.afl.tracker.domain.model;

import com.afl.tracker.domain.model.type.UserOrigin;

public record UserInfo(String id, String name, String email, UserOrigin origin) {

  public boolean isOrigin(UserOrigin origin) {
    return this.origin == origin;
  }

}
