package com.nhom5.netmonitor.monitor.bandwidth;

import java.util.OptionalDouble;

/** Công thức tính băng thông thuần túy (dễ kiểm thử, không phụ thuộc OSHI). */
public final class BandwidthCalculator {
    private BandwidthCalculator() {
    }

    /**
     * Tốc độ byte/giây giữa hai mốc counter.
     *
     * @return giá trị >= 0, hoặc -1 nếu mẫu không hợp lệ (counter giảm, thời gian không tăng)
     */
    public static double bytesPerSecond(long previousBytes, long currentBytes, long elapsedMillis) {
        if (elapsedMillis <= 0 || currentBytes < previousBytes) {
            return -1;
        }
        return (currentBytes - previousBytes) * 1000.0 / elapsedMillis;
    }

    /**
     * Tỷ lệ sử dụng = 8 * (recv + sent) / speed * 100; chỉ tính khi speed &gt; 0.
     * Kết quả được giới hạn trong [0, 100].
     */
    public static OptionalDouble usagePercent(double recvBytesPerSec, double sentBytesPerSec, long speedBitsPerSec) {
        if (speedBitsPerSec <= 0) {
            return OptionalDouble.empty();
        }
        double pct = 8.0 * (recvBytesPerSec + sentBytesPerSec) / speedBitsPerSec * 100.0;
        return OptionalDouble.of(Math.max(0.0, Math.min(100.0, pct)));
    }
}

