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

    // 日志记录器，用于输出调试、警告和错误信息
    private static final Logger log = LoggerFactory.getLogger(FileLockRateLimiter.class);
    // 锁文件的路径，所有进程必须使用相同的路径才能共享限流状态
    private final Path lockFilePath;
    // 每秒允许的最大请求数（许可数）
    private final int maxPermits;

    /**
     * @param lockFilePath 锁文件路径（所有进程必须使用相同路径）
     * @param maxPermits   每秒最大允许请求数
     */
    public FileLockRateLimiter(String lockFilePath, int maxPermits) {
        this.lockFilePath = Paths.get(lockFilePath);
        this.maxPermits = maxPermits;
        ensureLockFileExists(); // 确保锁文件存在，若不存在则创建
    }

    private void ensureLockFileExists() {
        try {
            if (!Files.exists(lockFilePath)) {// 检查文件是否存在
                Files.createDirectories(lockFilePath.getParent());// 创建父目录（如果不存在）
                Files.createFile(lockFilePath);// 创建空文件
                log.info("[FileLockRateLimiter] 创建锁文件: {}", lockFilePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("[FileLockRateLimiter] 创建锁文件失败: {}", lockFilePath, e);
            throw new RuntimeException("无法创建速率限制锁文件", e);
        }
    }

    /**
     * 尝试获取一个许可
     * 使用 try-with-resources 确保 RandomAccessFile 和 FileChannel 自动关闭
     * 整个操作在文件锁的保护下是原子性的，从而保证跨进程的计数器一致性
     * @return true 如果获取成功（允许发送请求），false 如果被限流
     */
    public boolean tryAcquire() {
        try (RandomAccessFile raf = new RandomAccessFile(lockFilePath.toFile(), "rw");
             FileChannel channel = raf.getChannel()) {

            FileLock lock = null;
            try {
                // 尝试获取文件锁（非阻塞），若被其他进程占用则返回 null
                // 文件锁由操作系统维护，可跨 JVM 进程同步
                lock = channel.tryLock();
                if (lock == null) {
                    // 其他进程正在操作文件，当前进程无法获得锁，直接返回 false（限流）
                    log.warn("[FileLockRateLimiter] 无法获取文件锁，其他进程正在操作");
                    return false;
                }

                // 获取当前时间的秒数，用于判断是否在同一秒内
                long currentSecond = Instant.now().getEpochSecond();
                // 读取锁文件中的内容，格式为 "秒数:已用许可数"，例如 "1682345678:3"
                String content = readContent(raf);

                long recordedSecond;// 文件中记录的秒数
                int currentCount;   // 当前秒内已使用的许可数

                if (content == null || content.trim().isEmpty()) {
                    // 文件为空（首次使用或文件被清空），则初始化状态
                    recordedSecond = currentSecond;
                    currentCount = 0;
                } else {
                    // 解析文件内容，格式为 "秒数:计数"
                    String[] parts = content.trim().split(":");
                    recordedSecond = Long.parseLong(parts[0]);
                    currentCount = Integer.parseInt(parts[1]);
                }

                // 判断是否进入了新的一秒
                if (recordedSecond != currentSecond) {
                    // 如果是新秒，重置计数（归零），并更新记录的秒数
                    recordedSecond = currentSecond;
                    currentCount = 0;
                }

                // 检查当前已用许可数是否小于最大许可数
                if (currentCount < maxPermits) {
                    // 允许请求，增加计数
                    currentCount++;
                    writeContent(raf, recordedSecond + ":" + currentCount);
                    log.debug("[FileLockRateLimiter] 获取许可成功, 当前秒={}, 已用={}/{}",
                            currentSecond, currentCount, maxPermits);
                    return true;
                } else {
                    // 已达上限，拒绝本次请求
                    log.debug("[FileLockRateLimiter] 已达速率上限, 当前秒={}, 已用={}/{}",
                            currentSecond, currentCount, maxPermits);
                    return false;
                }

            } catch (OverlappingFileLockException e) {
                log.warn("[FileLockRateLimiter] 文件锁重叠（同一JVM内）: {}", e.getMessage());
                return false;
            } finally {
                // 无论成功还是失败，都要释放文件锁，以便其他进程获取
                if (lock != null) {
                    lock.release();
                }
            }

        } catch (IOException e) {
            // 文件读写异常（如磁盘错误、文件被删除等），记录错误并返回 false（保守限流）
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
