package com.app.civify.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@ApiV1Controller
@RequestMapping("/cvs")
public class CvController {

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestPart("file") MultipartFile file) {
        // stub
        return ResponseEntity.ok("uploaded");
    }

    @PostMapping("/{id}/tailor")
    public ResponseEntity<String> tailor(@PathVariable Long id, @RequestBody String jobDescription) {
        // stub
        return ResponseEntity.ok("tailored");
    }
}
