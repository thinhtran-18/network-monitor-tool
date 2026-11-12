package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.ConnectionInfo;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/** Table adapter for immutable connection snapshots shared by the monitor and UI. */
public final class ConnectionTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
        "PROTOCOL", "LOCAL IP", "LOCAL PORT", "REMOTE IP", "REMOTE PORT", "STATE", "PID"
    };

    private List<ConnectionInfo> rows;

    public ConnectionTableModel() {
        this(List.of());
    }

    public ConnectionTableModel(List<ConnectionInfo> initialRows) {
        rows = List.copyOf(Objects.requireNonNull(initialRows, "initialRows"));
    }

    /**
     * Replaces the displayed snapshot. Safe to call from any thread; the table model
     * change and its Swing event are always applied on the Event Dispatch Thread.
     */
    public void setRows(List<ConnectionInfo> newRows) {
        List<ConnectionInfo> snapshot = List.copyOf(Objects.requireNonNull(newRows, "newRows"));
        Runnable update = () -> {
            rows = snapshot;
            fireTableDataChanged();
        };
        if (SwingUtilities.isEventDispatchThread()) update.run();
        else SwingUtilities.invokeLater(update);
    }

    public ConnectionInfo getConnectionAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    @Override public int getRowCount() { return rows.size(); }

    @Override public int getColumnCount() { return COLUMNS.length; }

    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 2 || columnIndex == 4 || columnIndex == 6 ? Integer.class : String.class;
    }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectionInfo row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> row.protocol();
            case 1 -> row.localAddress();
            case 2 -> row.localPort();
            case 3 -> row.remoteAddress();
            case 4 -> row.remotePort();
            case 5 -> row.state();
            case 6 -> row.pid();
            default -> throw new IndexOutOfBoundsException("Unknown column: " + columnIndex);
        };
    }
}
