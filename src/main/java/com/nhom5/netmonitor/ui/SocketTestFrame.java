package com.nhom5.netmonitor.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Socket test page layout; connection fields and log are demonstration UI only. */
public final class SocketTestFrame extends MonitorFrame {
    private final JLabel statusText = UiTheme.label("Chưa kết nối", 20, 46, 220, 30, 19, UiTheme.MUTED, true);
    private final JLabel sidebarStatus = UiTheme.label("●  Chưa kết nối", 218, 350, 115, 28, 11, UiTheme.MUTED, false);

    public SocketTestFrame() {
        super("Socket Test", 3, 1512, 979);
        addConfiguration();
        addSummaryCards();
        addLogPanel();
    }

    private void addConfiguration() {
        SurfacePanel config = new SurfacePanel(264, 88, 360, 400);
        config.add(UiTheme.label("Cấu hình kết nối", 20, 20, 300, 28, 17, UiTheme.WHITE, true));
        config.add(UiTheme.label("Chế độ", 20, 55, 120, 24, 12, UiTheme.MUTED, false));
        JPanel mode = new JPanel(null); mode.setBounds(20, 80, 320, 36); mode.setBackground(UiTheme.INPUT); mode.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE));
        JRadioButton server = new JRadioButton("Server"), client = new JRadioButton("Client", true);
        for (JRadioButton radio : new JRadioButton[]{server, client}) {
            radio.setBounds(radio == server ? 0 : 160, 0, 160, 36); radio.setHorizontalAlignment(JLabel.CENTER);
            radio.setForeground(radio.isSelected() ? Color.WHITE : UiTheme.MUTED); radio.setBackground(radio.isSelected() ? UiTheme.BLUE : UiTheme.INPUT);
            radio.setFont(new Font("Segoe UI", Font.PLAIN, 13)); radio.setFocusPainted(false); mode.add(radio);
        }
        ButtonGroup group = new ButtonGroup(); group.add(server); group.add(client);
        server.addActionListener(e -> { server.setBackground(UiTheme.BLUE); server.setForeground(Color.WHITE); client.setBackground(UiTheme.INPUT); client.setForeground(UiTheme.MUTED); });
        client.addActionListener(e -> { client.setBackground(UiTheme.BLUE); client.setForeground(Color.WHITE); server.setBackground(UiTheme.INPUT); server.setForeground(UiTheme.MUTED); });
        config.add(mode);
        config.add(UiTheme.label("Địa chỉ IP", 20, 126, 200, 22, 12, UiTheme.MUTED, false));
        JTextField ip = field("192.168.1.30"); ip.setBounds(20, 153, 320, 36); config.add(ip);
        config.add(UiTheme.label("Port", 20, 194, 200, 22, 12, UiTheme.MUTED, false));
        JTextField port = field("5000"); port.setBounds(20, 221, 320, 36); config.add(port);
        JButton connect = actionButton("Connect", 20, 285, 150, 36, UiTheme.BLUE, Color.WHITE);
        JButton disconnect = actionButton("Disconnect", 186, 285, 154, 36, UiTheme.CARD, new Color(151, 66, 76));
        connect.addActionListener(e -> { statusText.setText("Đã kết nối (demo)"); statusText.setForeground(UiTheme.GREEN); sidebarStatus.setText("●  Đang kết nối"); sidebarStatus.setForeground(UiTheme.GREEN); });
        disconnect.addActionListener(e -> { statusText.setText("Chưa kết nối"); statusText.setForeground(UiTheme.MUTED); sidebarStatus.setText("●  Chưa kết nối"); sidebarStatus.setForeground(UiTheme.MUTED); });
        config.add(connect); config.add(disconnect);
        config.add(UiTheme.label("Trạng thái", 20, 338, 120, 30, 13, UiTheme.MUTED, false));
        sidebarStatus.setBounds(218, 350, 115, 28); config.add(sidebarStatus);
        addPageComponent(config);
    }

    private void addSummaryCards() {
        addPageComponent(summaryCard(640, "Trạng thái", "Chưa kết nối", "--", 0));
        addPageComponent(summaryCard(928, "Tốc độ truyền", "--", "Trung bình -- MB/s", 1));
        addPageComponent(summaryCard(1216, "Độ trễ", "--", "Cao nhất -- ms", 2));
    }

    private SurfacePanel summaryCard(int x, String title, String value, String footer, int type) {
        SurfacePanel card = new SurfacePanel(x, 88, 256, 112);
        card.add(UiTheme.label(title, 20, 18, 210, 20, 12, UiTheme.MUTED, false));
        if (type == 0) card.add(statusText);
        else card.add(UiTheme.label(value, 20, 48, 210, 30, 23, UiTheme.MUTED, true));
        card.add(UiTheme.label(footer, 20, 78, 220, 22, 12, UiTheme.MUTED, false));
        return card;
    }

    private void addLogPanel() {
        SurfacePanel log = new SurfacePanel(640, 216, 848, 739);
        log.add(UiTheme.label("Nhật ký truyền dữ liệu", 20, 18, 230, 28, 16, UiTheme.WHITE, true));
        log.add(UiTheme.label("0 bản ghi", 250, 18, 100, 28, 12, UiTheme.MUTED, false));
        JButton clear = actionButton("Xóa nhật ký", 660, 18, 164, 36, UiTheme.CARD, UiTheme.MUTED);
        log.add(clear);
        DefaultTableModel model = new DefaultTableModel(new Object[0][0], new String[]{"Thời gian", "Loại", "Nội dung", "Dung lượng", "Tốc độ"}) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(36); table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(UiTheme.CARD); table.setForeground(UiTheme.WHITE); table.setGridColor(UiTheme.EDGE);
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(1, 40));
        table.getTableHeader().setBackground(UiTheme.SIDEBAR); table.getTableHeader().setForeground(UiTheme.MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table); scroll.setBounds(1, 64, 846, 674); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.CARD); scroll.setBackground(UiTheme.CARD); log.add(scroll);
        JPanel empty = new JPanel(null); empty.setBounds(180, 315, 488, 104); empty.setOpaque(false);
        JPanel icon = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                super.paintComponent(graphics);
                java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
                g.setColor(UiTheme.MUTED); g.setStroke(new java.awt.BasicStroke(2f));
                g.drawRoundRect(10, 7, 28, 20, 4, 4); g.drawLine(12, 10, 19, 18); g.drawLine(36, 10, 29, 18);
                g.drawLine(19, 18, 29, 18); g.dispose();
            }
        };
        icon.setBounds(220, 0, 48, 40); icon.setOpaque(false); empty.add(icon);
        JLabel message = UiTheme.label("Chưa có dữ liệu truyền. Nhấn Connect để bắt đầu.", 0, 56, 488, 28, 12, UiTheme.MUTED, false);
        message.setHorizontalAlignment(JLabel.CENTER); empty.add(message); log.add(empty);
        clear.addActionListener(e -> { model.setRowCount(0); });
        addPageComponent(log);
    }

    private static JTextField field(String text) {
        JTextField field = new JTextField(text); field.setBackground(UiTheme.INPUT); field.setForeground(UiTheme.WHITE);
        field.setCaretColor(UiTheme.WHITE); field.setFont(new Font("Consolas", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.EDGE), BorderFactory.createEmptyBorder(0, 12, 0, 8)));
        return field;
    }

    private static JButton actionButton(String text, int x, int y, int w, int h, Color bg, Color fg) {
        JButton b = new JButton(text); b.setBounds(x, y, w, h); b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13)); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE)); return b;
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new SocketTestFrame().setVisible(true)); }
}
