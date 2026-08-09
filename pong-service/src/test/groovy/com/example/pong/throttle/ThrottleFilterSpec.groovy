package com.example.pong.throttle

import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

/**
 * ThrottleFilter 单元测试
 */
class ThrottleFilterSpec extends Specification {

    @Subject
    ThrottleFilter filter = new ThrottleFilter()

    WebFilterChain chain = Mock(WebFilterChain)

    def "第一个请求应通过节流控制"() {
        given: "一个新的节流过滤器"
        def exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/pong"))

        and: "chain 返回空"
        chain.filter(_) >> Mono.empty()

        when: "第一个请求"
        filter.filter(exchange, chain).block()

        then: "应该通过"
        exchange.response.statusCode != HttpStatus.TOO_MANY_REQUESTS
        filter.currentCount == 1
    }

    def "同一秒内的第二个请求应被限流"() {
        given:
        def exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/pong"))
        def exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/pong"))

        and:
        chain.filter(_) >> Mono.empty()

        when: "第一个请求通过"
        filter.filter(exchange1, chain).block()

        and: "第二个请求被限流"
        filter.filter(exchange2, chain).block()

        then:
        exchange1.response.statusCode != HttpStatus.TOO_MANY_REQUESTS
        exchange2.response.statusCode == HttpStatus.TOO_MANY_REQUESTS
    }

    def "非 /api/pong 路径不应被限流"() {
        given:
        def exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/health"))

        and:
        chain.filter(_) >> Mono.empty()

        when: "发送多个请求到非限流路径"
        (1..5).each {
            filter.filter(exchange, chain).block()
        }

        then: "都不应被限流"
        exchange.response.statusCode != HttpStatus.TOO_MANY_REQUESTS
    }

    def "getThrottleLimit应返回1"() {
        expect:
        filter.throttleLimit == 1
    }

    def "getCurrentSecond应返回当前秒的时间戳"() {
        given:
        def expectedSecond = System.currentTimeMillis() / 1000

        when:
        def currentSecond = filter.currentSecond

        then:
        Math.abs(currentSecond - expectedSecond) <= 1
    }

    def "限流后响应状态码应为429"() {
        given:
        def exchange1 = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/pong"))
        def exchange2 = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/pong"))

        chain.filter(_) >> Mono.empty()

        when:
        filter.filter(exchange1, chain).block()
        filter.filter(exchange2, chain).block()

        then:
        exchange2.response.statusCode == HttpStatus.TOO_MANY_REQUESTS
        exchange2.response.statusCode.value() == 429
    }
}
