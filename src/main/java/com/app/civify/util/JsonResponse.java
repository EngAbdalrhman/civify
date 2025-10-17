package com.app.civify.util;

import lombok.Data;

/**
 * @author Abdalrhman Mostafa <amabdo30@gmail.com>
 * @details JsonResponse template
 * @since 1.0.0
 * @version 1.0.0
 * @date 2025-10-16
 *
 */
@Data
public class JsonResponse {

    private String status;
    private Object data;

    public JsonResponse(String status, Object data) {
        this.status = status;
        this.data = data;
    }
}
