package com.afl.tracker.domain.port;

import com.afl.tracker.domain.model.UserInfo;

public interface AuthPort {

  void validateToken(String accessToken);
  
  UserInfo getUserInfo(String accessToken);

}