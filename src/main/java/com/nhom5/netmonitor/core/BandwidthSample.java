package com.nhom5.netmonitor.core;

import java.util.OptionalDouble;

/**
 * Bất biến: một mẫu băng thông của một card mạng tại một thời điểm.
 *
 * @param interfaceId       mã card (tên hệ thống, ví dụ "eth3")
 * @param displayName       tên hiển thị của card
 * @param timestampMillis   thời điểm lấy mẫu (epoch millis)
 * @param totalBytesRecv    tổng byte đã nhận từ khi card hoạt động
 * @param totalBytesSent    tổng byte đã gửi từ khi card hoạt động
 * @param recvBytesPerSec   tốc độ nhận (byte/giây)
 * @param sentBytesPerSec   tốc độ gửi (byte/giây)
 * @param linkSpeedBitsPerSec tốc độ danh định của liên kết (bit/giây); 0 nếu không rõ
 * @param adapterStats      địa chỉ và bộ đếm gói tin của card nếu có
 * @param usagePercent      % băng thông sử dụng (nhận+gửi, theo danh định); rỗng nếu không tính được
 */
public record BandwidthSample(
        String interfaceId,
        String displayName,
        long timestampMillis,
        long totalBytesRecv,
        long totalBytesSent,
        double recvBytesPerSec,
        double sentBytesPerSec,
        long linkSpeedBitsPerSec,
        OptionalDouble usagePercent,
        AdapterStats adapterStats) {
    public record AdapterStats(String ipv4, String mac, long packetsSent, long packetsReceived,
                               long sendErrors, long receiveErrors) { }

    public BandwidthSample(String interfaceId, String displayName, long timestampMillis,
                           long totalBytesRecv, long totalBytesSent, double recvBytesPerSec,
                           double sentBytesPerSec, long linkSpeedBitsPerSec, OptionalDouble usagePercent) {
        this(interfaceId, displayName, timestampMillis, totalBytesRecv, totalBytesSent, recvBytesPerSec,
                sentBytesPerSec, linkSpeedBitsPerSec, usagePercent, null);
    }
}

