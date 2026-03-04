package com.app.civify.controller.v1.admin;

import com.app.civify.controller.annotations.ApiV1Controller;
import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.dto.blog.CreateBlogRequest;
import com.app.civify.security.UserPrincipal;
import com.app.civify.security.annotation.CivifyAuthorization;
import com.app.civify.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@ApiV1Controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/blogs")
public class AdminBlogController {
    private final BlogService blogService;




    @PostMapping("/")
    @CivifyAuthorization(permission = "CREATE.BLOG")
    public BlogDetailsDto create(@RequestBody CreateBlogRequest req ,  @AuthenticationPrincipal UserPrincipal userPrincipal) {

        return blogService.create(req, userPrincipal);
    }

}
