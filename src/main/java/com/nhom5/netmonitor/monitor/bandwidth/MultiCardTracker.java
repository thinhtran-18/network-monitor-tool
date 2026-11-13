package com.nhom5.netmonitor.monitor.bandwidth;

import com.nhom5.netmonitor.core.BandwidthSample;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Quản lý theo dõi nhiều card mạng cùng lúc, mỗi card có một {@link BandwidthHistory} riêng.
 *
 * <p>Hỗ trợ chọn/bỏ chọn card khi đang chạy. Khi đổi tập card, lịch sử các card
 * bị bỏ chọn sẽ được giữ lại (để xem lại) cho đến khi gọi {@link #clearAll()}.
 *
 * <p>Thread-safe: có thể gọi từ thread nền và đọc từ EDT.
 *
 * <p>Sản phẩm tuần 3 — Nguyễn Quang Huy.
 */
public final class MultiCardTracker {

    private final int maxSamplesPerCard;
    private final Map<String, BandwidthHistory> histories = new LinkedHashMap<>();

    public MultiCardTracker() {
        this(900);
    }

    public MultiCardTracker(int maxSamplesPerCard) {
        this.maxSamplesPerCard = maxSamplesPerCard;
    }

    /**
     * Ghi nhận một mẫu cho card tương ứng.
     * Nếu card chưa có lịch sử thì tạo mới.
     */
    public synchronized void accept(BandwidthSample sample) {
        histories.computeIfAbsent(sample.interfaceId(), id -> new BandwidthHistory(maxSamplesPerCard))
                .add(sample);
    }

    /** Lấy lịch sử của một card cụ thể, hoặc {@code null} nếu chưa có mẫu nào. */
    public synchronized BandwidthHistory getHistory(String interfaceId) {
        return histories.get(interfaceId);
    }

    /** Tập hợp tất cả card đã từng có mẫu. */
    public synchronized Set<String> trackedCards() {
        return Set.copyOf(histories.keySet());
    }

    /** Xóa lịch sử của một card. */
    public synchronized void clear(String interfaceId) {
        BandwidthHistory h = histories.get(interfaceId);
        if (h != null) h.clear();
    }

    /** Xóa toàn bộ lịch sử tất cả các card. */
    public synchronized void clearAll() {
        histories.clear();
    }

    /** Số card đang được theo dõi (đã có ít nhất 1 mẫu). */
    public synchronized int cardCount() {
        return histories.size();
    }
}

