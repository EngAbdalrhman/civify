package com.app.civify.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ApiV1Controller
@RequestMapping("/payments")
public class PaymentController {

    @PostMapping("/create")
    public ResponseEntity<String> create() {
        return ResponseEntity.ok("created");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload) {
        return ResponseEntity.ok("ok");
    }
}
