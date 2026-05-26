package com.techhub.service;

import com.techhub.dto.auth.LoginRequest;
import com.techhub.dto.auth.LoginResponse;
import com.techhub.dto.auth.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
