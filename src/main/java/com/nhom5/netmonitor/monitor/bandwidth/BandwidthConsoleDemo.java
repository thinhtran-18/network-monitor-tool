package com.nhom5.netmonitor.monitor.bandwidth;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

/**
 * Demo console tuần 1/2 (chạy độc lập): in danh sách card, rồi mỗi giây in byte gửi/nhận và tốc độ.
 *
 * <p>Cách chạy: {@code BandwidthConsoleDemo [seconds] [tên-card ...]}. Không có tên card thì
 * theo dõi mọi card đang có lưu lượng.
 */
public final class BandwidthConsoleDemo {
    private BandwidthConsoleDemo() {
    }

    public static void main(String[] args) throws Exception {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        System.out.println("== OSHI NetworkIF ==");
        for (NetworkIF nif : new SystemInfo().getHardware().getNetworkIFs()) {
            System.out.printf("%s | %s | speed=%d bit/s | recv=%d | sent=%d%n",
                    nif.getName(), nif.getDisplayName(), nif.getSpeed(), nif.getBytesRecv(), nif.getBytesSent());
        }
        System.out.println("== java.net.NetworkInterface (đối chiếu) ==");
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            System.out.printf("%s | %s | up=%b | loopback=%b%n",
                    ni.getName(), ni.getDisplayName(), ni.isUp(), ni.isLoopback());
        }

        BandwidthMonitor monitor = new BandwidthMonitor(1000);
        Set<String> ids = new HashSet<>();
        if (args.length > 1) {
            ids.addAll(List.of(args).subList(1, args.length));
        } else {
            monitor.listInterfaces().forEach(i -> ids.add(i.id()));
        }
        monitor.selectInterfaces(ids);
        monitor.addListener(s -> {
            if (s.recvBytesPerSec() > 0 || s.sentBytesPerSec() > 0) {
                System.out.printf("%s | recv=%d B (%.0f B/s) | sent=%d B (%.0f B/s) | usage=%s%n",
                        s.displayName(), s.totalBytesRecv(), s.recvBytesPerSec(),
                        s.totalBytesSent(), s.sentBytesPerSec(),
                        s.usagePercent().isPresent() ? String.format("%.2f%%", s.usagePercent().getAsDouble()) : "n/a");
            }
        });
        System.out.println("== Lấy mẫu " + seconds + " giây ==");
        monitor.start();
        Thread.sleep(seconds * 1000L);
        monitor.stop();
        System.out.println("Đã dừng.");
    }
}

