package com.nhom5.netmonitor.monitor.connection;

import com.nhom5.netmonitor.core.ConnectionFilter;
import com.nhom5.netmonitor.core.ConnectionInfo;
import oshi.SystemInfo;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Reads OS connection snapshots and optionally publishes them on a background thread. */
public final class ConnectionMonitor implements AutoCloseable {
    private final OperatingSystem operatingSystem;
    private ScheduledExecutorService executor;

    public ConnectionMonitor() {
        this(new SystemInfo().getOperatingSystem());
    }

    ConnectionMonitor(OperatingSystem operatingSystem) {
        this.operatingSystem = Objects.requireNonNull(operatingSystem);
    }

    public List<ConnectionInfo> scan(ConnectionFilter filter) {
        Objects.requireNonNull(filter, "filter");
        InternetProtocolStats stats = operatingSystem.getInternetProtocolStats();
        List<ConnectionInfo> rows = new ArrayList<>();
        for (InternetProtocolStats.IPConnection connection : stats.getConnections()) {
            String protocol = connection.getType().toUpperCase();
            String state = protocol.startsWith("UDP")
                    ? "N/A" : connection.getState() == null ? "UNKNOWN" : connection.getState().name();
            ConnectionInfo row = new ConnectionInfo(protocol,
                    address(connection.getLocalAddress()), connection.getLocalPort(),
                    address(connection.getForeignAddress()), connection.getForeignPort(),
                    state, connection.getowningProcessId());
            if (filter.matches(row)) rows.add(row);
        }
        rows.sort(Comparator.comparing(ConnectionInfo::protocol)
                .thenComparing(ConnectionInfo::localAddress)
                .thenComparingInt(ConnectionInfo::localPort)
                .thenComparing(ConnectionInfo::remoteAddress)
                .thenComparingInt(ConnectionInfo::remotePort));
        return List.copyOf(rows);
    }

    /** Listener calls execute on the monitor thread. */
    public synchronized void start(long intervalMillis, ConnectionFilter filter,
                                   Consumer<List<ConnectionInfo>> listener) {
        if (intervalMillis < 250) throw new IllegalArgumentException("Interval must be at least 250 ms");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(listener, "listener");
        if (executor != null && !executor.isShutdown()) throw new IllegalStateException("Monitor already started");
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "connection-monitor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                listener.accept(scan(filter));
            } catch (RuntimeException exception) {
                System.err.println("Connection scan failed: " + exception.getMessage());
            }
        }, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void close() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private static String address(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "*";
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException exception) {
            return "UNKNOWN";
        }
    }
}
