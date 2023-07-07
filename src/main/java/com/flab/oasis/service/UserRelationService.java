package com.flab.oasis.service;

import com.flab.oasis.constant.BookCategory;
import com.flab.oasis.mapper.user.FeedMapper;
import com.flab.oasis.model.Feed;
import com.flab.oasis.model.RecommendUser;
import com.flab.oasis.model.UserCategory;
import com.flab.oasis.model.UserRelation;
import com.flab.oasis.repository.UserCategoryRepository;
import com.flab.oasis.repository.UserInfoRepository;
import com.flab.oasis.repository.UserRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRelationService {
    private final UserCategoryRepository userCategoryRepository;
    private final UserRelationRepository userRelationRepository;
    private final UserInfoRepository userInfoRepository;
    private final FeedMapper feedMapper;

    private static final int CHECK_SIZE = 30;

    @Cacheable(cacheNames = "recommendUserCache", key = "#uid", cacheManager = "redisCacheManager")
    public List<String> getRecommendUserListByUid(String uid) {
        Set<String> excludeUidSet = userRelationRepository.getUserRelationListByUid(uid).stream()
                .map(UserRelation::getRelationUser)
                .collect(Collectors.toSet());
        excludeUidSet.add(uid);

        // category가 겹치는 user를 가져온다.
        List<String> overlappingCategoryUidList = userCategoryRepository.getUidListIfOverlappingBookCategory(uid);

        // exclude uid에 해당하는 user를 제외한다.
        overlappingCategoryUidList = overlappingCategoryUidList.stream()
                .filter(s -> !excludeUidSet.contains(s))
                .collect(Collectors.toList());

        // overlappingCategoryUidList에 해당하는 UserCategory를 가져온다.
        List<UserCategory> overlappingUserCategoryList = userCategoryRepository
                .getUserCategoryListByUidList(overlappingCategoryUidList);

        if (overlappingUserCategoryList.isEmpty()) {
            return getDefaultRecommendUserExcludeUidList(uid, excludeUidSet);
        }

        List<String> recommendUserList = makeRecommendUserListByCategory(uid, overlappingUserCategoryList);

        // 카테고리 추천 유저가 30명이 안될 경우, 기본 추천 유저를 가져온다.
        if (recommendUserList.size() < CHECK_SIZE) {
            excludeUidSet.addAll(recommendUserList);

            recommendUserList.addAll(
                    getDefaultRecommendUserExcludeUidList(uid, excludeUidSet)
            );
        }

        return recommendUserList;
    }

    @CachePut(cacheNames = "UserRelation", key = "#userRelation.uid", cacheManager = "redisCacheManager")
    public List<UserRelation> createUserRelation(UserRelation userRelation) {
        userRelationRepository.createUserRelation(userRelation);

        List<UserRelation> userRelationList = userRelationRepository.getUserRelationListByUid(userRelation.getUid());
        userRelationList.add(userRelation);

        return userRelationList;
    }

    private List<String> getDefaultRecommendUserExcludeUidList(String uid, Set<String> excludeUidSet) {
        // 6개월 안에 가입한 default recommend user를 최대 30명 가져와서 exclude uid를 제외한 유저를 반환한다.
        return userInfoRepository.getDefaultRecommendUserList(uid).stream()
                .filter(s -> !excludeUidSet.contains(s))
                .collect(Collectors.toList());
    }

    private List<String> makeRecommendUserListByCategory(String uid, List<UserCategory> overlappingCategoryUserList) {
        Set<BookCategory> userCategorySet = new HashSet<>(userCategoryRepository.getUserCategoryListByUid(uid))
                .stream()
                .map(UserCategory::getBookCategory)
                .collect(Collectors.toSet());

        // 겹치는 카테고리를 제외한 카테고리의 개수 카운트
        Map<String, Long> userCategoryCountMap = overlappingCategoryUserList.stream()
                .filter(userCategory -> !userCategorySet.contains(userCategory.getBookCategory()))
                .map(UserCategory::getUid)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // feed count는 0으로 두고, category count로 recommend user 생성
        Map<String, RecommendUser> recommendUserMap = userCategoryCountMap.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> RecommendUser.builder()
                                        .uid(entry.getKey())
                                        .categoryCount(entry.getValue())
                                        .feedCount(0)
                                        .build()
                        )
                );

        List<String> uidList = overlappingCategoryUserList.stream()
                .map(UserCategory::getUid)
                .distinct()
                .collect(Collectors.toList());

        // 작성한 feed가 존재할 경우, 해당 feed의 개수를 count하여 recommend user에 set
        feedMapper.getFeedListByUidList(uidList).stream()
                .map(Feed::getUid)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .forEach((key, value) -> recommendUserMap.get(key).setFeedCount(value));

        return new ArrayList<>(recommendUserMap.values()).stream()
                .sorted()
                .map(RecommendUser::getUid)
                .collect(Collectors.toList());
    }
}
