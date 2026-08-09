package com.example.pong.controller

import com.example.common.model.PingPongMessage
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Subject

/**
 * PongController 单元测试
 */
class PongControllerSpec extends Specification {

    @Subject
    PongController controller = new PongController()

    def "handlePing应返回Pong响应"() {
        given: "一个 Ping 请求消息"
        def request = new PingPongMessage("PING", "Hello", "ping-service-1")
        request.setMessageId("test-msg-001")

        when: "调用 handlePing"
        def responseMono = controller.handlePing(request)
        def responseEntity = responseMono.block()

        then: "应返回 200 和 Pong 消息"
        responseEntity.statusCode == HttpStatus.OK
        responseEntity.body.type == "PONG"
        responseEntity.body.payload == "World"
        responseEntity.body.source == "pong-service"
        responseEntity.body.messageId == "test-msg-001"
    }

    def "handlePing应保留原始messageId"() {
        given:
        def request = new PingPongMessage("PING", "Hello", "ping-service")
        request.setMessageId("original-id-123")

        when:
        def responseEntity = controller.handlePing(request).block()

        then:
        responseEntity.body.messageId == "original-id-123"
    }

    def "handlePing响应payload应为World"() {
        given:
        def request = new PingPongMessage("PING", "Hello", "any-source")

        when:
        def responseEntity = controller.handlePing(request).block()

        then:
        responseEntity.body.payload == "World"
    }

    def "health应返回服务状态"() {
        when:
        def responseEntity = controller.health().block()

        then:
        responseEntity.statusCode == HttpStatus.OK
        responseEntity.body.contains("PONG service is UP")
    }
}
