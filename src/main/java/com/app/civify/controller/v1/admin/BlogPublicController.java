package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiV1Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/blogs")
public class BlogPublicController {
    private final BlogService blogService;

    @GetMapping("/")
    public List<BlogListItemDto> list() {
        return blogService.list();
    }

    @GetMapping("/{slug}")
    public BlogDetailsDto getBySlug(@PathVariable String slug) {
        return blogService.getBySlug(slug);
    }


}
