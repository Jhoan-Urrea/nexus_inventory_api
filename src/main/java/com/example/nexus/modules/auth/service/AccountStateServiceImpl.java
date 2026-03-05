package com.example.nexus.modules.auth.service;

import com.example.nexus.modules.auth.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AccountStateServiceImpl implements AccountStateService {

    @Override
    public void assertCanAuthenticate(UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (!userDetails.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("Account is locked");
        }

        if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException("Account is expired");
        }

        if (!userDetails.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials are expired");
        }
    }
}
