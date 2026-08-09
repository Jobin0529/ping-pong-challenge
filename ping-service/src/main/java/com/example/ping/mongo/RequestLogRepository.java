package com.example.ping.mongo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestLogRepository extends MongoRepository<RequestLog, String> {

    // 按 messageId 查询
    List<RequestLog> findByMessageId(String messageId);

    // 按结果类型查询
    List<RequestLog> findByResult(String result);

    // 按服务名查询
    List<RequestLog> findByServiceName(String serviceName);

    // 按实例 ID 查询
    List<RequestLog> findByInstanceId(String instanceId);

    // 按时间范围查询
    List<RequestLog> findByTimestampBetween(Long startTime, Long endTime);

    // 按是否被限流查询
    List<RequestLog> findByRateLimited(Boolean rateLimited);

    // 查询最近的日志
    @Query("{ 'serviceName' : ?0 }")
    List<RequestLog> findTop100ByServiceNameOrderByTimestampDesc(String serviceName);

    // 统计指定时间范围内的请求数量
    long countByTimestampBetween(Long startTime, Long endTime);

    // 统计指定时间范围内成功的请求数量
    long countByResultAndTimestampBetween(String result, Long startTime, Long endTime);

    // 统计指定时间范围内被限流的请求数量
    long countByRateLimitedAndTimestampBetween(Boolean rateLimited, Long startTime, Long endTime);
}
