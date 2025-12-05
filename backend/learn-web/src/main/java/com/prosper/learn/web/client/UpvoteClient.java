package com.prosper.learn.web.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "learn-service", contextId = "upvote")
public interface UpvoteClient {


}
