package com.nhom5.netmonitor.core;

/** Nhận sự kiện cập nhật băng thông. Được gọi trên thread nền, không phải EDT. */
@FunctionalInterface
public interface BandwidthListener {

    /** Có mẫu băng thông mới cho một card. */
    void onBandwidthSample(BandwidthSample sample);

    /** Có lỗi khi lấy mẫu (mặc định bỏ qua). */
    default void onBandwidthError(String interfaceId, Throwable error) {
    }
}

