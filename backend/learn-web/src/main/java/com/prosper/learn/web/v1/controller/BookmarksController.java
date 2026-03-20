package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.service.BookmarkService;
import com.prosper.learn.interaction.bookmark.BookmarkDO;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarksController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{contentType}/{contentId}")
    @SaCheckLogin
    public ApiResponse<Boolean> toggleBookmark(
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @CurrentUser UserDO currentUser) {

        Enums.ContentType type = Enums.ContentType.parse(contentType);
        boolean bookmarked = bookmarkService.toggleBookmark(currentUser.getId(), contentId, type);
        return ApiResponse.success(bookmarked);
    }

    @GetMapping("/{contentType}/list")
    @SaCheckLogin
    public ApiResponse<List<BookmarkDO>> getUserBookmarks(
            @PathVariable String contentType,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser UserDO currentUser) {

        Enums.ContentType type = Enums.ContentType.parse(contentType);
        List<BookmarkDO> bookmarks = bookmarkService.getUserBookmarks(currentUser.getId(), type, lastId, limit);
        return ApiResponse.success(bookmarks);
    }
}
