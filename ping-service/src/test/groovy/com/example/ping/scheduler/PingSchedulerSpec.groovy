package com.example.ping.scheduler

import com.example.common.model.RequestResult
import com.example.ping.service.PingService
import spock.lang.Specification
import spock.lang.Subject

/**
 * PingScheduler 单元测试
 */
class PingSchedulerSpec extends Specification {

    PingService pingService = Mock(PingService)

    @Subject
    PingScheduler scheduler = new PingScheduler(pingService)

    def "初始状态所有计数器应为0"() {
        expect:
        scheduler.totalAttempts == 0
        scheduler.successCount == 0
        scheduler.rateLimitedCount == 0
        scheduler.throttledByPongCount == 0
    }

    def "当PingService返回SENT_AND_RESPONDED时成功计数应增加"() {
        given:
        pingService.sendPing() >> RequestResult.SENT_AND_RESPONDED

        when:
        scheduler.scheduledPing()

        then:
        scheduler.totalAttempts == 1
        scheduler.successCount == 1
        scheduler.rateLimitedCount == 0
        scheduler.throttledByPongCount == 0
    }

    def "当PingService返回RATE_LIMITED_LOCALLY时限流计数应增加"() {
        given:
        pingService.sendPing() >> RequestResult.RATE_LIMITED_LOCALLY

        when:
        scheduler.scheduledPing()

        then:
        scheduler.totalAttempts == 1
        scheduler.successCount == 0
        scheduler.rateLimitedCount == 1
        scheduler.throttledByPongCount == 0
    }

    def "当PingService返回THROTTLED_BY_PONG时Pong限流计数应增加"() {
        given:
        pingService.sendPing() >> RequestResult.THROTTLED_BY_PONG

        when:
        scheduler.scheduledPing()

        then:
        scheduler.totalAttempts == 1
        scheduler.successCount == 0
        scheduler.rateLimitedCount == 0
        scheduler.throttledByPongCount == 1
    }

    def "多次调用应正确累计计数"() {
        given:
        pingService.sendPing() >>> [
                RequestResult.SENT_AND_RESPONDED,
                RequestResult.RATE_LIMITED_LOCALLY,
                RequestResult.THROTTLED_BY_PONG,
                RequestResult.SENT_AND_RESPONDED
        ]

        when:
        4.times { scheduler.scheduledPing() }

        then:
        scheduler.totalAttempts == 4
        scheduler.successCount == 2
        scheduler.rateLimitedCount == 1
        scheduler.throttledByPongCount == 1
    }
}
