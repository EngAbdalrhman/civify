package com.app.civify.service.ai;

public interface AiService {


    String callModel(String prompt, String provider, String model);
}
