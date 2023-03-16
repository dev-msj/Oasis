package com.flab.oasis.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.oasis.constant.ErrorCode;
import com.flab.oasis.mapper.UserAuthMapper;
import com.flab.oasis.model.UserSession;
import com.flab.oasis.model.dao.UserAuthDAO;
import com.flab.oasis.model.exception.AuthorizationException;
import com.flab.oasis.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAuthRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserAuthMapper userAuthMapper;
    private final ObjectMapper objectMapper;

    public UserAuthDAO getUserAuthDAOByUid(String uid) {
        return Optional.ofNullable(userAuthMapper.getUserAuthDAOByUid(uid))
                .orElseThrow(() -> new AuthorizationException(
                        ErrorCode.UNAUTHORIZED, "User does not exist.", uid
                ));
    }

    public UserSession getUserSessionByUid(String uid) {
        try {
            // redis에 없으면 DB에서 가져온다.
            UserSession userSession = objectMapper.convertValue(
                    Optional.ofNullable(
                            redisTemplate.opsForValue().get(makeKey(uid))
                    ).orElse(
                            getUserAuthDAOByUid(uid).parseUserSession()
                    ),
                    new TypeReference<UserSession>() {}
            );

            redisTemplate.opsForValue().set(makeKey(userSession.getUid()), userSession);

            return userSession;
        } catch (Exception e) {
            LogUtils.error(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage());

            // redis에 문제가 생겨 연결할 수 없을 경우 DB에서 직접 가져온다.
            return getUserAuthDAOByUid(uid).parseUserSession();
        }
    }

    public void updateRefreshToken(UserSession userSession) {
        try {
            redisTemplate.opsForValue().getAndDelete(makeKey(userSession.getUid()));
        } catch (Exception e) {
            LogUtils.error(ErrorCode.SERVICE_UNAVAILABLE, e.getMessage());
        }

        userAuthMapper.updateRefreshToken(userSession);
    }

    private String makeKey(String uid) {
        return String.format("UserSession::%s", uid);
    }
}