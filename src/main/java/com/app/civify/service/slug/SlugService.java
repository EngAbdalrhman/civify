package com.app.civify.service.slug;

import org.springframework.stereotype.Service;

@Service
public class SlugService {
    public String slugify(String input) {
        if (input == null) return "post";
        String s = input.trim().toLowerCase();

        s = s.replaceAll("[^a-z0-9]+", "-");

        s = s.replaceAll("^-+|-+$", "");

        return s.isBlank() ? "post" : s;
    }

    public String uniqueSlug(String baseSlug, java.util.function.Predicate<String> exists) {
        String slug = baseSlug;
        int i = 2;
        while (exists.test(slug)) {
            slug = baseSlug + "-" + i;
            i++;
        }
        return slug;
    }
}
