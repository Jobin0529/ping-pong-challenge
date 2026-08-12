package com.example.pong.controller

import com.example.common.model.PingPongMessage
import spock.lang.Specification
import spock.lang.Subject

class PongControllerSpec extends Specification {

    @Subject
    PongController controller = new PongController()

    def "handlePing应返回Pong响应"() {
        given: "一个 Ping 请求消息"
        def request = new PingPongMessage("PING", "Hello", "ping-service-1")
        request.setMessageId("test-msg-001")

        when: "调用 handlePing"
        def responseMono = controller.handlePing(request)
        def response = responseMono.block()   // 直接拿到 PingPongMessage 对象

        then: "验证响应字段"
        response.type == "PONG"
        response.payload == "World"
        response.messageId == "test-msg-001"
        // 注意：source 字段是 PingPongMessage 的属性，不是 type
    }

    def "handlePing应保留原始messageId"() {
        given:
        def request = new PingPongMessage("PING", "Hello", "ping-service")
        request.setMessageId("original-id-123")

        when:
        def response = controller.handlePing(request).block()

        then:
        response.messageId == "original-id-123"
    }

    def "handlePing响应payload应为World"() {
        given:
        def request = new PingPongMessage("PING", "Hello", "any-source")

        when:
        def response = controller.handlePing(request).block()

        then:
        response.payload == "World"
    }

}