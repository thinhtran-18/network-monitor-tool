package com.nhom5.netmonitor.core;

import java.util.Locale;

/** Optional IP and port filters; a match on either endpoint is accepted. */
public record ConnectionFilter(String ip, Integer port) {
    public ConnectionFilter {
        ip = ip == null ? "" : ip.trim().toLowerCase(Locale.ROOT);
        if (port != null && (port < 0 || port > 65535)) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
    }

    public static ConnectionFilter all() {
        return new ConnectionFilter("", null);
    }

    public boolean matches(ConnectionInfo row) {
        boolean ipMatches = ip.isEmpty()
                || row.localAddress().toLowerCase(Locale.ROOT).contains(ip)
                || row.remoteAddress().toLowerCase(Locale.ROOT).contains(ip);
        boolean portMatches = port == null
                || row.localPort() == port || row.remotePort() == port;
        return ipMatches && portMatches;
    }
}
