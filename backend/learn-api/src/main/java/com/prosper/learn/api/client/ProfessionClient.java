package com.prosper.learn.api.client;

import com.prosper.learn.dto.response.ProfessionDTO;
import com.prosper.learn.dto.response.Response;
import org.springframework.web.bind.annotation.*;

public interface ProfessionClient {

    //@GetMapping(value = "/profession/list", params = "page")
    Response<Object> listByPage(@RequestParam(value = "page", defaultValue = "1") Integer page);

    //@GetMapping(value = "/profession/list", params = {"state"})
    Response<Object> listByStateAndLastId(
            @RequestParam Byte state, @RequestParam(value = "lastId", defaultValue = "0") Long lastId);

    //@GetMapping(value = "/profession/list", params = {"mainCategory"})
    Response<Object> listByMainCategoryAndLastId(
            @RequestParam Integer mainCategory, @RequestParam(value = "lastId", defaultValue = "0") Long lastId);

    //@GetMapping(value = "/profession/list", params = {"mainCategory", "subCategory"})
    Response<Object> listByMainCategoryAndSubCategoryAndLastId(
            @RequestParam Integer mainCategory, @RequestParam Integer subCategory,
            @RequestParam(value = "lastId", defaultValue = "0") Long lastId);

    //@GetMapping("/profession/list/approved")
    Response<Object> listApproved(@RequestParam(value = "lastId", defaultValue = "0") Long lastId);

    //@GetMapping("/profession")
    Response<Object> getById(@RequestParam Long id);

    //@PostMapping("/profession")
    Response<Object> create(@RequestBody ProfessionDTO professionDTO);

    //@PutMapping("/profession")
    Response<Object> update(@RequestBody ProfessionDTO professionDTO);

    //@PostMapping("/profession/operate")
    Response<Object> operate(
            @RequestParam Long id, @RequestParam String action, @RequestParam(required = false) String rejectedReason);

    // @DeleteMapping("/profession")
    Response<Object> delete(@RequestParam Long id);

    // 获取热门职业（按学习人数排行）
    //@GetMapping("/profession/hot")
    Response<Object> getHotProfessions(@RequestParam(value = "limit", defaultValue = "10") Integer limit);
}
