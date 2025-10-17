package com.app.civify.service.ai;

import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    String parseCv(MultipartFile file);

    String tailorCv(Long cvId, String jobDescription);

    String callModel(String prompt, String provider, String model);
}
