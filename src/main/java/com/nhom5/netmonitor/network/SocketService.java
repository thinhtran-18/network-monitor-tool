package com.nhom5.netmonitor.network;

import com.google.gson.Gson;
import com.nhom5.netmonitor.core.BandwidthSample;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Sends card measurements over TCP as one UTF-8 JSON record per line. */
public final class SocketService implements AutoCloseable {
    private static final int MAX_CLIENTS = 16;
    private static final int MAX_RECORD_BYTES = 8192;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "socket-service");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Socket> clients = new HashSet<>();
    private final AtomicBoolean sending = new AtomicBoolean();
    private final Consumer<String> status;
    private final Consumer<Event> log;
    private final Gson gson = new Gson();
    private ServerSocket server;
    private Socket agent;
    private boolean closed;
    private long generation;

    public record Metrics(String type, int version, long timestamp, String interfaceId,
                          double recvBytesPerSec, double sentBytesPerSec) { }
    public record Event(String type, String message, int bytes, Metrics metrics) { }

    public SocketService(Consumer<String> status, Consumer<Event> log) {
        this.status = status;
        this.log = log;
    }

    public synchronized void startServer(int port) {
        stop();
        long run = generation;
        status.accept("Đang khởi động server");
        executor.execute(() -> listen(port, run));
    }

    private void listen(int port, long run) {
        try (ServerSocket listener = new ServerSocket()) {
            synchronized (this) {
                if (!isCurrent(run)) return;
                server = listener;
            }
            listener.bind(new InetSocketAddress(port));
            publishStatus(run, "Server đang lắng nghe cổng " + port);
            while (!listener.isClosed()) {
                Socket client = listener.accept();
                synchronized (this) {
                    if (!isCurrent(run) || clients.size() >= MAX_CLIENTS) {
                        client.close();
                        continue;
                    }
                    clients.add(client);
                    log.accept(new Event("Kết nối", client.getRemoteSocketAddress().toString(), 0, null));
                    executor.execute(() -> readClient(client, run));
                }
            }
        } catch (IOException e) {
            publishStatus(run, "Lỗi server: " + e.getMessage());
        }
    }

    private void readClient(Socket client, long run) {
        String peer = client.getRemoteSocketAddress().toString();
        try (client; var input = new BufferedInputStream(client.getInputStream())) {
            var record = new ByteArrayOutputStream();
            for (int next; (next = input.read()) != -1;) {
                if (next == '\n') {
                    receive(record, peer, run);
                    record.reset();
                } else {
                    if (record.size() >= MAX_RECORD_BYTES) throw new IOException("Bản tin vượt giới hạn");
                    record.write(next);
                }
            }
        } catch (IOException e) {
            publishEvent(run, new Event("Lỗi đọc", peer + " · " + e.getMessage(), 0, null));
        } finally {
            synchronized (this) {
                clients.remove(client);
                publishEvent(run, new Event("Ngắt kết nối", peer, 0, null));
            }
        }
    }

    private void receive(ByteArrayOutputStream record, String peer, long run) {
        try {
            Metrics sample = gson.fromJson(record.toString(StandardCharsets.UTF_8), Metrics.class);
            if (sample == null || !"METRICS".equals(sample.type()) || sample.version() != 1
                    || sample.interfaceId() == null || !Double.isFinite(sample.recvBytesPerSec())
                    || !Double.isFinite(sample.sentBytesPerSec()) || sample.recvBytesPerSec() < 0
                    || sample.sentBytesPerSec() < 0) throw new IllegalArgumentException();
            publishEvent(run, new Event("Nhận", peer + " · " + sample.interfaceId(), record.size() + 1, sample));
        } catch (RuntimeException invalid) {
            publishEvent(run, new Event("Bản tin lỗi", peer, record.size() + 1, null));
        }
    }

    public synchronized void connect(String host, int port) {
        stop();
        long run = generation;
        status.accept("Đang kết nối");
        executor.execute(() -> {
            Socket connection = new Socket();
            try {
                synchronized (this) {
                    if (!isCurrent(run)) {
                        connection.close();
                        return;
                    }
                    agent = connection;
                }
                connection.connect(new InetSocketAddress(host, port), 5000);
                publishStatus(run, "Đã kết nối " + host + ":" + port);
            } catch (IOException | IllegalArgumentException e) {
                closeQuietly(connection);
                publishStatus(run, "Lỗi kết nối: " + e.getMessage());
            }
        });
    }

    public synchronized void send(BandwidthSample sample) {
        Socket connection = agent;
        if (closed || connection == null || !connection.isConnected() || connection.isClosed()
                || !sending.compareAndSet(false, true)) return;
        long run = generation;
        executor.execute(() -> {
            try {
                Metrics metrics = new Metrics("METRICS", 1, sample.timestampMillis(), sample.interfaceId(),
                        sample.recvBytesPerSec(), sample.sentBytesPerSec());
                byte[] record = (gson.toJson(metrics) + "\n").getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(record);
                publishEvent(run, new Event("Gửi", metrics.interfaceId(), record.length, metrics));
            } catch (IOException e) {
                closeQuietly(connection);
                publishStatus(run, "Mất kết nối: " + e.getMessage());
            } finally {
                sending.set(false);
            }
        });
    }

    private boolean isCurrent(long run) { return !closed && generation == run; }

    private synchronized void publishStatus(long run, String message) {
        if (isCurrent(run)) status.accept(message);
    }

    private synchronized void publishEvent(long run, Event event) {
        if (isCurrent(run)) log.accept(event);
    }

    public synchronized void stop() {
        generation++;
        closeQuietly(server);
        server = null;
        closeQuietly(agent);
        agent = null;
        for (Socket client : clients) closeQuietly(client);
        clients.clear();
        if (!closed) status.accept("Đã ngắt kết nối");
    }

    private static void closeQuietly(java.io.Closeable resource) {
        if (resource == null) return;
        try { resource.close(); } catch (IOException ignored) { }
    }

    @Override public synchronized void close() {
        closed = true;
        stop();
        executor.shutdownNow();
    }
}
