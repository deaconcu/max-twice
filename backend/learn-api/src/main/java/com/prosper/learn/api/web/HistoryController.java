package com.prosper.learn.api.web;

import com.prosper.learn.api.client.HistoryClient;
import com.prosper.learn.dto.HistoryDTO;
import com.prosper.learn.persistence.mapper.HistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class HistoryController implements HistoryClient {

    private final HistoryMapper historyMapper;

    @Override
    public List<HistoryDTO> listByPostingId(int id, int page, int pageSize) {
        // todo
        return null;
    }

    @Override
    public void prove(int id) {
        // todo

    }
}
