package com.nhom5.netmonitor.monitor.bandwidth;

import com.nhom5.netmonitor.core.BandwidthListener;
import com.nhom5.netmonitor.core.BandwidthSample;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

/**
 * Lấy mẫu byte gửi/nhận của các card mạng được chọn mỗi chu kỳ trên một thread nền,
 * tính tốc độ, tổng byte, % băng thông và phát {@link BandwidthSample} cho listener.
 *
 * <p>Listener được gọi trên thread nền; UI phải tự chuyển sang EDT.
 */
public final class BandwidthMonitor {

    /** Thông tin tối thiểu để người dùng chọn card. */
    public record InterfaceInfo(String id, String displayName, long speedBitsPerSec) {
        @Override
        public String toString() {
            return displayName + " [" + id + "]";
        }
    }

    private record Baseline(long recv, long sent, long nanoTime) {
    }

    private final HardwareAbstractionLayer hal;
    private final long intervalMillis;
    private final List<BandwidthListener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> selected = new CopyOnWriteArraySet<>();
    private volatile Thread worker;

    public BandwidthMonitor() {
        this(1000);
    }

    public BandwidthMonitor(long intervalMillis) {
        if (intervalMillis < 100) {
            throw new IllegalArgumentException("intervalMillis phải >= 100");
        }
        this.intervalMillis = intervalMillis;
        this.hal = new SystemInfo().getHardware();
    }

    /** Liệt kê các card mạng hiện có. */
    public List<InterfaceInfo> listInterfaces() {
        List<InterfaceInfo> result = new ArrayList<>();
        for (NetworkIF nif : hal.getNetworkIFs()) {
            result.add(new InterfaceInfo(nif.getName(), nif.getDisplayName(), nif.getSpeed()));
        }
        return result;
    }

    /** Chọn card cần theo dõi (có thể gọi khi đang chạy). Đổi lựa chọn sẽ lấy lại mốc. */
    public void selectInterfaces(Set<String> interfaceIds) {
        selected.clear();
        selected.addAll(interfaceIds);
    }

    public void addListener(BandwidthListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BandwidthListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() {
        if (worker != null) {
            return;
        }
        Thread t = new Thread(this::runLoop, "bandwidth-monitor");
        t.setDaemon(true);
        worker = t;
        t.start();
    }

    /** Yêu cầu dừng và chờ tối đa ba chu kỳ; không khởi động worker mới khi worker cũ còn chạy. */
    public void stop() {
        Thread t;
        synchronized (this) {
            t = worker;
            if (t == null) return;
            t.interrupt();
        }
        if (t == Thread.currentThread()) return;
        try {
            t.join(intervalMillis * 3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runLoop() {
        Thread self = Thread.currentThread();
        // Mỗi lần chạy có mốc riêng, nên không tính cả thời gian monitor đã dừng.
        Map<String, Baseline> baselines = new HashMap<>();
        try {
            while (!self.isInterrupted()) {
                try {
                    sampleOnce(baselines);
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    return;
                } catch (RuntimeException e) {
                    for (BandwidthListener l : listeners) {
                        l.onBandwidthError(null, e);
                    }
                    try {
                        Thread.sleep(intervalMillis);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
        } finally {
            synchronized (this) {
                if (worker == self) worker = null;
            }
        }
    }

    private void sampleOnce(Map<String, Baseline> baselines) {
        List<NetworkIF> all = hal.getNetworkIFs();
        Set<String> present = new java.util.HashSet<>();
        for (NetworkIF nif : all) {
            String id = nif.getName();
            present.add(id);
            if (!selected.contains(id)) {
                baselines.remove(id);
                continue;
            }
            if (!nif.updateAttributes()) {
                baselines.remove(id); // không đọc được -> lấy lại mốc
                continue;
            }
            long recv = nif.getBytesRecv();
            long sent = nif.getBytesSent();
            long now = System.nanoTime();
            Baseline prev = baselines.put(id, new Baseline(recv, sent, now));
            if (prev == null) {
                continue; // mẫu đầu: chỉ lấy mốc
            }
            long elapsedMillis = (now - prev.nanoTime()) / 1_000_000L;
            double recvRate = BandwidthCalculator.bytesPerSecond(prev.recv(), recv, elapsedMillis);
            double sentRate = BandwidthCalculator.bytesPerSecond(prev.sent(), sent, elapsedMillis);
            if (recvRate < 0 || sentRate < 0) {
                continue; // counter giảm / thời gian không tăng: bỏ mẫu, mốc mới đã lưu
            }
            long speed = nif.getSpeed();
            BandwidthSample sample = new BandwidthSample(id, nif.getDisplayName(), System.currentTimeMillis(),
                    recv, sent, recvRate, sentRate, speed,
                    BandwidthCalculator.usagePercent(recvRate, sentRate, speed),
                    new BandwidthSample.AdapterStats(String.join(", ", nif.getIPv4addr()), nif.getMacaddr(),
                            nif.getPacketsSent(), nif.getPacketsRecv(), nif.getOutErrors(), nif.getInErrors()));
            for (BandwidthListener l : listeners) {
                l.onBandwidthSample(sample);
            }
        }
        baselines.keySet().retainAll(present); // card biến mất
    }
}

