package com.example.emp.mapper;

import com.example.emp.model.PushSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PushSubscriptionMapper {

    List<PushSubscription> findAll();

    void upsert(PushSubscription sub);

    void deleteByEndpoint(@Param("endpoint") String endpoint);

    // 카카오 미연동 사원의 push 구독 목록
    List<PushSubscription> findUnconnectedToKakao();
}
