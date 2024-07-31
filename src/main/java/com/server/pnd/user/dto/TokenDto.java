package com.server.pnd.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
@Builder
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;
}
