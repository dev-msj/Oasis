package com.flab.oasis.controller;

import com.flab.oasis.model.FeedUpdateRequest;
import com.flab.oasis.model.FeedWriteRequest;
import com.flab.oasis.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller("/api/feeds")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @PostMapping("/{uid}")
    public void writeFeedByFeedWriteRequest(@PathVariable String uid, @RequestBody FeedWriteRequest feedWriteRequest) {
        feedWriteRequest.setUid(uid);

        feedService.writeFeedByFeedWriteRequest(feedWriteRequest);
    }

    @PatchMapping("/{uid}/{feed_id}")
    public void writeFeedByFeedWriteRequest(@PathVariable String uid, @PathVariable("feed_id") String feedId,
                                            @RequestBody FeedUpdateRequest feedUpdateRequest) {
        feedUpdateRequest.setUid(uid);
        feedUpdateRequest.setFeedId(feedId);

        feedService.updateFeedByFeedUpdateRequest(feedUpdateRequest);
    }
}
