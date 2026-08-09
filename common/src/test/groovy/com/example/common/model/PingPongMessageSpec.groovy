package com.example.common.model

import spock.lang.Specification
import spock.lang.Subject

/**
 * PingPongMessage 单元测试
 */
class PingPongMessageSpec extends Specification {

    def "使用构造函数创建消息应正确设置属性"() {
        when:
        def message = new PingPongMessage("PING", "Hello", "ping-service-1")

        then:
        message.type == "PING"
        message.payload == "Hello"
        message.source == "ping-service-1"
        message.timestamp > 0
        message.messageId != null
        !message.messageId.isEmpty()
    }

    def "默认构造函数应创建空消息"() {
        when:
        def message = new PingPongMessage()

        then:
        message.type == null
        message.payload == null
        message.source == null
    }

    def "setter方法应正确设置属性"() {
        given:
        def message = new PingPongMessage()

        when:
        message.setType("PONG")
        message.setPayload("World")
        message.setSource("pong-service")
        message.setTimestamp(1234567890L)
        message.setMessageId("test-id-001")

        then:
        message.getType() == "PONG"
        message.getPayload() == "World"
        message.getSource() == "pong-service"
        message.getTimestamp() == 1234567890L
        message.getMessageId() == "test-id-001"
    }

    def "toString应包含关键信息"() {
        given:
        def message = new PingPongMessage("PING", "Hello", "ping-service")
        message.setMessageId("msg-001")

        when:
        def str = message.toString()

        then:
        str.contains("PING")
        str.contains("Hello")
        str.contains("ping-service")
        str.contains("msg-001")
    }

    def "每条消息的messageId应该是唯一的"() {
        when:
        def msg1 = new PingPongMessage("PING", "Hello", "source1")
        def msg2 = new PingPongMessage("PING", "Hello", "source1")

        then:
        msg1.messageId != msg2.messageId
    }

    def "时间戳应接近当前时间"() {
        given:
        def before = System.currentTimeMillis()

        when:
        def message = new PingPongMessage("PING", "Hello", "test")
        def after = System.currentTimeMillis()

        then:
        message.timestamp >= before
        message.timestamp <= after
    }
}
