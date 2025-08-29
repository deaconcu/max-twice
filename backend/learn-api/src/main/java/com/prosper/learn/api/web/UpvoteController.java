package com.prosper.learn.api.web;

import com.prosper.learn.api.client.UpvoteClient;
import com.prosper.learn.common.Enums;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.service.UpvoteService;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class UpvoteController implements UpvoteClient {


}
