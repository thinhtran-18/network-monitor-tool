package com.nhom5.netmonitor.monitor.bandwidth;

import com.nhom5.netmonitor.core.BandwidthSample;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Bài kiểm thử đối chiếu độ chính xác với Task Manager / Resource Monitor.
 *
 * <p>Chạy BandwidthMonitor liên tục trong khoảng thời gian cho trước, ghi kết quả
 * ra file CSV, đồng thời in ra console để người dùng so sánh bằng mắt với Task Manager.
 *
 * <p>Kết quả ghi gồm: thời gian, tên card, recv B/s, sent B/s, usage%, interval (ms),
 * tổng byte recv, tổng byte sent. Cuối file ghi thống kê tổng hợp.
 *
 * <p>Sản phẩm tuần 4 — Nguyễn Quang Huy.
 *
 * <p>Cách dùng: {@code AccuracyBenchmark [seconds] [card-name] [output-csv]}
 */
public final class AccuracyBenchmark {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private AccuracyBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        String cardFilter = args.length > 1 ? args[1] : null;
        String csvFile = args.length > 2 ? args[2] : "accuracy_benchmark.csv";

        BandwidthMonitor monitor = new BandwidthMonitor(1000);
        BandwidthHistory history = new BandwidthHistory(seconds + 10);

        // Chọn card
        var cards = monitor.listInterfaces();
        BandwidthMonitor.InterfaceInfo chosen = null;
        for (var card : cards) {
            if (cardFilter != null && card.displayName().contains(cardFilter)) {
                chosen = card;
                break;
            }
            if (card.speedBitsPerSec() > 0 && !card.displayName().toLowerCase().contains("virtual")
                    && !card.displayName().toLowerCase().contains("filter")) {
                if (chosen == null) chosen = card;
            }
        }
        if (chosen == null && !cards.isEmpty()) chosen = cards.get(0);
        if (chosen == null) {
            System.err.println("Khong tim thay card mang.");
            System.exit(1);
        }

        System.out.println("== Doi chieu do chinh xac ==");
        System.out.println("Card: " + chosen.displayName() + " [" + chosen.id() + "]");
        System.out.println("Toc do danh dinh: " + formatSpeed(chosen.speedBitsPerSec()));
        System.out.println("Thoi gian: " + seconds + " giay");
        System.out.println("Mo Task Manager > Performance > Ethernet/WiFi de so sanh.");
        System.out.println("File CSV: " + csvFile);
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-12s %12s %12s %8s%n", "Thoi gian", "Recv (B/s)", "Sent (B/s)", "Usage%");
        System.out.println("------------------------------------------------------------");

        monitor.addListener(sample -> {
            history.add(sample);
            String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(sample.timestampMillis()),
                    ZoneId.systemDefault()).format(FMT);
            String usage = sample.usagePercent().isPresent()
                    ? String.format(Locale.ROOT, "%7.2f%%", sample.usagePercent().getAsDouble())
                    : "    n/a";
            System.out.printf("%-12s %,12.0f %,12.0f %s%n", time,
                    sample.recvBytesPerSec(), sample.sentBytesPerSec(), usage);
        });

        monitor.selectInterfaces(java.util.Set.of(chosen.id()));
        monitor.start();
        Thread.sleep(seconds * 1000L);
        monitor.stop();

        // Ghi CSV
        List<BandwidthSample> allSamples = history.snapshot();
        BandwidthHistory.Stats stats = history.sessionStats();
        writeCsv(Path.of(csvFile), allSamples, stats, chosen);

        // In thống kê
        System.out.println("------------------------------------------------------------");
        System.out.println("== Thong ke phien ==");
        System.out.printf("So mau:          %d%n", stats.sampleCount());
        System.out.printf("Thoi gian:       %.1f giay%n", stats.durationMillis() / 1000.0);
        System.out.printf("Tong nhan:       %s%n", formatBytes(stats.totalBytesRecv()));
        System.out.printf("Tong gui:        %s%n", formatBytes(stats.totalBytesSent()));
        System.out.printf("TB nhan:         %s/s%n", formatBytes((long) stats.avgRecvBytesPerSec()));
        System.out.printf("TB gui:          %s/s%n", formatBytes((long) stats.avgSentBytesPerSec()));
        System.out.printf("Cao nhat nhan:   %s/s%n", formatBytes((long) stats.maxRecvBytesPerSec()));
        System.out.printf("Cao nhat gui:    %s/s%n", formatBytes((long) stats.maxSentBytesPerSec()));
        System.out.printf("Thap nhat nhan:  %s/s%n", formatBytes((long) stats.minRecvBytesPerSec()));
        System.out.printf("Thap nhat gui:   %s/s%n", formatBytes((long) stats.minSentBytesPerSec()));
        System.out.println("------------------------------------------------------------");
        System.out.println("Hay ghi so lieu Task Manager vao bang doi chieu trong docs/GHI_CHU_OSHI_TUAN_1.md");
    }

    private static void writeCsv(Path path, List<BandwidthSample> samples,
                                  BandwidthHistory.Stats stats, BandwidthMonitor.InterfaceInfo card)
            throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            w.println("# AccuracyBenchmark — Card: " + card.displayName()
                    + " | Speed: " + formatSpeed(card.speedBitsPerSec()));
            w.println("timestamp_ms,time,interface,recv_bytes_per_sec,sent_bytes_per_sec,"
                    + "total_recv,total_sent,usage_percent");
            for (BandwidthSample s : samples) {
                String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.timestampMillis()),
                        ZoneId.systemDefault()).format(FMT);
                w.printf(Locale.ROOT, "%d,%s,%s,%.2f,%.2f,%d,%d,%s%n",
                        s.timestampMillis(), time, s.interfaceId(),
                        s.recvBytesPerSec(), s.sentBytesPerSec(),
                        s.totalBytesRecv(), s.totalBytesSent(),
                        s.usagePercent().isPresent()
                                ? String.format(Locale.ROOT, "%.4f", s.usagePercent().getAsDouble())
                                : "");
            }
            w.println();
            w.println("# Session stats");
            w.printf("# Samples: %d, Duration: %.1f s%n", stats.sampleCount(), stats.durationMillis() / 1000.0);
            w.printf("# Total recv: %d bytes, Total sent: %d bytes%n", stats.totalBytesRecv(), stats.totalBytesSent());
            w.printf(Locale.ROOT, "# Avg recv: %.2f B/s, Avg sent: %.2f B/s%n",
                    stats.avgRecvBytesPerSec(), stats.avgSentBytesPerSec());
            w.printf(Locale.ROOT, "# Max recv: %.2f B/s, Max sent: %.2f B/s%n",
                    stats.maxRecvBytesPerSec(), stats.maxSentBytesPerSec());
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1_000_000_000) return String.format(Locale.ROOT, "%.2f GB", bytes / 1e9);
        if (bytes >= 1_000_000) return String.format(Locale.ROOT, "%.2f MB", bytes / 1e6);
        if (bytes >= 1_000) return String.format(Locale.ROOT, "%.2f KB", bytes / 1e3);
        return bytes + " B";
    }

    private static String formatSpeed(long bitsPerSec) {
        if (bitsPerSec >= 1_000_000_000) return String.format(Locale.ROOT, "%.0f Gbps", bitsPerSec / 1e9);
        if (bitsPerSec >= 1_000_000) return String.format(Locale.ROOT, "%.0f Mbps", bitsPerSec / 1e6);
        return bitsPerSec + " bps";
    }
}

