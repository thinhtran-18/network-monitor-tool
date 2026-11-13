package com.nhom5.netmonitor;

import com.nhom5.netmonitor.core.ConnectionFilter;
import com.nhom5.netmonitor.core.ConnectionInfo;
import com.nhom5.netmonitor.monitor.connection.ConnectionMonitor;

import java.util.List;

/** Entry point for the Network Monitor Tool. */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        String ip = args.length > 0 ? args[0] : "";
        Integer port = args.length > 1 ? Integer.valueOf(args[1]) : null;
        try (ConnectionMonitor monitor = new ConnectionMonitor()) {
            List<ConnectionInfo> connections = monitor.scan(new ConnectionFilter(ip, port));
            System.out.printf("ConnectionMonitor | ket noi: %d | bo loc IP=%s, port=%s%n",
                    connections.size(), ip.isBlank() ? "*" : ip, port == null ? "*" : port);
            System.out.printf("%-6s %-39s %-7s %-39s %-7s %-13s %s%n",
                    "PROTO", "LOCAL IP", "PORT", "REMOTE IP", "PORT", "STATE", "PID");
            for (ConnectionInfo c : connections) {
                System.out.printf("%-6s %-39s %-7d %-39s %-7d %-13s %d%n",
                        c.protocol(), c.localAddress(), c.localPort(), c.remoteAddress(),
                        c.remotePort(), c.state(), c.pid());
            }
        } catch (NumberFormatException exception) {
            System.err.println("Cach dung: App [IP] [PORT]");
            System.exit(2);
        } catch (RuntimeException exception) {
            System.err.println("Khong the doc danh sach ket noi: " + exception.getMessage());
            System.exit(1);
        }
    }
}
