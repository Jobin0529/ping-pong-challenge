package com.example.common.ratelimit

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Subject

import java.nio.file.Path

/**
 * FileLockRateLimiter 单元测试
 */
class FileLockRateLimiterSpec extends Specification {

    @TempDir
    Path tempDir

    @Subject
    FileLockRateLimiter rateLimiter

    def "初始化时应创建锁文件"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()

        when:
        rateLimiter = new FileLockRateLimiter(lockFile, 2)

        then:
        rateLimiter.lockFilePath.toFile().exists()
        rateLimiter.maxPermits == 2
    }

    def "第一次获取许可应成功"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 2)

        when:
        def result = rateLimiter.tryAcquire()

        then:
        result == true
    }

    def "在限制范围内获取许可应全部成功"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 3)

        when:
        def results = (1..3).collect { rateLimiter.tryAcquire() }

        then:
        results.every { it == true }
    }

    def "超过限制后获取许可应失败"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 2)

        when: "获取2个许可"
        rateLimiter.tryAcquire()
        rateLimiter.tryAcquire()
        def thirdAttempt = rateLimiter.tryAcquire()

        then: "第三次应失败"
        thirdAttempt == false
    }

    def "限制为1时第二次获取应失败"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 1)

        when:
        def first = rateLimiter.tryAcquire()
        def second = rateLimiter.tryAcquire()

        then:
        first == true
        second == false
    }

    def "getMaxPermits应返回正确的限制值"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 5)

        expect:
        rateLimiter.maxPermits == 5
    }

    def "getLockFilePath应返回正确的路径"() {
        given:
        def lockFile = tempDir.resolve("test.lock").toString()
        rateLimiter = new FileLockRateLimiter(lockFile, 2)

        expect:
        rateLimiter.lockFilePath.toString() == lockFile
    }
}
