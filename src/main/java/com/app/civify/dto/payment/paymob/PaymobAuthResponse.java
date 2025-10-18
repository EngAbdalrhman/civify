package com.app.civify.dto.payment.paymob;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PaymobAuthResponse {
    private String token;
    private Profile profile;

    @Data
    public static class Profile {
        private Long id;
        private String email;
    }
}
