/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.nhom5.netmonitor.network;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
/**
 *
 * @author Admin
 */
public class Agent {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        System.out.println("[AGENT] Đang kết nối tới Server tại " + SERVER_IP + ":" + SERVER_PORT + "...");
        
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            
            System.out.println("[AGENT] Kết nối thành công! Bắt đầu gửi dữ liệu định kỳ...");
            
            for (int i = 1; i <= 10; i++) {
                String jsonPayload = String.format(
                    "{\"type\": \"METRICS\", \"agentId\": \"AGENT_01\", \"seq\": %d, \"bytesSent\": %d, \"bytesRecv\": %d}",
                    i, i * 1024, i * 2048
                );
                
                writer.println(jsonPayload);
                System.out.println("[AGENT ĐÃ GỬI]: " + jsonPayload);
                
                Thread.sleep(1000);
            }
            
            System.out.println("[AGENT] Đã hoàn thành gửi dữ liệu mẫu.");
        } catch (InterruptedException e) {
            System.out.println("[AGENT] Tiến trình bị gián đoạn.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("[AGENT] Lỗi kết nối tới Server: " + e.getMessage());
        }
    }
    
}
