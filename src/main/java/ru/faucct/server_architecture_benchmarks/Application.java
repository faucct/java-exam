package ru.faucct.server_architecture_benchmarks;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Application {
    private class ValueOrRange {
        boolean range;
        int min, max, step = 1;
        final JLabel minLabel = new JLabel("Min"), maxLabel = new JLabel("Max"), stepLabel = new JLabel("Step");
        final JSpinner valueField, minField, maxField, stepField;
        final JPanel fields;
        final JRadioButton radioButton;

        ValueOrRange(String label, Supplier<SpinnerNumberModel> numberModel, Runnable sync) {
            this.valueField = new JSpinner(numberModel.get());
            valueField.addChangeListener(e -> {
                if (valueField.isVisible()) {
                    min = max = (int) valueField.getValue();
                    sync();
                }
            });
            this.minField = new JSpinner(numberModel.get());
            minField.addChangeListener(e -> {
                if (minField.isVisible()) {
                    min = (int) minField.getValue();
                    sync();
                }
            });
            this.maxField = new JSpinner(numberModel.get());
            maxField.addChangeListener(e -> {
                if (maxField.isVisible()) {
                    max = (int) maxField.getValue();
                    sync();
                }
            });
            this.stepField = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
            stepField.addChangeListener(e -> {
                if (stepField.isVisible()) {
                    step = (int) stepField.getValue();
                    sync();
                }
            });
            fields = new JPanel();
            final GroupLayout fieldsLayout = new GroupLayout(fields);
            final GroupLayout.SequentialGroup sequentialGroup = fieldsLayout.createSequentialGroup();
            final GroupLayout.ParallelGroup parallelGroup = fieldsLayout.createParallelGroup();
            for (JComponent component : new JComponent[]{valueField, minLabel, minField, maxLabel, maxField, stepLabel, stepField}) {
                sequentialGroup.addComponent(component);
                parallelGroup.addComponent(component);
            }
            fieldsLayout.setHorizontalGroup(sequentialGroup);
            fieldsLayout.setVerticalGroup(fieldsLayout.createSequentialGroup().addGroup(parallelGroup));
            fields.setLayout(fieldsLayout);
            radioButton = new JRadioButton(label);
            radioButton.addActionListener(e -> sync.run());
        }

        void sync() {
            range = radioButton.isSelected();

            valueField.setVisible(!range);
            valueField.setValue(min);
            valueField.setEnabled(!running);

            minLabel.setVisible(range);
            minField.setVisible(range);
            minField.setValue(min);
            minField.setEnabled(!running);

            maxLabel.setVisible(range);
            maxField.setVisible(range);
            maxField.setValue(max);
            maxField.setEnabled(!running);

            stepLabel.setVisible(range);
            stepField.setVisible(range);
            stepField.setValue(step);
            stepField.setEnabled(!running);

            radioButton.setSelected(range);
            radioButton.setEnabled(!running);
        }

        private IntStream range() {
            return IntStream.rangeClosed(0, (max - min) / step).map(i -> min + i * step);
        }
    }

    private boolean running;
    private final JSpinner requestsNumberField;
    private final JRadioButton
            threadPerClientArchitecture = new JRadioButton("thread-per-client"),
            blockingPoolArchitecture = new JRadioButton("blocking"),
            nonBlockingPoolArchitecture = new JRadioButton("non blocking"),
            asynchronousPoolArchitecture = new JRadioButton("asynchronous");
    private final ValueOrRange arraySize, clientsNumber, delayBetweenRequests;
    private final JButton runButton = new JButton("Run");
    private final JProgressBar progressBar = new JProgressBar();

    public static void main(String[] args) {
        Application application = new Application();
    }

    Application() {
        JFrame pane = new JFrame();//creating instance of JFrame

        final GridBagLayout layout = new GridBagLayout();
        pane.setLayout(layout);
        pane.add(new JLabel("Requests number per client"), labelConstraints(0, 0));
        requestsNumberField = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        pane.add(requestsNumberField, fieldConstraints(0, 1));

        {
            final ButtonGroup architectureGroup = new ButtonGroup();
            pane.add(new JLabel("Architecture"), labelConstraints(1, 0));
            final JPanel fields = new JPanel();
            final GroupLayout fieldsLayout = new GroupLayout(fields);
            fieldsLayout.setHorizontalGroup(fieldsLayout.createSequentialGroup()
                    .addGroup(
                            fieldsLayout.createParallelGroup()
                                    .addComponent(threadPerClientArchitecture).addComponent(blockingPoolArchitecture)
                    )
                    .addGroup(
                            fieldsLayout.createParallelGroup()
                                    .addComponent(nonBlockingPoolArchitecture).addComponent(asynchronousPoolArchitecture)
                    )
            );
            fieldsLayout.setVerticalGroup(fieldsLayout.createSequentialGroup()
                    .addGroup(
                            fieldsLayout.createParallelGroup()
                                    .addComponent(threadPerClientArchitecture).addComponent(nonBlockingPoolArchitecture)
                    )
                    .addGroup(
                            fieldsLayout.createParallelGroup()
                                    .addComponent(blockingPoolArchitecture).addComponent(asynchronousPoolArchitecture)
                    )
            );
            architectureGroup.add(threadPerClientArchitecture);
            architectureGroup.add(blockingPoolArchitecture);
            architectureGroup.add(nonBlockingPoolArchitecture);
            architectureGroup.add(asynchronousPoolArchitecture);
            fields.setLayout(fieldsLayout);
            pane.add(fields, fieldConstraints(1, 1));
        }

        final ButtonGroup variatingParameterGroup = new ButtonGroup();
        pane.add(new JLabel("Variating parameter"), labelConstraints(2, 0));
        {
            arraySize = new ValueOrRange("Array size", () -> new SpinnerNumberModel(0, 0, null, 1), this::sync);
            variatingParameterGroup.add(arraySize.radioButton);
            pane.add(arraySize.radioButton, labelConstraints(3, 0));
            pane.add(arraySize.fields, fieldConstraints(3, 1));
        }
        {
            clientsNumber = new ValueOrRange("Clients number", () -> new SpinnerNumberModel(1, 0, null, 1), this::sync);
            variatingParameterGroup.add(clientsNumber.radioButton);
            pane.add(clientsNumber.radioButton, labelConstraints(4, 0));
            pane.add(clientsNumber.fields, fieldConstraints(4, 1));
        }
        {
            delayBetweenRequests = new ValueOrRange("Delay between requests (ms)", () -> new SpinnerNumberModel(0, 0, null, 1), this::sync);
            variatingParameterGroup.add(delayBetweenRequests.radioButton);
            pane.add(delayBetweenRequests.radioButton, labelConstraints(5, 0));
            pane.add(delayBetweenRequests.fields, fieldConstraints(5, 1));
        }
        pane.add(runButton, labelConstraints(6, 0));
        pane.add(progressBar, fieldConstraints(6, 1));
        arraySize.radioButton.setSelected(true);
        asynchronousPoolArchitecture.setSelected(true);
        pane.setVisible(true);
        pane.setSize(600, 400);
        runButton.addActionListener(e -> {
            running = true;
            new Thread(() -> {
                try {
                    final List<Benchmark.Config> configs = benchmarkConfigs(architecture());
                    progressBar.setValue(0);
                    progressBar.setMaximum(configs.size());
                    final List<Benchmark.Result> results = new ArrayList<>();
                    for (Benchmark.Config benchmarkConfig : configs) {
                        final Benchmark.Result result = new Benchmark(benchmarkConfig).run();
                        results.add(result);
                        progressBar.setValue(progressBar.getValue() + 1);
                    }
                    final ChartsFrame chartsFrame = new ChartsFrame(results, variatingParameter());
                    chartsFrame.setTitle(chartsTitleArchitecture() + ". " + chartsTitleFixedParameters());
                    final File directory = new File(LocalDateTime.now().toString());
                    directory.mkdir();
                    chartsFrame.save(directory);
                    try (final FileWriter output = new FileWriter(new File(directory, "results.tsv"))) {
                        output.write("Architecture\tArray size\tClients number\tDelay\tRequests number\tServer request duration\tProcessing request duration\tClient request duration\n");
                        for (Benchmark.Result result : results) {
                            output.write(
                                    result.config.architecture + "\t"
                                    + result.config.arraySize + "\t"
                                    + result.config.clientsNumber + "\t"
                                    + result.config.delayNanoseconds + "\t"
                                    + result.config.requestsNumber + "\t"
                                    + result.averageServerRequestDuration + "\t"
                                    + result.averageProcessingDuration + "\t"
                                    + result.averageClientRequestDuration + "\n"
                            );
                        }
                    }
                } catch (IOException | InterruptedException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    running = false;
                    sync();
                }
            }, "bench").start();
            sync();
        });
        sync();
    }

    private static GridBagConstraints labelConstraints(int gridy, int gridx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        return constraints;
    }

    private static GridBagConstraints fieldConstraints(int gridy, int gridx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        return constraints;
    }

    private void sync() {
        threadPerClientArchitecture.setEnabled(!running);
        blockingPoolArchitecture.setEnabled(!running);
        nonBlockingPoolArchitecture.setEnabled(!running);
        asynchronousPoolArchitecture.setEnabled(!running);
        requestsNumberField.setEnabled(!running);
        arraySize.sync();
        clientsNumber.sync();
        delayBetweenRequests.sync();
        runButton.setEnabled(!running);
        progressBar.setEnabled(running);
    }

    private List<Benchmark.Config> benchmarkConfigs(Benchmark.Config.Architecture architecture) {
        if (arraySize.range) {
            return arraySize.range().mapToObj(arraySize -> new Benchmark.Config(
                    architecture, arraySize,
                    clientsNumber.min,
                    delayBetweenRequests.min * 1_000_000,
                    (int) requestsNumberField.getValue()
            )).collect(Collectors.toList());
        }
        if (clientsNumber.range) {
            return clientsNumber.range().mapToObj(clientsNumber -> new Benchmark.Config(
                    architecture, arraySize.min,
                    clientsNumber,
                    delayBetweenRequests.min * 1_000_000,
                    (int) requestsNumberField.getValue()
            )).collect(Collectors.toList());
        }
        return delayBetweenRequests.range().mapToObj(delayBetweenRequests -> new Benchmark.Config(
                architecture, arraySize.min,
                clientsNumber.min,
                delayBetweenRequests * 1_000_000,
                (int) requestsNumberField.getValue()
        )).collect(Collectors.toList());
    }

    private VariatingParameter variatingParameter() {
        if (arraySize.range) {
            return VariatingParameter.ARRAY_SIZE;
        }
        if (clientsNumber.range) {
            return VariatingParameter.CLIENTS_NUMBER;
        }
        return VariatingParameter.DELAY_BETWEEN_REQUESTS;
    }

    private String chartsTitleFixedParameters() {
        switch (variatingParameter()) {
            case ARRAY_SIZE:
                return "Clients number – " + clientsNumber.min + ". Delay between requests – " + delayBetweenRequests.min;
            case CLIENTS_NUMBER:
                return "Array size – " + arraySize.min + ". Delay between requests – " + delayBetweenRequests.min;
            case DELAY_BETWEEN_REQUESTS:
                return "Clients number – " + clientsNumber.min + ". Array size – " + arraySize.min;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String chartsTitleArchitecture() {
        switch (architecture()) {
            case ThreadPerClient:
                return "Thread per client";
            case BlockingPool:
                return "Blocking";
            case NonBlockingPool:
                return "Non-blocking";
            case Asynchronous:
                return "Asynchronous";
            default:
                throw new IllegalArgumentException();
        }
    }

    private Benchmark.Config.Architecture architecture() {
        if (asynchronousPoolArchitecture.isSelected())
            return Benchmark.Config.Architecture.Asynchronous;
        if (nonBlockingPoolArchitecture.isSelected()) {
            return Benchmark.Config.Architecture.NonBlockingPool;
        }
        if (blockingPoolArchitecture.isSelected())
            return Benchmark.Config.Architecture.BlockingPool;
        return Benchmark.Config.Architecture.ThreadPerClient;
    }
}
