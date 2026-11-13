package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.ConnectionInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/** Connections page with a searchable JTable backed by OS snapshots. */
public final class ConnectionsFrame extends MonitorFrame {
    private final JTable table;
    private final ConnectionTableModel tableModel;
    private final TableRowSorter<ConnectionTableModel> sorter;
    private final JTextField search = new JTextField();
    private String protocolFilter = "All";
    private String stateFilter = "Tất cả trạng thái";
    private int totalConnectionCount;
    private final JLabel countLabel = UiTheme.label("", 0, 0, 170, 34, 12, UiTheme.MUTED, false);

    ConnectionsFrame(MonitorSession session) {
        super(session, "Connections", 1, 1440, 900);
        tableModel = new ConnectionTableModel();
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        addToolbar();
        styleTable();

        SurfacePanel tableCard = new SurfacePanel(264, 176, 1152, 700);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBounds(1, 1, 1150, 698);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.CARD);
        scroll.setBackground(UiTheme.CARD);
        tableCard.add(scroll);
        addPageComponent(tableCard);
        applyFilters();
    }

    /**
     * Replaces the table snapshot. This can be called by a monitor listener from
     * a worker thread; all model and counter changes are moved onto Swing's EDT.
     */
    public void setConnections(List<ConnectionInfo> connections) {
        List<ConnectionInfo> snapshot = List.copyOf(connections);
        Runnable update = () -> {
            totalConnectionCount = snapshot.size();
            tableModel.setRows(snapshot);
            applyFilters();
        };
        if (SwingUtilities.isEventDispatchThread()) update.run();
        else SwingUtilities.invokeLater(update);
    }

    private void addToolbar() {
        SurfacePanel bar = new SurfacePanel(264, 88, 1152, 72);
        search.setText("");
        search.putClientProperty("JTextField.placeholderText", "Tìm theo địa chỉ IP...");
        search.setBounds(18, 18, 320, 36);
        styleField(search);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        bar.add(search);
        String[] protocols = {"All", "TCP", "UDP"};
        int x = 354;
        for (String value : protocols) {
            JButton button = button(value.equals("All") ? "Tất cả" : value, x, 18, value.equals("All") ? 82 : 72, 36,
                    value.equals(protocolFilter));
            button.addActionListener(e -> { protocolFilter = value; applyFilters(); updateProtocolButtons(bar); });
            bar.add(button); x += value.equals("All") ? 82 : 72;
        }
        JComboBox<String> states = new JComboBox<>(new String[]{"Tất cả trạng thái", "ESTABLISHED", "LISTEN", "TIME_WAIT", "CLOSE_WAIT"});
        states.setBounds(604, 18, 200, 36); states.setBackground(UiTheme.INPUT); states.setForeground(UiTheme.WHITE);
        states.setFont(new Font("Segoe UI", Font.PLAIN, 13)); states.setFocusable(false);
        states.addActionListener(e -> { stateFilter = (String) states.getSelectedItem(); applyFilters(); });
        bar.add(states);
        countLabel.setBounds(830, 18, 168, 36); bar.add(countLabel);
        JButton refresh = button("Xóa bộ lọc", 1012, 18, 122, 36, false);
        refresh.addActionListener(e -> {
            search.setText(""); states.setSelectedIndex(0); protocolFilter = "All";
            applyFilters(); updateProtocolButtons(bar);
        });
        bar.add(refresh);
        addPageComponent(bar);
    }

    private void updateProtocolButtons(SurfacePanel bar) {
        for (java.awt.Component component : bar.getComponents()) {
            if (component instanceof JButton b && (b.getText().equals("Tất cả") || b.getText().equals("TCP") || b.getText().equals("UDP"))) {
                boolean selected = (protocolFilter.equals("All") && b.getText().equals("Tất cả")) || protocolFilter.equals(b.getText());
                b.setBackground(selected ? UiTheme.BLUE : UiTheme.INPUT);
            }
        }
        bar.repaint();
    }

    private void applyFilters() {
        if (sorter == null) return;
        String text = search.getText().trim();
        sorter.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends ConnectionTableModel, ? extends Integer> entry) {
                String protocol = entry.getStringValue(0);
                String state = entry.getStringValue(5);
                boolean matchesProtocol = protocolFilter.equals("All") || protocol.startsWith(protocolFilter);
                boolean matchesState = stateFilter.equals("Tất cả trạng thái") || stateFilter.equals(state);
                boolean matchesText = text.isEmpty();
                for (int i = 1; !matchesText && i < 7; i++) matchesText = entry.getStringValue(i).toLowerCase().contains(text.toLowerCase());
                return matchesProtocol && matchesState && matchesText;
            }
        });
        countLabel.setText("Hiển thị " + table.getRowCount() + " / " + totalConnectionCount + " kết nối");
    }

    private void styleTable() {
        table.setRowHeight(40);
        table.setFont(new Font("Consolas", Font.PLAIN, 13));
        table.setForeground(UiTheme.WHITE);
        table.setBackground(UiTheme.CARD);
        table.setGridColor(UiTheme.EDGE);
        table.setShowHorizontalLines(true); table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(34, 53, 82)); table.setSelectionForeground(UiTheme.WHITE);
        table.setFillsViewportHeight(true); table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(1, 44));
        table.getTableHeader().setBackground(UiTheme.SIDEBAR); table.getTableHeader().setForeground(UiTheme.MUTED);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                cell.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                cell.setBackground(selected ? new Color(34, 53, 82) : (row % 2 == 0 ? UiTheme.CARD : new Color(22, 35, 56)));
                cell.setForeground(UiTheme.WHITE);
                if (column == 2 || column == 4 || column == 6) cell.setHorizontalAlignment(JLabel.RIGHT);
                else cell.setHorizontalAlignment(JLabel.LEFT);
                if (column == 0) { cell.setFont(new Font("Segoe UI", Font.BOLD, 12)); cell.setForeground(String.valueOf(value).startsWith("UDP") ? UiTheme.TEAL : UiTheme.UPLOAD); }
                if (column == 5) {
                    cell.setFont(new Font("Consolas", Font.BOLD, 11));
                    cell.setForeground(String.valueOf(value).equals("ESTABLISHED") ? UiTheme.GREEN : UiTheme.AMBER);
                }
                return cell;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        int[] widths = {105, 220, 130, 220, 130, 210, 100};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private static JButton button(String text, int x, int y, int w, int h, boolean selected) {
        JButton b = new JButton(text); b.setBounds(x, y, w, h); b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12)); b.setForeground(UiTheme.WHITE);
        b.setBackground(selected ? UiTheme.BLUE : UiTheme.INPUT); b.setBorder(BorderFactory.createLineBorder(UiTheme.EDGE));
        return b;
    }

    private static void styleField(JTextField field) {
        field.setBackground(UiTheme.INPUT); field.setForeground(UiTheme.WHITE); field.setCaretColor(UiTheme.WHITE);
        field.setFont(new Font("Consolas", Font.PLAIN, 13)); field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.EDGE), BorderFactory.createEmptyBorder(0, 12, 0, 8)));
    }

    public static void main(String[] args) { MonitorSession.launch(1); }
}
