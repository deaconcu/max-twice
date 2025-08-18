package com.prosper.learn.api.client;

import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.dto.Response;
import org.springframework.web.bind.annotation.*;

public interface ProfessionClient {

    @GetMapping(value = "/profession/list", params = "page")
    Response<Object> listByPage(@RequestParam(value = "page", defaultValue = "1") int page);

    @GetMapping(value = "/profession/list", params = {"state"})
    Response<Object> listByStateAndLastId(@RequestParam String state, @RequestParam(value = "lastId", defaultValue = "0") int lastId);

    @GetMapping(value = "/profession/list", params = {"mainCategory"})
    Response<Object> listByMainCategoryAndLastId(@RequestParam int mainCategory, @RequestParam(value = "lastId", defaultValue = "0") int lastId);

    @GetMapping(value = "/profession/list", params = {"mainCategory", "subCategory"})
    Response<Object> listByMainCategoryAndSubCategoryAndLastId(@RequestParam int mainCategory, @RequestParam int subCategory, @RequestParam(value = "lastId", defaultValue = "0") int lastId);

    @GetMapping("/profession/list/approved")
    Response<Object> listApproved(@RequestParam(value = "lastId", defaultValue = "0") int lastId);

    @GetMapping("/profession")
    Response<Object> getById(@RequestParam int id);

    @PostMapping("/profession")
    Response<Object> create(@RequestBody ProfessionDTO professionDTO);

    @PutMapping("/profession")
    Response<Object> update(@RequestBody ProfessionDTO professionDTO);

    @PostMapping("/profession/operate")
    Response<Object> operate(@RequestParam int id, @RequestParam String action, @RequestParam(required = false) String rejectedReason);

    @DeleteMapping("/profession")
    Response<Object> delete(@RequestParam int id);
}
