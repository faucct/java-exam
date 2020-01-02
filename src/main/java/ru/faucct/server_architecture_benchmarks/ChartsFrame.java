package ru.faucct.server_architecture_benchmarks;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class ChartsFrame extends JFrame {

    private final JFreeChart clientRequestDuration;
    private final JFreeChart processingDuration;
    private final JFreeChart serverRequestDuration;

    public ChartsFrame(List<Benchmark.Result> results, VariatingParameter variatingParameter) {
        final String xAxis;
        final ToDoubleFunction<Benchmark.Result> key;
        switch (variatingParameter) {
            case ARRAY_SIZE:
                xAxis = "Array size";
                key = result -> result.config.arraySize;
                break;
            case CLIENTS_NUMBER:
                xAxis = "Clients number";
                key = result -> result.config.clientsNumber;
                break;
            case DELAY_BETWEEN_REQUESTS:
                xAxis = "Delay between requests (ms)";
                key = result -> result.config.delayNanoseconds / 1e6;
                break;
            default:
                throw new RuntimeException(variatingParameter.toString());
        }
        XYSeries series1 = new XYSeries("");
        XYSeries series2 = new XYSeries("");
        XYSeries series3 = new XYSeries("");
        for (Benchmark.Result result : results) {
            series1.add(key.applyAsDouble(result), result.averageClientRequestDuration / 1e6);
            series2.add(key.applyAsDouble(result), result.averageProcessingDuration / 1e6);
            series3.add(key.applyAsDouble(result), result.averageServerRequestDuration / 1e6);
        }

        setLayout(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = constraints.weighty = 1;
        clientRequestDuration = ChartFactory.createXYLineChart("", xAxis, "Client request duration (ms)", new XYSeriesCollection(series1));
        add(new ChartPanel(clientRequestDuration), constraints);
        processingDuration = ChartFactory.createXYLineChart("", xAxis, "Processing duration (ms)", new XYSeriesCollection(series2));
        add(new ChartPanel(processingDuration), constraints);
        serverRequestDuration = ChartFactory.createXYLineChart("", xAxis, "Server request duration (ms)", new XYSeriesCollection(series3));
        add(new ChartPanel(serverRequestDuration), constraints);
        setVisible(true);
        setSize(1200, 400);
    }

    void save(File directory) throws IOException {
        ChartUtils.saveChartAsPNG(new File(directory, "client_request_duration.png"), clientRequestDuration, 400, 400);
        ChartUtils.saveChartAsPNG(new File(directory, "processing_duration.png"), processingDuration, 400, 400);
        ChartUtils.saveChartAsPNG(new File(directory, "server_request_duration.png"), serverRequestDuration, 400, 400);
    }
}
