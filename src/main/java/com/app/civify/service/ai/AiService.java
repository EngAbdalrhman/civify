package com.app.civify.service.ai;

import com.app.civify.dto.ai.AiModelResponse;

public interface AiService {




    AiModelResponse callModel(String prompt, String provider, String model);
}
