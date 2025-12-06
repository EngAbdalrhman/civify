package com.app.civify.service.bloomfilter;

import com.app.civify.repository.UserRepository;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;

@Service

public class BloomFilterService {
    private BloomFilter<String> emailFilter;
    private BloomFilter<String> usernameFilter;
    public BloomFilterService(UserRepository userRepository) {

        long estimatedUsers = userRepository.count(); // estimate size

        emailFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                Math.max(estimatedUsers, 1000),  // min size
                0.01                             // false positive probability (1%)
        );

        usernameFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                Math.max(estimatedUsers, 1000),
                0.01
        );

        // Load existing users
        userRepository.findAll().forEach(user -> {
            emailFilter.put(user.getEmail());
            usernameFilter.put(user.getUsername());
        });
    }

    public boolean mightHaveEmail(String email) {
        return emailFilter.mightContain(email);
    }

    public boolean mightHaveUsername(String username) {
        return usernameFilter.mightContain(username);
    }

    public void addEmail(String email) {
        emailFilter.put(email);
    }

    public void addUsername(String username) {
        usernameFilter.put(username);
    }
}
