package com.nhom5.netmonitor.core;

/** Immutable snapshot of one TCP or UDP connection/socket row. */
public record ConnectionInfo(String protocol, String localAddress, int localPort,
                             String remoteAddress, int remotePort, String state, int pid) {
}
