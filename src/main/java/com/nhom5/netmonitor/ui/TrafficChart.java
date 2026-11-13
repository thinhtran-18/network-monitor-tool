package com.nhom5.netmonitor.ui;

import com.nhom5.netmonitor.core.BandwidthSample;
import java.awt.BasicStroke;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/** Binds live samples to the chart area of the existing Swing cards. */
final class TrafficChart extends ChartPanel {
    private final TimeSeries download = new TimeSeries("Download");
    private final TimeSeries upload = new TimeSeries("Upload");

    TrafficChart(int x, int y, int width, int height) {
        super(ChartFactory.createTimeSeriesChart(null, null, "MB/s",
                new TimeSeriesCollection(), false, true, false));
        setBounds(x, y, width, height);
        JFreeChart chart = getChart();
        chart.setBackgroundPaint(UiTheme.CARD);
        XYPlot plot = chart.getXYPlot();
        TimeSeriesCollection data = new TimeSeriesCollection(download);
        data.addSeries(upload);
        plot.setDataset(data);
        plot.setBackgroundPaint(UiTheme.CARD);
        plot.setOutlineVisible(false);
        plot.setDomainGridlinePaint(UiTheme.EDGE);
        plot.setRangeGridlinePaint(UiTheme.EDGE);
        plot.getDomainAxis().setTickLabelPaint(UiTheme.MUTED);
        plot.getRangeAxis().setTickLabelPaint(UiTheme.MUTED);
        plot.getRangeAxis().setLabelPaint(UiTheme.MUTED);
        plot.getRenderer().setSeriesPaint(0, UiTheme.TEAL);
        plot.getRenderer().setSeriesPaint(1, UiTheme.UPLOAD);
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(2));
        setMouseZoomable(false);
    }

    void showSamples(List<BandwidthSample> history, long duration) {
        long end = history.isEmpty() ? System.currentTimeMillis() : history.get(history.size() - 1).timestampMillis();
        download.setNotify(false);
        upload.setNotify(false);
        download.clear();
        upload.clear();
        for (BandwidthSample sample : history) {
            if (sample.timestampMillis() < end - duration) continue;
            Millisecond time = new Millisecond(new Date(sample.timestampMillis()));
            download.addOrUpdate(time, sample.recvBytesPerSec() / 1_000_000.0);
            upload.addOrUpdate(time, sample.sentBytesPerSec() / 1_000_000.0);
        }
        download.setNotify(true);
        upload.setNotify(true);
        getChart().getXYPlot().getDomainAxis().setRange(end - duration, end);
    }

    static String rate(double bytesPerSecond) {
        return String.format(Locale.ROOT, "%.3f MB/s", bytesPerSecond / 1_000_000);
    }

    static String volume(long bytes) {
        return bytes >= 1_000_000_000
                ? String.format(Locale.ROOT, "%.2f GB", bytes / 1_000_000_000.0)
                : String.format(Locale.ROOT, "%.2f MB", bytes / 1_000_000.0);
    }
}
