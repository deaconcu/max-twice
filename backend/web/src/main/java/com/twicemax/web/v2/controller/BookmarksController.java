package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.response.bookmark.BookmarkDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.BookmarkService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.v2.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
public class BookmarksController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{contentType}/{contentId}")
    @SaCheckLogin
    public Map<String, Boolean> toggleBookmark(
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @CurrentUser UserDO currentUser) {

        Enums.ContentType type = Enums.ContentType.parse(contentType);
        boolean bookmarked = bookmarkService.toggleBookmark(currentUser.getId(), contentId, type);
        return Map.of("bookmarked", bookmarked);
    }

    @GetMapping("/{contentType}/list")
    @SaCheckLogin
    public CursorPage<BookmarkDTO<Object>> getUserBookmarks(
            @PathVariable String contentType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser UserDO currentUser) {

        Enums.ContentType type = Enums.ContentType.parse(contentType);
        return bookmarkService.getUserBookmarks(currentUser.getId(), type, cursor, limit);
    }
}
