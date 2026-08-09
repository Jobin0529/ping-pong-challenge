package com.example.common.model

import spock.lang.Specification
import spock.lang.Unroll

/**
 * RequestResult 枚举单元测试
 */
class RequestResultSpec extends Specification {

    def "应包含所有预期的枚举值"() {
        expect:
        RequestResult.values().length == 3
        RequestResult.valueOf("SENT_AND_RESPONDED") != null
        RequestResult.valueOf("RATE_LIMITED_LOCALLY") != null
        RequestResult.valueOf("THROTTLED_BY_PONG") != null
    }

    @Unroll
    def "#name 的 code 应为 #expectedCode"() {
        expect:
        RequestResult.valueOf(name).getCode() == expectedCode

        where:
        name                   | expectedCode
        "SENT_AND_RESPONDED"   | "SENT_AND_RESPONDED"
        "RATE_LIMITED_LOCALLY" | "RATE_LIMITED_LOCALLY"
        "THROTTLED_BY_PONG"    | "THROTTLED_BY_PONG"
    }

    @Unroll
    def "#name 应有非空的 description"() {
        expect:
        RequestResult.valueOf(name).getDescription() != null
        !RequestResult.valueOf(name).getDescription().isEmpty()

        where:
        name << ["SENT_AND_RESPONDED", "RATE_LIMITED_LOCALLY", "THROTTLED_BY_PONG"]
    }

    def "SENT_AND_RESPONDED 描述应包含'正常响应'"() {
        expect:
        RequestResult.SENT_AND_RESPONDED.description.contains("正常响应")
    }

    def "RATE_LIMITED_LOCALLY 描述应包含'速率限制'"() {
        expect:
        RequestResult.RATE_LIMITED_LOCALLY.description.contains("速率限制")
    }

    def "THROTTLED_BY_PONG 描述应包含'429'"() {
        expect:
        RequestResult.THROTTLED_BY_PONG.description.contains("429")
    }
}
