package com.nhom5.netmonitor.monitor.bandwidth;

import com.nhom5.netmonitor.core.BandwidthSample;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Lưu lịch sử các mẫu băng thông trong bộ nhớ (giới hạn số mẫu)
 * và cung cấp số liệu thống kê cho biểu đồ và bảng thống kê.
 *
 * <p>Thread-safe: có thể gọi {@link #add(BandwidthSample)} từ thread nền
 * và đọc thống kê từ EDT.
 *
 * <p>Sản phẩm tuần 3 — Nguyễn Quang Huy.
 */
public final class BandwidthHistory {

    /** Kết quả thống kê bất biến cho một giai đoạn. */
    public record Stats(
            long sampleCount,
            long totalBytesRecv,
            long totalBytesSent,
            double avgRecvBytesPerSec,
            double avgSentBytesPerSec,
            double maxRecvBytesPerSec,
            double maxSentBytesPerSec,
            double minRecvBytesPerSec,
            double minSentBytesPerSec,
            long firstTimestamp,
            long lastTimestamp,
            long durationMillis) {

        /** Tổng tốc độ trung bình (nhận + gửi). */
        public double avgTotalBytesPerSec() {
            return avgRecvBytesPerSec + avgSentBytesPerSec;
        }
    }

    private final int maxSamples;
    private final Deque<BandwidthSample> samples;

    // Thống kê tích lũy toàn phiên (không bị ảnh hưởng bởi giới hạn maxSamples)
    private long sessionSampleCount;
    private double sessionRecvSum;
    private double sessionSentSum;
    private double sessionRecvMax;
    private double sessionSentMax;
    private double sessionRecvMin = Double.MAX_VALUE;
    private double sessionSentMin = Double.MAX_VALUE;
    private long sessionTotalRecv;
    private long sessionTotalSent;
    private long sessionFirstTimestamp = -1;
    private long sessionLastTimestamp;
    private BandwidthSample firstSample;

    /** Mặc định giữ tối đa 900 mẫu (15 phút ở tần suất 1 giây). */
    public BandwidthHistory() {
        this(900);
    }

    /**
     * @param maxSamples số mẫu tối đa giữ trong bộ nhớ cho biểu đồ (>= 10)
     */
    public BandwidthHistory(int maxSamples) {
        if (maxSamples < 10) throw new IllegalArgumentException("maxSamples phải >= 10");
        this.maxSamples = maxSamples;
        this.samples = new ArrayDeque<>(maxSamples);
    }

    /** Thêm một mẫu mới (gọi từ thread nền). */
    public synchronized void add(BandwidthSample sample) {
        // Cập nhật thống kê phiên
        sessionSampleCount++;
        sessionRecvSum += sample.recvBytesPerSec();
        sessionSentSum += sample.sentBytesPerSec();
        sessionRecvMax = Math.max(sessionRecvMax, sample.recvBytesPerSec());
        sessionSentMax = Math.max(sessionSentMax, sample.sentBytesPerSec());
        sessionRecvMin = Math.min(sessionRecvMin, sample.recvBytesPerSec());
        sessionSentMin = Math.min(sessionSentMin, sample.sentBytesPerSec());
        sessionLastTimestamp = sample.timestampMillis();
        if (sessionFirstTimestamp < 0) {
            sessionFirstTimestamp = sample.timestampMillis();
            firstSample = sample;
        }
        // Tính tổng byte nhận/gửi trong phiên dựa trên chênh lệch counter
        if (firstSample != null) {
            sessionTotalRecv = Math.max(0, sample.totalBytesRecv() - firstSample.totalBytesRecv());
            sessionTotalSent = Math.max(0, sample.totalBytesSent() - firstSample.totalBytesSent());
        }
        // Giữ giới hạn bộ nhớ cho biểu đồ
        if (samples.size() >= maxSamples) {
            samples.removeFirst();
        }
        samples.addLast(sample);
    }

    /** Trả về danh sách mẫu hiện có (bản sao, an toàn để dùng trên EDT). */
    public synchronized List<BandwidthSample> snapshot() {
        return new ArrayList<>(samples);
    }

    /**
     * Trả về các mẫu trong khoảng {@code durationMillis} cuối cùng.
     * Dùng cho biểu đồ với cửa sổ thời gian khác nhau (1 phút, 5 phút, 15 phút).
     */
    public synchronized List<BandwidthSample> snapshot(long durationMillis) {
        if (samples.isEmpty()) return List.of();
        long cutoff = samples.getLast().timestampMillis() - durationMillis;
        List<BandwidthSample> result = new ArrayList<>();
        for (BandwidthSample s : samples) {
            if (s.timestampMillis() >= cutoff) {
                result.add(s);
            }
        }
        return result;
    }

    /** Mẫu mới nhất, hoặc {@code null} nếu chưa có. */
    public synchronized BandwidthSample latest() {
        return samples.isEmpty() ? null : samples.getLast();
    }

    /** Số mẫu đang giữ trong bộ nhớ (cho biểu đồ). */
    public synchronized int size() {
        return samples.size();
    }

    /** Thống kê toàn phiên (từ lúc bắt đầu cho đến mẫu mới nhất). */
    public synchronized Stats sessionStats() {
        if (sessionSampleCount == 0) return emptyStats();
        return new Stats(
                sessionSampleCount,
                sessionTotalRecv,
                sessionTotalSent,
                sessionRecvSum / sessionSampleCount,
                sessionSentSum / sessionSampleCount,
                sessionRecvMax,
                sessionSentMax,
                sessionRecvMin,
                sessionSentMin,
                sessionFirstTimestamp,
                sessionLastTimestamp,
                sessionLastTimestamp - sessionFirstTimestamp);
    }

    /**
     * Thống kê chỉ cho các mẫu trong khoảng {@code durationMillis} gần nhất.
     * Ví dụ: thống kê 1 phút gần nhất, 5 phút gần nhất.
     */
    public synchronized Stats windowStats(long durationMillis) {
        List<BandwidthSample> window = snapshot(durationMillis);
        if (window.isEmpty()) return emptyStats();
        double rSum = 0, sSum = 0;
        double rMax = 0, sMax = 0;
        double rMin = Double.MAX_VALUE, sMin = Double.MAX_VALUE;
        long totalRecv = 0, totalSent = 0;
        BandwidthSample first = window.get(0);
        BandwidthSample last = window.get(window.size() - 1);
        for (BandwidthSample s : window) {
            rSum += s.recvBytesPerSec();
            sSum += s.sentBytesPerSec();
            rMax = Math.max(rMax, s.recvBytesPerSec());
            sMax = Math.max(sMax, s.sentBytesPerSec());
            rMin = Math.min(rMin, s.recvBytesPerSec());
            sMin = Math.min(sMin, s.sentBytesPerSec());
        }
        totalRecv = Math.max(0, last.totalBytesRecv() - first.totalBytesRecv());
        totalSent = Math.max(0, last.totalBytesSent() - first.totalBytesSent());
        return new Stats(window.size(), totalRecv, totalSent,
                rSum / window.size(), sSum / window.size(),
                rMax, sMax, rMin, sMin,
                first.timestampMillis(), last.timestampMillis(),
                last.timestampMillis() - first.timestampMillis());
    }

    /** Xóa toàn bộ lịch sử và thống kê (khi đổi card). */
    public synchronized void clear() {
        samples.clear();
        sessionSampleCount = 0;
        sessionRecvSum = sessionSentSum = 0;
        sessionRecvMax = sessionSentMax = 0;
        sessionRecvMin = Double.MAX_VALUE;
        sessionSentMin = Double.MAX_VALUE;
        sessionTotalRecv = sessionTotalSent = 0;
        sessionFirstTimestamp = -1;
        sessionLastTimestamp = 0;
        firstSample = null;
    }

    private static Stats emptyStats() {
        return new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}

