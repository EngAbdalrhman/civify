package com.app.civify.service.blog;

import com.app.civify.dto.blog.BlogDetailsDto;
import com.app.civify.dto.blog.BlogListItemDto;
import com.app.civify.dto.blog.CreateBlogRequest;
import com.app.civify.entity.blog.Blog;
import com.app.civify.exception.ResourceAlreadyExistsException;
import com.app.civify.exception.ResourceNotFoundException;
import com.app.civify.repository.blog.BlogRepository;
import com.app.civify.security.UserPrincipal;
import com.app.civify.service.slug.SlugService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;
    private final SlugService slugService;

    @Transactional
    public BlogDetailsDto create(CreateBlogRequest req, UserPrincipal author) {
        String base = slugService.slugify(req.title());
        String slug ;
        if (req.slug()!=null && !req.slug().isEmpty()){
            if (blogRepository.existsBySlug(req.slug()))
                throw new ResourceAlreadyExistsException("slug already exist");
            slug = req.slug();


        }else {
            slug =  slugService.uniqueSlug(base, blogRepository::existsBySlug);
        }

        Blog blog = Blog.builder()
                .title(req.title())
                .slug(slug)
                .content(req.content())
                .author(author.getUser())
                .build();

        Blog saved = blogRepository.save(blog);

        return new BlogDetailsDto(saved.getId(), saved.getTitle(), saved.getSlug(), saved.getContent());
    }

    public List<BlogListItemDto> list() {
        return blogRepository.findAll().stream()
                .map(b -> new BlogListItemDto(b.getId(), b.getTitle(), b.getSlug()))
                .toList();
    }

    public BlogDetailsDto getBySlug(String slug) {
        Blog b = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));

        return new BlogDetailsDto(b.getId(), b.getTitle(), b.getSlug(), b.getContent());
    }
}
