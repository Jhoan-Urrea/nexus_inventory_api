package com.example.nexus.modules.auth.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface AccountStateService {

    void assertCanAuthenticate(UserDetails userDetails);
}
