package com.prosper.learn.api.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "learn-service", contextId = "upvote")
public interface UpvoteClient {


}
