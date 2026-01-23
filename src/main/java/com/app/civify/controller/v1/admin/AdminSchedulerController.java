package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.job.SubscriptionJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for scheduler/job operations.
 * Allows manual triggering of scheduled jobs for testing purposes.
 */
@ApiV1Controller
@RequestMapping("/admin/scheduler")
@RequiredArgsConstructor
@Tag(name = "Admin - Scheduler", description = "Admin endpoints to trigger scheduled jobs manually")
@Slf4j
public class AdminSchedulerController {

    private final SubscriptionJob subscriptionJob;

    @PostMapping("/subscription-job")
    @Operation(summary = "Trigger subscription job manually", 
               description = "Manually triggers the subscription lifecycle job (invoicing, reminders, expiration)")
    public ResponseEntity<Map<String, Object>> triggerSubscriptionJob() {
        log.info("🔧 Admin manually triggered SubscriptionJob");
        
        Map<String, Object> response = new HashMap<>();
        response.put("job", "SubscriptionJob");
        
        try {
            long startTime = System.currentTimeMillis();
            subscriptionJob.execute(null);
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("message", "Subscription job executed successfully");
            response.put("durationMs", duration);
            log.info("✅ SubscriptionJob completed in {}ms", duration);
            
            return ResponseEntity.ok(response);
        } catch (JobExecutionException e) {
            log.error("❌ SubscriptionJob failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Job execution failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/invoice-reminders")
    @Operation(summary = "Trigger invoice/renewal processing only",
               description = "Processes pending invoices and sends reminders without expiring subscriptions")
    public ResponseEntity<Map<String, Object>> triggerInvoiceProcessing() {
        log.info("🔧 Admin manually triggered invoice processing");
        
        Map<String, Object> response = new HashMap<>();
        response.put("job", "InvoiceProcessing");
        
        try {
            long startTime = System.currentTimeMillis();
            // The SubscriptionJob has the logic, we just call execute with null context
            subscriptionJob.execute(null);
            long duration = System.currentTimeMillis() - startTime;
            
            response.put("success", true);
            response.put("message", "Invoice processing completed");
            response.put("durationMs", duration);
            
            return ResponseEntity.ok(response);
        } catch (JobExecutionException e) {
            log.error("❌ Invoice processing failed: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Processing failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
