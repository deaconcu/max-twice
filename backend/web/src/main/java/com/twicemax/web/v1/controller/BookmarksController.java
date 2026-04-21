package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.response.bookmark.BookmarkDTO;
import com.twicemax.application.service.BookmarkService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.v1.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/bookmarks")
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
    public ApiResponse<List<BookmarkDTO<Object>>> getUserBookmarks(
            @PathVariable String contentType,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser UserDO currentUser) {

        Enums.ContentType type = Enums.ContentType.parse(contentType);
        List<BookmarkDTO<Object>> bookmarks = bookmarkService.getUserBookmarks(currentUser.getId(), type, lastId, limit);
        return ApiResponse.success(bookmarks);
    }
}
