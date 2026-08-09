package com.example.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * 基于 Java FileLock 的跨进程速率限制器
 *
 * 原理：
 *   - 使用文件锁实现跨 JVM 进程的互斥访问
 *   - 在锁文件内记录当前秒的时间戳和已允许的请求数
 *   - 当同一秒内已允许请求数达到 maxPermits 时，拒绝新的请求
 *
 * 使用方式：
 *   FileLockRateLimiter limiter = new FileLockRateLimiter("/tmp/ping-pong-rate.lock", 2);
 *   if (limiter.tryAcquire()) {
 *       // 允许发送请求
 *   } else {
 *       // 被速率限制
 *   }
 */
public class FileLockRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(FileLockRateLimiter.class);

    private final Path lockFilePath;
    private final int maxPermits;

    /**
     * @param lockFilePath 锁文件路径（所有进程必须使用相同路径）
     * @param maxPermits   每秒最大允许请求数
     */
    public FileLockRateLimiter(String lockFilePath, int maxPermits) {
        this.lockFilePath = Paths.get(lockFilePath);
        this.maxPermits = maxPermits;
        ensureLockFileExists();
    }

    private void ensureLockFileExists() {
        try {
            if (!Files.exists(lockFilePath)) {
                Files.createDirectories(lockFilePath.getParent());
                Files.createFile(lockFilePath);
                log.info("[FileLockRateLimiter] 创建锁文件: {}", lockFilePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("[FileLockRateLimiter] 创建锁文件失败: {}", lockFilePath, e);
            throw new RuntimeException("无法创建速率限制锁文件", e);
        }
    }

    /**
     * 尝试获取一个许可
     *
     * @return true 如果获取成功（允许发送请求），false 如果被限流
     */
    public boolean tryAcquire() {
        try (RandomAccessFile raf = new RandomAccessFile(lockFilePath.toFile(), "rw");
             FileChannel channel = raf.getChannel()) {

            FileLock lock = null;
            try {
                // 尝试获取文件锁（跨进程互斥）
                lock = channel.tryLock();
                if (lock == null) {
                    log.warn("[FileLockRateLimiter] 无法获取文件锁，其他进程正在操作");
                    return false;
                }

                // 读取当前记录
                long currentSecond = Instant.now().getEpochSecond();
                String content = readContent(raf);

                long recordedSecond;
                int currentCount;

                if (content == null || content.trim().isEmpty()) {
                    // 文件为空，初始化
                    recordedSecond = currentSecond;
                    currentCount = 0;
                } else {
                    String[] parts = content.trim().split(":");
                    recordedSecond = Long.parseLong(parts[0]);
                    currentCount = Integer.parseInt(parts[1]);
                }

                if (recordedSecond != currentSecond) {
                    // 新的一秒，重置计数器
                    recordedSecond = currentSecond;
                    currentCount = 0;
                }

                if (currentCount < maxPermits) {
                    // 允许请求，增加计数
                    currentCount++;
                    writeContent(raf, recordedSecond + ":" + currentCount);
                    log.debug("[FileLockRateLimiter] 获取许可成功, 当前秒={}, 已用={}/{}",
                            currentSecond, currentCount, maxPermits);
                    return true;
                } else {
                    // 已达上限
                    log.debug("[FileLockRateLimiter] 已达速率上限, 当前秒={}, 已用={}/{}",
                            currentSecond, currentCount, maxPermits);
                    return false;
                }

            } catch (OverlappingFileLockException e) {
                log.warn("[FileLockRateLimiter] 文件锁重叠（同一JVM内）: {}", e.getMessage());
                return false;
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }

        } catch (IOException e) {
            log.error("[FileLockRateLimiter] 操作锁文件异常: {}", lockFilePath, e);
            return false;
        }
    }

    private String readContent(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        String content = raf.readLine();
        return content;
    }

    private void writeContent(RandomAccessFile raf, String content) throws IOException {
        raf.seek(0);
        raf.writeBytes(content);
        raf.setLength(content.length());
    }

    /**
     * 获取最大许可数
     */
    public int getMaxPermits() {
        return maxPermits;
    }

    /**
     * 获取锁文件路径
     */
    public Path getLockFilePath() {
        return lockFilePath;
    }
}
