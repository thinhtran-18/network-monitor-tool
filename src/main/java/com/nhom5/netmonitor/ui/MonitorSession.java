package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.BandwidthSample;
import com.nhom5.netmonitor.core.ConnectionFilter;
import com.nhom5.netmonitor.core.ConnectionInfo;
import com.nhom5.netmonitor.monitor.bandwidth.BandwidthMonitor;
import com.nhom5.netmonitor.monitor.bandwidth.BandwidthMonitor.InterfaceInfo;
import com.nhom5.netmonitor.monitor.connection.ConnectionMonitor;
import com.nhom5.netmonitor.network.SocketService;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

/** Shares one set of monitors and the current session across Huyền's four views. */
final class MonitorSession {
    static final long HISTORY_MILLIS = 15 * 60 * 1000L;
    private final MonitorFrame[] pages = new MonitorFrame[4];
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "monitor-session");
        thread.setDaemon(true);
        return thread;
    });
    final SocketService socket = new SocketService(
            status -> onEdt(() -> socketPage().showSocketStatus(status)),
            event -> onEdt(() -> socketPage().appendEvent(event)));
    private ConnectionMonitor connections;
    private BandwidthMonitor bandwidth;
    private MonitorFrame current;
    private boolean closed;
    private List<ConnectionInfo> rows = List.of();
    private boolean connectionError;
    private long connectionsAt;
    private final ArrayDeque<BandwidthSample> history = new ArrayDeque<>();
    private BandwidthSample latest;
    private long received, sent, samples;
    private double downSum, upSum, downMax, upMax;
    private String selectedCard;
    private String bandwidthStatus = "Đang tìm card mạng";

    static void launch(int page) {
        SwingUtilities.invokeLater(() -> {
            MonitorSession session = new MonitorSession();
            session.show(page);
            session.worker.execute(session::start);
        });
    }

    void show(int page) {
        MonitorFrame next = pages[page];
        if (next == null) {
            next = switch (page) {
                case 0 -> new DashboardFrame(this);
                case 1 -> new ConnectionsFrame(this);
                case 2 -> new StatisticsFrame(this);
                case 3 -> new SocketTestFrame(this);
                default -> throw new IllegalArgumentException("Unknown page");
            };
            pages[page] = next;
        }
        if (current == next) return;
        if (current != null) current.setVisible(false);
        current = next;
        refresh();
        current.setVisible(true);
    }

    private void start() {
        try {
            connections = new ConnectionMonitor();
            connections.start(2000, new ConnectionFilter("", null), data -> onEdt(() -> {
                rows = data;
                connectionsAt = System.currentTimeMillis();
                if (current instanceof ConnectionsFrame || current instanceof StatisticsFrame) refresh();
            }));
        } catch (RuntimeException e) {
            onEdt(() -> {
                connectionError = true;
                if (current instanceof ConnectionsFrame) refresh();
            });
        }
        try {
            bandwidth = new BandwidthMonitor();
            bandwidth.addListener(sample -> onEdt(() -> accept(sample)));
            List<InterfaceInfo> cards = bandwidth.listInterfaces();
            InterfaceInfo preferred = cards.stream()
                    .filter(card -> card.speedBitsPerSec() > 0)
                    .filter(MonitorSession::isActiveAdapter)
                    .findFirst().orElse(cards.isEmpty() ? null : cards.get(0));
            onEdt(() -> {
                DashboardFrame dashboard = (DashboardFrame) pages[0];
                if (dashboard != null) dashboard.setCards(cards, preferred);
                availableCards = cards;
                defaultCard = preferred;
                if (preferred == null) bandwidthStatus = "Không có card mạng";
                else if (selectedCard == null) select(preferred);
                refresh();
            });
            bandwidth.start();
        } catch (RuntimeException e) {
            onEdt(() -> {
                bandwidthStatus = "Lỗi đọc card mạng";
                refresh();
            });
        }
    }

    private static boolean isActiveAdapter(InterfaceInfo card) {
        String name = card.displayName().toLowerCase(Locale.ROOT);
        if (name.contains("filter") || name.contains("virtual") || name.contains("miniport")) return false;
        try {
            NetworkInterface network = NetworkInterface.getByName(card.id());
            return network != null && network.isUp() && !network.isLoopback();
        } catch (SocketException unavailable) {
            return false;
        }
    }

    private List<InterfaceInfo> availableCards = List.of();
    private InterfaceInfo defaultCard;

    void loadCards(DashboardFrame dashboard) {
        dashboard.setCards(availableCards, availableCards.stream()
                .filter(card -> Objects.equals(selectedCard, card.id())).findFirst().orElse(defaultCard));
    }

    void select(InterfaceInfo card) {
        if (card == null || Objects.equals(selectedCard, card.id())) return;
        selectedCard = card.id();
        history.clear();
        latest = null;
        received = sent = samples = 0;
        downSum = upSum = downMax = upMax = 0;
        bandwidthStatus = "Đang lấy mẫu";
        worker.execute(() -> bandwidth.selectInterfaces(Set.of(card.id())));
        refresh();
    }

    private void accept(BandwidthSample sample) {
        if (!Objects.equals(sample.interfaceId(), selectedCard)) return;
        if (latest != null) {
            received += Math.max(0, sample.totalBytesRecv() - latest.totalBytesRecv());
            sent += Math.max(0, sample.totalBytesSent() - latest.totalBytesSent());
        }
        latest = sample;
        samples++;
        downSum += sample.recvBytesPerSec();
        upSum += sample.sentBytesPerSec();
        downMax = Math.max(downMax, sample.recvBytesPerSec());
        upMax = Math.max(upMax, sample.sentBytesPerSec());
        history.addLast(sample);
        while (!history.isEmpty() && history.getFirst().timestampMillis() < sample.timestampMillis() - HISTORY_MILLIS) {
            history.removeFirst();
        }
        socket.send(sample);
        if (current instanceof DashboardFrame || current instanceof StatisticsFrame) refresh();
    }

    private SocketTestFrame socketPage() { return (SocketTestFrame) pages[3]; }

    private void refresh() {
        if (current instanceof DashboardFrame dashboard) {
            dashboard.showSample(latest, received, sent, downMax, upMax, bandwidthStatus);
            dashboard.showHistory(List.copyOf(history));
        } else if (current instanceof ConnectionsFrame table) {
            table.setConnections(rows);
            table.showStatus(connectionError ? "Lỗi đọc kết nối" : connectionsAt == 0 ? "Đang quét" : "Dữ liệu thật", connectionsAt);
        } else if (current instanceof StatisticsFrame statistics) {
            statistics.showData(List.copyOf(history), rows, received, sent,
                    samples == 0 ? 0 : downSum / samples, samples == 0 ? 0 : upSum / samples, downMax, upMax);
            statistics.showStatus(latest == null ? bandwidthStatus : "Dữ liệu thật",
                    latest == null ? 0 : latest.timestampMillis());
        }
    }

    void runSocketAction(Runnable action) { worker.execute(action); }

    void close() {
        if (closed) return;
        closed = true;
        for (MonitorFrame frame : pages) {
            if (frame != null) frame.dispose();
        }
        worker.execute(() -> {
            socket.close();
            if (connections != null) connections.close();
            if (bandwidth != null) bandwidth.stop();
        });
        worker.shutdown();
    }

    private void onEdt(Runnable action) {
        SwingUtilities.invokeLater(() -> {
            if (!closed) action.run();
        });
    }
}
