package com.app.civify.dto.payment.paymob;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymobMerchantDto {
    private Long id;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("company_name")
    private String companyName;
    
    private List<String> phones;
    
    @JsonProperty("company_emails")
    private List<String> companyEmails;
}
