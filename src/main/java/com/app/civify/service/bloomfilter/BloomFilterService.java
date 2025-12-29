package com.app.civify.service.bloomfilter;

import com.app.civify.repository.UserRepository;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BloomFilterService {

    private final UserRepository userRepository;

    // Initialize with a default empty filter to avoid NullPointerExceptions
    // if accessed before the DB load completes.
    private BloomFilter<String> emailFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 1000, 0.01);

    private BloomFilter<String> usernameFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8), 1000, 0.01);

    private BloomFilter<Long> idFilter = BloomFilter.create(
            Funnels.longFunnel(), 1000, 0.01);


    /**
     * This runs AFTER the application context is fully ready and
     * Hibernate has finished creating the table columns.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        System.out.println("Initializing Bloom Filters from database...");

        long estimatedUsers = userRepository.count();

        // Re-create filters with better size estimates based on DB count
        emailFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                Math.max(estimatedUsers, 1000),  // ensure min size is 1000
                0.01                             // 1% false positive rate
        );

        usernameFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                Math.max(estimatedUsers, 1000),
                0.01
        );

        idFilter = BloomFilter.create(
                Funnels.longFunnel(),
                estimatedUsers,
                0.01
        );

        // Load existing users safely
        userRepository.findAll().forEach(user -> {
            if (user.getEmail() != null) {
                emailFilter.put(user.getEmail());
            }
            if (user.getUsername() != null) {
                usernameFilter.put(user.getUsername());
            }
            if (user.getId() != null) {
                idFilter.put(user.getId());
            }
        });

        System.out.println("Bloom Filters initialized successfully.");
    }

    public boolean mightHaveEmail(String email) {
        if (email == null) return false;
        return emailFilter.mightContain(email);
    }

    public boolean mightHaveUsername(String username) {
        if (username == null) return false;
        return usernameFilter.mightContain(username);
    }

    public boolean mightHaveId(Long id) {
        if (id == null) return false;
        return idFilter.mightContain(id);
    }


    public void addId(Long id) {
        if (id != null) idFilter.put(id);
    }

    public void addEmail(String email) {
        if (email != null) emailFilter.put(email);
    }

    public void addUsername(String username) {
        if (username != null) usernameFilter.put(username);
    }
}