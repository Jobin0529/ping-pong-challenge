package com.example.ping.service

import com.example.common.model.PingPongMessage
import com.example.common.model.RequestResult
import com.example.common.ratelimit.FileLockRateLimiter
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

/**
 * PingService 单元测试
 */
class PingServiceSpec extends Specification {

    WebClient webClient = Mock(WebClient)
    FileLockRateLimiter rateLimiter = Mock(FileLockRateLimiter)

    @Subject
    PingService pingService = new PingService(webClient, rateLimiter)

    def "当速率限制拒绝时应返回RATE_LIMITED_LOCALLY"() {
        given:
        rateLimiter.tryAcquire() >> false

        when:
        def result = pingService.sendPing()

        then:
        result == RequestResult.RATE_LIMITED_LOCALLY
    }

    def "当速率限制通过且Pong正常响应时应返回SENT_AND_RESPONDED"() {
        given: "速率限制通过"
        rateLimiter.tryAcquire() >> true

        and: "模拟 WebClient 链式调用"
        def requestBodySpec = Mock(WebClient.RequestBodySpec)
        def requestHeadersSpec = Mock(WebClient.RequestHeadersSpec)
        def responseSpec = Mock(WebClient.ResponseSpec)

        webClient.post() >> requestBodySpec
        requestBodySpec.uri(_) >> requestBodySpec
        requestBodySpec.bodyValue(_) >> requestBodySpec
        requestBodySpec.retrieve() >> responseSpec
        responseSpec.onStatus(_, _) >> Mono.empty()
        responseSpec.bodyToMono(PingPongMessage) >> Mono.just(
                new PingPongMessage("PONG", "World", "pong-service"))

        when:
        def result = pingService.sendPing()

        then:
        result == RequestResult.SENT_AND_RESPONDED
    }

    def "ThrottledException应正确设置消息"() {
        when:
        def exception = new PingService.ThrottledException("test error")

        then:
        exception.message == "test error"
    }

    def "ThrottledException应是RuntimeException的子类"() {
        when:
        def exception = new PingService.ThrottledException("test")

        then:
        exception instanceof RuntimeException
    }
}
