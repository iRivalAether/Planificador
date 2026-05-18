package com.os.simulator.view;

import com.os.simulator.controllers.MainController;
import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.services.synchronization.SynchronizationSnapshot;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.control.ProgressBar;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;

/**
 * Vista principal del simulador.
 * Construye la interfaz grafica con tabs para procesos, ejecucion, comparacion e historial.
 */
public class MainView {
    // Nodo raiz que contiene toda la interfaz.
    private final BorderPane root;
    // Lista observable para que la tabla se actualice automaticamente cuando cambian los procesos.
    private final ObservableList<Process> processItems;
    // Controlador que recibe las acciones disparadas por botones y formularios.
    private final MainController controller;
    // Area de texto donde se muestran eventos y decisiones del simulador.
    private final TextArea eventLogArea;
    // Tabla principal donde se inspeccionan los procesos.
    private final TableView<Process> processTable;
    // Etiqueta que resume las metricas mas importantes sin abrir otra pantalla.
    private final Label metricsLabel;
    // Muestra un resumen textual del estado de sincronizacion y comunicacion.
    private final TextArea synchronizationStateArea;
    // Panel visual del diagrama productor-memoria-consumidor.
    private Pane syncDiagramPane;
    // Etiqueta que describe el paso actual de la demo automatica.
    private Label syncPhaseLabel;
    // Charts
    private javafx.scene.chart.BarChart<String, Number> burstsChart;
    private javafx.scene.chart.BarChart<String, Number> comparisonChart;
    // Timeline animation
    private Timeline timeline;
    private Timeline syncDemoTimeline;
    private int playTime = 0;
    private boolean isPlaying = false;
    // Timeline rows container (kept for refreshes)
    private VBox timelineRows;
    // Resource label to show CPU and memory totals with units
    private Label resourceLabel;

    /**
     * Crea la vista principal y arma todos los componentes visuales.
     *
     * @param controller controlador principal del simulador.
     */
    public MainView(MainController controller) {
        // Se almacena el controlador para desacoplar la vista de la logica del simulador.
        this.controller = controller;
        this.root = new BorderPane();
        this.processItems = FXCollections.observableArrayList();
        this.eventLogArea = new TextArea();
        this.processTable = new TableView<>();
        this.metricsLabel = new Label();
        this.synchronizationStateArea = new TextArea();
        buildInterface();
    }

    private void buildInterface() {
        // La estructura se divide en cabecera, contenido y barra de estado para mantener claridad visual.
        root.setPadding(new Insets(10));
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        root.setBottom(buildStatusBar());
    }

    private Parent buildHeader() {
        // Se usa un encabezado simple porque la prioridad de esta vista es la legibilidad del simulador.
        Label title = new Label("Simulador de Gestor de Procesos");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox header = new VBox(title);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private Parent buildTabs() {
        // Cada tab representa una tarea distinta para no saturar una sola pantalla.
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(buildDashboardTab());
        tabPane.getTabs().add(buildConfigurationTab());
        tabPane.getTabs().add(buildExecutionTab());
        tabPane.getTabs().add(buildSynchronizationTab());
        tabPane.getTabs().add(buildTimelineTab());
        tabPane.getTabs().add(buildComparisonTab());
        tabPane.getTabs().add(buildHistoryTab());
        return tabPane;
    }

    private Tab buildTimelineTab() {
        Tab tab = new Tab("Linea de tiempo"); tab.setClosable(false);
        Label desc = new Label("Linea de tiempo Gantt: cada fila muestra la ejecucion de un proceso a lo largo del tiempo. Marcadores indican eventos de sincronizacion/comunicacion.");

        ScrollPane scroll = new ScrollPane();
        VBox rows = new VBox(6);
        this.timelineRows = rows;
        scroll.setContent(rows);
        scroll.setFitToWidth(true);

        Button playPause = new Button("Play");
        Button step = new Button("Step");
        Button reset = new Button("Reset");
        HBox controls = new HBox(8, playPause, step, reset);

        playPause.setOnAction(e -> {
            if (!isPlaying) {
                startTimeline(rows);
                playPause.setText("Pause");
            } else {
                stopTimeline();
                playPause.setText("Play");
            }
        });

        step.setOnAction(e -> { advanceOneFrame(rows); });
        reset.setOnAction(e -> { stopTimeline(); playTime = 0; renderTimeline(rows, playTime); playPause.setText("Play"); });

        VBox container = new VBox(8, desc, controls, scroll);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        // initial render at time 0
        renderTimeline(rows, 0);
        return tab;
    }

    private void renderTimeline(VBox rows, int playAt) {
        rows.getChildren().clear();
        List<Process> processes = controller.getProcesses();
        List<String> events = controller.getEventLog();
        int maxTime = Math.max(1, controller.getSystemState().getCurrentTime());
        int widthPerUnit = 24; // px per time unit
        int canvasHeight = 30;
        // Prefer structured EVENT:DISPATCH / EVENT:PREEMPT / EVENT:FINISH if available
        Pattern pDispatch = Pattern.compile("\\[t=(\\d+)\\].*EVENT:DISPATCH PID (\\d+)");
        Pattern pPreempt = Pattern.compile("\\[t=(\\d+)\\].*EVENT:PREEMPT PID (\\d+)");
        Pattern pFinish = Pattern.compile("\\[t=(\\d+)\\].*EVENT:FINISH PID (\\d+)");

        Map<Integer, List<int[]>> intervals = new HashMap<>();
        // scan for dispatch events and pair with next preempt/finish for same PID
        for (int i = 0; i < events.size(); i++) {
            String ev = events.get(i);
            Matcher md = pDispatch.matcher(ev);
            if (md.find()) {
                int t = Integer.parseInt(md.group(1)); int pid = Integer.parseInt(md.group(2));
                int end = maxTime;
                for (int j = i + 1; j < events.size(); j++) {
                    String ev2 = events.get(j);
                    Matcher mfin = pFinish.matcher(ev2);
                    Matcher mpre = pPreempt.matcher(ev2);
                    if (mfin.find()) {
                        int t2 = Integer.parseInt(mfin.group(1)); int pid2 = Integer.parseInt(mfin.group(2));
                        if (pid2 == pid) { end = t2; break; }
                    }
                    if (mpre.find()) {
                        int t2 = Integer.parseInt(mpre.group(1)); int pid2 = Integer.parseInt(mpre.group(2));
                        if (pid2 == pid) { end = t2; break; }
                    }
                }
                intervals.computeIfAbsent(pid, k -> new ArrayList<>()).add(new int[]{t, end});
            }
        }

        // Fallback: try to parse human-friendly messages if structured events are missing
        if (intervals.isEmpty()) {
            Pattern pExec = Pattern.compile("\\[t=(\\d+)\\] .*ejecuta .*PID (\\d+)");
            Pattern pEnd = Pattern.compile("\\[t=(\\d+)\\] .*devuelve .*PID (\\d+)");
            Pattern pTerm = Pattern.compile("\\[t=(\\d+)\\] .*Proceso terminado: PID (\\d+)");
            for (String ev : events) {
                Matcher m = pExec.matcher(ev);
                if (m.find()) {
                    int t = Integer.parseInt(m.group(1)); int pid = Integer.parseInt(m.group(2));
                    int end = maxTime;
                    for (String ev2 : events) {
                        Matcher m2 = pEnd.matcher(ev2);
                        if (m2.find()) {
                            int t2 = Integer.parseInt(m2.group(1)); int pid2 = Integer.parseInt(m2.group(2));
                            if (pid2 == pid && t2 >= t) { end = t2; break; }
                        }
                        Matcher m3 = pTerm.matcher(ev2);
                        if (m3.find()) { int t3 = Integer.parseInt(m3.group(1)); int pid3 = Integer.parseInt(m3.group(2)); if (pid3 == pid && t3 >= t) { end = t3; break; } }
                    }
                    intervals.computeIfAbsent(pid, k -> new ArrayList<>()).add(new int[]{t, end});
                }
            }
        }

        // For each process draw a row
        for (Process p : processes) {
            HBox row = new HBox(6);
            Label lbl = new Label("PID " + p.getPid() + " - " + p.getName()); lbl.setMinWidth(140);
            Pane canvas = new Pane();
            canvas.setPrefSize(Math.max(400, Math.max(1, maxTime) * widthPerUnit), canvasHeight);
            canvas.setStyle("-fx-background-color: #f8f8f8;");

            // draw grid lines (lightweight lines)
            for (int t = 0; t <= maxTime; t++) {
                double x = t * widthPerUnit;
                Line line = new Line(x, 0, x, canvasHeight);
                line.setStroke(Color.LIGHTGRAY);
                line.setMouseTransparent(true);
                canvas.getChildren().add(line);
            }

            List<int[]> its = intervals.getOrDefault(p.getPid(), Collections.emptyList());
            Color color = Color.hsb((p.getPid()*47) % 360, 0.6, 0.8);
            for (int[] itv : its) {
                int start = itv[0]; int end = itv[1];
                if (playAt <= start) continue; // not started yet
                double visibleEnd = Math.min(end, playAt);
                double x = start * widthPerUnit; double w = Math.max(2, (visibleEnd - start) * widthPerUnit);
                Rectangle rect = new Rectangle(x, 6, w, canvasHeight - 12);
                rect.setFill(color);
                rect.setStroke(Color.BLACK);
                rect.setMouseTransparent(true);
                canvas.getChildren().add(rect);
            }

            // draw sync/comm markers for this pid when their time <= playAt
            for (String ev : events) {
                // producer/consumer textual markers
                Matcher mp = Pattern.compile("\\[t=(\\d+)\\].*(Productor|Consumidor).*PID (\\d+).*", Pattern.CASE_INSENSITIVE).matcher(ev);
                if (mp.find()) {
                    int t = Integer.parseInt(mp.group(1)); int pidm = Integer.parseInt(mp.group(3));
                    if (pidm == p.getPid() && t <= playAt) {
                        double x = t * widthPerUnit;
                        Circle marker = new Circle(x, canvasHeight/2.0, 4, Color.DARKORANGE);
                        marker.setMouseTransparent(true);
                        canvas.getChildren().add(marker);
                    }
                }

                // structured PRODUCE/CONSUME events
                if (ev.contains("EVENT:PRODUCE")) {
                    Matcher m = Pattern.compile("\\[t=(\\d+)\\].*EVENT:PRODUCE(?:_BLOCKED|_START|_END|)? PID (\\d+)").matcher(ev);
                    if (m.find()) {
                        int t = Integer.parseInt(m.group(1)); int pidm = Integer.parseInt(m.group(2));
                        if (pidm == p.getPid() && t <= playAt) {
                            double x = t * widthPerUnit;
                            Circle marker = new Circle(x, canvasHeight/2.0, 4, Color.LIMEGREEN);
                            marker.setMouseTransparent(true);
                            canvas.getChildren().add(marker);
                        }
                    }
                }

                if (ev.contains("EVENT:CONSUME")) {
                    Matcher m = Pattern.compile("\\[t=(\\d+)\\].*EVENT:CONSUME(?:_BLOCKED|_START|_END|)? PID (\\d+)").matcher(ev);
                    if (m.find()) {
                        int t = Integer.parseInt(m.group(1)); int pidm = Integer.parseInt(m.group(2));
                        if (pidm == p.getPid() && t <= playAt) {
                            double x = t * widthPerUnit;
                            Circle marker = new Circle(x, canvasHeight/2.0, 4, Color.DEEPSKYBLUE);
                            marker.setMouseTransparent(true);
                            canvas.getChildren().add(marker);
                        }
                    }
                }

                // blocked/unblock markers
                if (ev.contains("EVENT:PRODUCE_BLOCKED") || ev.contains("EVENT:CONSUME_BLOCKED") || ev.contains("_BLOCKED")) {
                    Matcher m = Pattern.compile("\\[t=(\\d+)\\].*PID (\\d+)").matcher(ev);
                    if (m.find()) {
                        int t = Integer.parseInt(m.group(1)); int pidm = Integer.parseInt(m.group(2));
                        if (pidm == p.getPid() && t <= playAt) {
                            double x = t * widthPerUnit;
                            Circle marker = new Circle(x, canvasHeight/2.0, 5, Color.CRIMSON);
                            marker.setMouseTransparent(true);
                            canvas.getChildren().add(marker);
                        }
                    }
                }
                if (ev.contains("EVENT:UNBLOCK")) {
                    Matcher m = Pattern.compile("\\[t=(\\d+)\\].*EVENT:UNBLOCK PID (\\d+)").matcher(ev);
                    if (m.find()) {
                        int t = Integer.parseInt(m.group(1)); int pidm = Integer.parseInt(m.group(2));
                        if (pidm == p.getPid() && t <= playAt) {
                            double x = t * widthPerUnit;
                            Circle marker = new Circle(x, canvasHeight/2.0, 5, Color.GOLD);
                            marker.setMouseTransparent(true);
                            canvas.getChildren().add(marker);
                        }
                    }
                }
            }

            row.getChildren().addAll(lbl, canvas);
            rows.getChildren().add(row);
        }

        // Legend
        HBox legend = new HBox(10); legend.setPadding(new Insets(6));
        Label l1 = new Label("Rectangulo: ejecucion en CPU (crece durante reproduccion)"); Label l2 = new Label("Punto naranja: evento de sincronizacion/comunicacion");
        legend.getChildren().addAll(l1, l2);
        rows.getChildren().add(0, legend);

        // Draw buffer occupancy Gantt-like row below processes
        int capacity = 1;
        try { capacity = controller.getProducerConsumerBufferCapacity(); } catch (Exception ignored) {}
        int[] occupancy = new int[maxTime + 1];
        for (String ev : events) {
            Matcher mProd = Pattern.compile("\\[t=(\\d+)\\].*EVENT:PRODUCE(?:_| )? PID (\\d+)").matcher(ev);
            Matcher mCons = Pattern.compile("\\[t=(\\d+)\\].*EVENT:CONSUME(?:_| )? PID (\\d+)").matcher(ev);
            if (mProd.find()) {
                int t = Integer.parseInt(mProd.group(1)); if (t >= 0 && t <= maxTime) occupancy[t] = occupancy[Math.max(0, t-1)] + 1;
            } else if (mCons.find()) {
                int t = Integer.parseInt(mCons.group(1)); if (t >= 0 && t <= maxTime) occupancy[t] = Math.max(0, occupancy[Math.max(0, t-1)] - 1);
            }
        }
        // fill forward occupancy for missing timestamps
        for (int t = 1; t <= maxTime; t++) {
            if (occupancy[t] == 0 && occupancy[t-1] != 0) occupancy[t] = occupancy[t-1];
        }

        HBox bufferRow = new HBox(6);
        Label bufferLabel = new Label("Buffer ocupacion (0.." + capacity + ")"); bufferLabel.setMinWidth(140);
        Pane bufferPane = new Pane(); bufferPane.setPrefSize(Math.max(400, Math.max(1, maxTime) * widthPerUnit), canvasHeight);
        for (int t = 0; t <= maxTime; t++) {
            int occ = occupancy[t]; double x = t * widthPerUnit; double w = Math.max(2, widthPerUnit);
            double h = (capacity <= 0) ? 0 : (canvasHeight - 8) * (occ / (double)capacity);
            Rectangle r = new Rectangle(x, canvasHeight - 4 - h, w, h);
            r.setFill(occ > 0 ? Color.DARKSEAGREEN : Color.LIGHTGRAY);
            r.setStroke(Color.GRAY);
            r.setMouseTransparent(true);
            bufferPane.getChildren().add(r);
        }
        bufferRow.getChildren().addAll(bufferLabel, bufferPane);
        rows.getChildren().add(bufferRow);
    }

    private void startTimeline(VBox rows) {
        if (timeline != null) timeline.stop();
        int maxTime = Math.max(1, controller.getSystemState().getCurrentTime());
        timeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            playTime++;
            renderTimeline(rows, playTime);
            if (playTime >= maxTime) {
                stopTimeline();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        isPlaying = true;
        timeline.play();
    }

    private void stopTimeline() {
        if (timeline != null) timeline.stop();
        isPlaying = false;
    }

    private void advanceOneFrame(VBox rows) {
        int maxTime = Math.max(1, controller.getSystemState().getCurrentTime());
        playTime = Math.min(maxTime, playTime + 1);
        renderTimeline(rows, playTime);
    }

    private Tab buildSynchronizationTab() {
        // Esta pestaña muestra explicitamente la separacion entre comunicacion y sincronizacion.
        Tab tab = new Tab("Sincronizacion");
        tab.setClosable(false);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        TextField producerPidField = new TextField("1001");
        TextField consumerPidField = new TextField("2001");
        TextField sharedValueField = new TextField("dato-demo");

        form.add(new Label("PID Productor"), 0, 0);
        form.add(producerPidField, 1, 0);
        form.add(new Label("PID Consumidor"), 0, 1);
        form.add(consumerPidField, 1, 1);
        form.add(new Label("Dato a producir"), 0, 2);
        form.add(sharedValueField, 1, 2);

        Button produceButton = new Button("Producir");
        produceButton.setOnAction(event -> {
            try {
                int pid = Integer.parseInt(producerPidField.getText());
                String value = sharedValueField.getText();
                boolean produced = controller.produceInSharedMemory(pid, value);
                if (produced) {
                    eventLogArea.appendText(System.lineSeparator() + "Productor PID " + pid + " produjo: " + value);
                } else {
                    eventLogArea.appendText(System.lineSeparator() + "Productor PID " + pid + " quedo en espera.");
                }
                refreshAll();
            } catch (NumberFormatException exception) {
                eventLogArea.appendText(System.lineSeparator() + "PID de productor invalido.");
            }
        });

        Button consumeButton = new Button("Consumir");
        consumeButton.setOnAction(event -> {
            try {
                int pid = Integer.parseInt(consumerPidField.getText());
                String value = controller.consumeFromSharedMemory(pid);
                if (value != null) {
                    eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + pid + " consumio: " + value);
                } else {
                    eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + pid + " quedo en espera.");
                }
                refreshAll();
            } catch (NumberFormatException exception) {
                eventLogArea.appendText(System.lineSeparator() + "PID de consumidor invalido.");
            }
        });

        Button refreshSyncButton = new Button("Actualizar estado");
        refreshSyncButton.setOnAction(event -> refreshSynchronizationState());

        Button demoButton = new Button("Demo Productor-Consumidor");
        demoButton.setOnAction(ev -> {
            // Simple demo: consumer tries to read before producer writes, then producer writes and consumer reads.
            eventLogArea.appendText(System.lineSeparator() + "--- Demo: consumer lee antes de producer ---");
            int consumerPid = 9001; int producerPid = 8001;
            String before = controller.consumeFromSharedMemory(consumerPid);
            if (before != null) eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + consumerPid + " leyo: " + before);
            else eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + consumerPid + " quedo en espera (bloqueado).");

            boolean wrote = controller.produceInSharedMemory(producerPid, "valor-demo");
            if (wrote) eventLogArea.appendText(System.lineSeparator() + "Productor PID " + producerPid + " produjo: valor-demo");
            else eventLogArea.appendText(System.lineSeparator() + "Productor PID " + producerPid + " quedo en espera (bloqueado).");

            String after = controller.consumeFromSharedMemory(consumerPid);
            if (after != null) eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + consumerPid + " leyo: " + after);
            else eventLogArea.appendText(System.lineSeparator() + "Consumidor PID " + consumerPid + " aun no pudo leer.");
            refreshAll();
        });

        // Automatic scenario controls
        TextField numProducersField = new TextField("2");
        TextField numConsumersField = new TextField("2");
        TextField cyclesField = new TextField("3");
        TextField bufferSizeField = new TextField("1");
        Button runScenario = new Button("Ejecutar escenario automatico");
        runScenario.setOnAction(ev -> {
            try {
                int np = Integer.parseInt(numProducersField.getText());
                int nc = Integer.parseInt(numConsumersField.getText());
                int cycles = Integer.parseInt(cyclesField.getText());
                int buffer = Integer.parseInt(bufferSizeField.getText());
                playProducerConsumerDemo(np, nc, cycles, buffer);
            } catch (NumberFormatException ex) {
                eventLogArea.appendText(System.lineSeparator() + "Parametros invalidos para escenario.");
            }
        });

        HBox scenarioControls = new HBox(8, new Label("Prods:"), numProducersField, new Label("Cons:"), numConsumersField, new Label("Ciclos:"), cyclesField, new Label("Buffer:"), bufferSizeField, runScenario);

        HBox buttons = new HBox(10, produceButton, consumeButton, demoButton, refreshSyncButton);

        syncPhaseLabel = new Label("Paso actual: listo para iniciar la demo");
        syncDiagramPane = new Pane();
        syncDiagramPane.setPrefSize(720, 180);
        syncDiagramPane.setMinHeight(180);
        syncDiagramPane.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f5f8fb); -fx-border-color: #d0d7de; -fx-border-radius: 6; -fx-background-radius: 6;");

        synchronizationStateArea.setEditable(false);
        synchronizationStateArea.setPrefRowCount(16);

        VBox container = new VBox(
                12,
                new Label("Escenario Productor-Consumidor (memoria compartida + mutex + semaforos)"),
                form,
                buttons,
                scenarioControls,
                syncPhaseLabel,
                syncDiagramPane,
                new Separator(),
                new Label("Estado actual de sincronizacion"),
                synchronizationStateArea);
        container.setPadding(new Insets(10));
        tab.setContent(container);

        refreshSynchronizationState();
        renderSynchronizationDiagram("Estado inicial: buffer vacio y sincronizacion lista", Color.SLATEGRAY, 9001, 8001);
        return tab;
    }

    private void playProducerConsumerDemo(int numProducers, int numConsumers, int cycles, int bufferSize) {
        if (syncDemoTimeline != null) {
            syncDemoTimeline.stop();
        }

        controller.resetProducerConsumerSimulation(Math.max(1, bufferSize));
        refreshAll();

        List<Runnable> steps = new ArrayList<>();
        List<Integer> producerPids = new ArrayList<>();
        List<Integer> consumerPids = new ArrayList<>();
        for (int i = 0; i < Math.max(1, numProducers); i++) {
            producerPids.add(8001 + i);
        }
        for (int i = 0; i < Math.max(1, numConsumers); i++) {
            consumerPids.add(9001 + i);
        }

        int initialProducerPid = producerPids.get(0);
        int initialConsumerPid = consumerPids.get(0);

        steps.add(() -> {
            syncPhaseLabel.setText("Paso 1: el consumidor intenta leer antes de que exista dato");
            eventLogArea.appendText(System.lineSeparator() + "[Demo] Consumidor intenta leer y debe bloquearse si el buffer esta vacio.");
            for (int pid : consumerPids) {
                controller.consumeFromSharedMemory(pid);
            }
            refreshSynchronizationState();
            renderSynchronizationDiagram("Consumidores bloqueados esperando dato", Color.CRIMSON, initialConsumerPid, initialProducerPid);
        });

        steps.add(() -> {
            syncPhaseLabel.setText("Paso 2: el productor toma el mutex, escribe y libera un dato");
            eventLogArea.appendText(System.lineSeparator() + "[Demo] Productor escribe en memoria compartida.");
            for (int pid : producerPids) {
                controller.produceInSharedMemory(pid, "dato-demo-" + pid);
            }
            refreshSynchronizationState();
            renderSynchronizationDiagram("Productores produjeron y notificaron a los consumidores", Color.LIMEGREEN, initialConsumerPid, initialProducerPid);
        });

        steps.add(() -> {
            syncPhaseLabel.setText("Paso 3: el consumidor despierta, toma el mutex y lee el dato");
            eventLogArea.appendText(System.lineSeparator() + "[Demo] Consumidor vuelve a intentarlo y ahora puede leer.");
            for (int pid : consumerPids) {
                controller.consumeFromSharedMemory(pid);
            }
            refreshSynchronizationState();
            renderSynchronizationDiagram("Consumidores sincronizados y datos consumidos", Color.DEEPSKYBLUE, initialConsumerPid, initialProducerPid);
        });

        for (int i = 0; i < Math.max(1, cycles - 1); i++) {
            int cycleIndex = i + 2;
            steps.add(() -> {
                syncPhaseLabel.setText("Ciclo extra: productor/consumidor repiten la sincronizacion");
                for (int pid : producerPids) {
                    controller.produceInSharedMemory(pid, "dato-demo-" + cycleIndex + "-p" + pid);
                }
                refreshSynchronizationState();
                renderSynchronizationDiagram("Nuevos datos producidos", Color.GOLDENROD, initialConsumerPid, initialProducerPid);
            });
            steps.add(() -> {
                for (int pid : consumerPids) {
                    controller.consumeFromSharedMemory(pid);
                }
                refreshSynchronizationState();
                renderSynchronizationDiagram("Nuevos datos consumidos", Color.MEDIUMSLATEBLUE, initialConsumerPid, initialProducerPid);
            });
        }

        playScenarioSteps(steps);
    }

    private void playScenarioSteps(List<Runnable> steps) {
        if (steps.isEmpty()) {
            return;
        }

        final int[] index = {0};
        syncDemoTimeline = new Timeline(new KeyFrame(Duration.millis(950), event -> {
            if (index[0] < steps.size()) {
                steps.get(index[0]).run();
                index[0]++;
            } else {
                if (syncDemoTimeline != null) {
                    syncDemoTimeline.stop();
                }
                syncPhaseLabel.setText("Demo finalizada: productor y consumidor quedaron coordinados");
            }
        }));
        syncDemoTimeline.setCycleCount(Timeline.INDEFINITE);
        syncDemoTimeline.playFromStart();
    }

    private void renderSynchronizationDiagram(String phase, Color accent, int consumerPid, int producerPid) {
        if (syncDiagramPane == null) {
            return;
        }

        syncDiagramPane.getChildren().clear();
        SynchronizationSnapshot snapshot = controller.getSynchronizationSnapshot();

        double centerY = 82;
        double producerX = 70;
        double memoryX = 300;
        double consumerX = 530;

        Line pToMem = new Line(producerX + 92, centerY, memoryX - 20, centerY);
        pToMem.setStroke(accent);
        pToMem.setStrokeWidth(3);

        Line memToC = new Line(memoryX + 100, centerY, consumerX - 20, centerY);
        memToC.setStroke(accent.darker());
        memToC.setStrokeWidth(3);

        Rectangle producerCard = new Rectangle(producerX, 34, 170, 96);
        producerCard.setArcWidth(18);
        producerCard.setArcHeight(18);
        producerCard.setFill(Color.web("#fff3e6"));
        producerCard.setStroke(Color.web("#d97b00"));

        Rectangle memoryCard = new Rectangle(memoryX, 28, 190, 108);
        memoryCard.setArcWidth(18);
        memoryCard.setArcHeight(18);
        memoryCard.setFill(snapshot.getSharedValue().isBlank() ? Color.web("#f3f4f6") : Color.web("#e8f8ee"));
        memoryCard.setStroke(snapshot.getSharedValue().isBlank() ? Color.web("#7b8794") : Color.web("#2f855a"));

        Rectangle consumerCard = new Rectangle(consumerX, 34, 170, 96);
        consumerCard.setArcWidth(18);
        consumerCard.setArcHeight(18);
        consumerCard.setFill(Color.web("#e8f2ff"));
        consumerCard.setStroke(Color.web("#1864ab"));

        Label producerLabel = new Label("Productor\nPID " + producerPid);
        producerLabel.setLayoutX(producerX + 22);
        producerLabel.setLayoutY(52);
        producerLabel.setStyle("-fx-font-weight: bold;");

        Label memoryLabel = new Label("Memoria compartida\n" + (snapshot.getSharedValue().isBlank() ? "vacia" : snapshot.getSharedValue()));
        memoryLabel.setLayoutX(memoryX + 18);
        memoryLabel.setLayoutY(46);
        memoryLabel.setStyle("-fx-font-weight: bold;");

        Label consumerLabel = new Label("Consumidor\nPID " + consumerPid);
        consumerLabel.setLayoutX(consumerX + 24);
        consumerLabel.setLayoutY(52);
        consumerLabel.setStyle("-fx-font-weight: bold;");

        Label semaphoreLabel = new Label(String.format(
                "Mutex: %s | empty=%d | filled=%d", 
                snapshot.isMutexLocked() ? "bloqueado" : "libre",
                snapshot.getEmptySlotsValue(),
                snapshot.getFilledSlotsValue()));
        semaphoreLabel.setLayoutX(18);
        semaphoreLabel.setLayoutY(146);
        semaphoreLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Label phaseLabel = new Label(phase);
        phaseLabel.setLayoutX(18);
        phaseLabel.setLayoutY(12);
        phaseLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label waitingLabel = new Label(String.format(
                "Espera mutex=%s | empty=%s | filled=%s",
                snapshot.getMutexWaitingPids(),
                snapshot.getEmptySlotsWaitingPids(),
                snapshot.getFilledSlotsWaitingPids()));
        waitingLabel.setLayoutX(18);
        waitingLabel.setLayoutY(162);
        waitingLabel.setStyle("-fx-font-size: 11px;");

        syncDiagramPane.getChildren().addAll(
                pToMem,
                memToC,
                producerCard,
                memoryCard,
                consumerCard,
                phaseLabel,
                producerLabel,
                memoryLabel,
                consumerLabel,
                semaphoreLabel,
                waitingLabel);
    }

    private Tab buildDashboardTab() {
        // Esta pestaña resume el estado general porque es la que el usuario consulta con mayor frecuencia.
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);

        TableColumn<Process, Integer> pidColumn = new TableColumn<>("PID");
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));

        TableColumn<Process, String> nameColumn = new TableColumn<>("Nombre");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Process, String> stateColumn = new TableColumn<>("Estado");
        stateColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getState().name()));

        TableColumn<Process, Integer> priorityColumn = new TableColumn<>("Prioridad");
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));

        TableColumn<Process, Integer> cpuColumn = new TableColumn<>("CPU Restante");
        cpuColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getRemainingCpu()).asObject());
        cpuColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Process, Integer>() {
            private final ProgressBar bar = new ProgressBar(0);
            private final Label lbl = new Label();
            private final HBox box = new HBox(6, bar, lbl);
            {
                bar.setPrefWidth(120);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Integer remaining, boolean empty) {
                super.updateItem(remaining, empty);
                if (empty || remaining == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Process p = getTableView().getItems().get(getIndex());
                    int burst = Math.max(1, p.getCpuBurst());
                    int used = p.getCpuUsed();
                    double progress = Math.min(1.0, Math.max(0.0, (double) used / burst));
                    bar.setProgress(progress);
                    lbl.setText(String.format("%d/%d", used, burst));
                    setGraphic(box);
                }
            }
        });

        TableColumn<Process, Integer> allocatedCpuColumn = new TableColumn<>("CPU Asignada (nucleos)");
        allocatedCpuColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAllocatedCpuUnits()).asObject());
        allocatedCpuColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Process, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); setGraphic(null); }
                else {
                    Process p = getTableView().getItems().get(getIndex());
                    setText(String.valueOf(value));
                    if (value > 0) setStyle("-fx-background-color: rgba(173,216,230,0.3);"); else setStyle("");
                }
            }
        });

        TableColumn<Process, Integer> memoryReqColumn = new TableColumn<>("Memoria Requerida (MB)");
        memoryReqColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getMemoryRequired()).asObject());

        TableColumn<Process, Integer> allocatedMemColumn = new TableColumn<>("Memoria Asignada (MB)");
        allocatedMemColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAllocatedMemoryUnits()).asObject());
        allocatedMemColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Process, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setText(null); setGraphic(null); }
                else {
                    Process p = getTableView().getItems().get(getIndex());
                    int req = p.getMemoryRequired();
                    setText(String.format("%d/%d", value, req));
                    if (value == 0 && p.getState().name().equals("WAITING")) {
                        setStyle("-fx-background-color: rgba(255,99,71,0.25);");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        processTable.getColumns().setAll(pidColumn, nameColumn, stateColumn, priorityColumn, cpuColumn, allocatedCpuColumn, memoryReqColumn, allocatedMemColumn);
        processTable.setItems(processItems);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        eventLogArea.setEditable(false);
        eventLogArea.setPrefRowCount(10);

        resourceLabel = new Label();
        VBox container = new VBox(10,
            processTable,
            new Label("Metricas actuales"),
            metricsLabel,
            resourceLabel,
            new Label("Registro de eventos"),
            eventLogArea);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildConfigurationTab() {
        // Aqui el usuario define la carga de trabajo y selecciona el planificador activo.
        Tab tab = new Tab("Configuracion");
        tab.setClosable(false);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));

        TextField nameField = new TextField();
        TextField priorityField = new TextField("5");
        TextField cpuField = new TextField("5");
        TextField memoryField = new TextField("128");
        TextField randomCountField = new TextField("3");
        ComboBox<String> schedulerCombo = new ComboBox<>(FXCollections.observableArrayList("FCFS", "SJF", "Round Robin", "Prioridad"));
        schedulerCombo.getSelectionModel().selectFirst();

        form.add(new Label("Nombre"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Prioridad"), 0, 1);
        form.add(priorityField, 1, 1);
        form.add(new Label("CPU (nucleos)"), 0, 2);
        form.add(cpuField, 1, 2);
        form.add(new Label("Memoria (MB)"), 0, 3);
        form.add(memoryField, 1, 3);
        form.add(new Label("Cantidad aleatoria"), 0, 4);
        form.add(randomCountField, 1, 4);
        form.add(new Label("Algoritmo"), 0, 5);
        form.add(schedulerCombo, 1, 5);

        Button addButton = new Button("Agregar proceso");
        addButton.setOnAction(event -> {
            // Se valida de forma minima en la UI; la logica completa vive en el service.
            controller.createProcess(
                    nameField.getText().isBlank() ? "Proceso manual" : nameField.getText(),
                    Integer.parseInt(priorityField.getText()),
                    Integer.parseInt(cpuField.getText()),
                    Integer.parseInt(memoryField.getText())
            );
            refreshProcesses();
            refreshEvents();
        });

        Button randomButton = new Button("Generar aleatorios");
        randomButton.setOnAction(event -> {
            // Generar cargas sinteticas ayuda a comparar algoritmos sin capturar datos manualmente.
            controller.generateRandomProcesses(Integer.parseInt(randomCountField.getText()));
            refreshProcesses();
            refreshEvents();
        });

        Button schedulerButton = new Button("Aplicar planificador");
        schedulerButton.setOnAction(event -> controller.setScheduler(schedulerCombo.getValue()));

        Button exportButton = new Button("Exportar procesos");
        exportButton.setOnAction(event -> runSafeAction(() -> controller.exportProcesses(Path.of("data", "procesos.txt"))));

        Button importButton = new Button("Importar procesos");
        importButton.setOnAction(event -> {
            runSafeAction(() -> controller.importProcesses(Path.of("data", "procesos.txt")));
            refreshAll();
        });

        HBox buttons = new HBox(10, addButton, randomButton, schedulerButton, exportButton, importButton);
        VBox container = new VBox(15, form, buttons);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildExecutionTab() {
        // Esta pestaña concentra las acciones que hacen avanzar la simulacion.
        Tab tab = new Tab("Ejecucion");
        tab.setClosable(false);

        Button stepButton = new Button("Ejecutar paso");
        stepButton.setOnAction(event -> {
            // El modo paso a paso permite ver claramente como cambia cada algoritmo.
            controller.runStep();
            refreshAll();
        });

        Button runAllButton = new Button("Ejecutar completo");
        runAllButton.setOnAction(event -> {
            // La ejecucion completa sirve para obtener metricas finales sin intervenir manualmente.
            controller.runSimulationToCompletion();
            controller.appendExecutionToHistory();
            refreshAll();
        });

        Button compareButton = new Button("Comparar algoritmos");
        compareButton.setOnAction(event -> {
            // La comparacion usa las mismas cargas para que el resultado sea justo.
            StringBuilder builder = new StringBuilder();
            controller.compareAlgorithms().forEach((algorithm, metrics) ->
                    builder.append(algorithm)
                            .append(" -> espera promedio: ")
                            .append(formatNumber(metrics.getAverageWaitingTime()))
                            .append(", retorno promedio: ")
                            .append(formatNumber(metrics.getAverageTurnaroundTime()))
                            .append(", CPU: ")
                            .append(formatNumber(metrics.getCpuUtilization()))
                            .append("%")
                            .append(System.lineSeparator()));
            eventLogArea.appendText(System.lineSeparator() + "Comparacion de algoritmos:" + System.lineSeparator() + builder);
        });

        Button suspendButton = new Button("Suspender seleccionado");
        suspendButton.setOnAction(event -> runProcessAction(process -> controller.suspendProcess(process.getPid())));

        Button resumeButton = new Button("Reanudar seleccionado");
        resumeButton.setOnAction(event -> runProcessAction(process -> controller.resumeProcess(process.getPid())));

        Button terminateButton = new Button("Terminar seleccionado");
        terminateButton.setOnAction(event -> runProcessAction(process -> controller.terminateProcess(process.getPid(), "Terminacion manual desde la interfaz")));

        // detalle de procesos y grafico de rafagas
        TableView<Process> detailTable = new TableView<>();
        TableColumn<Process, Integer> pidCol = new TableColumn<>("PID"); pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));
        TableColumn<Process, String> nameCol = new TableColumn<>("Nombre"); nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Process, Integer> burstCol = new TableColumn<>("CPU Burst"); burstCol.setCellValueFactory(new PropertyValueFactory<>("cpuBurst"));
        TableColumn<Process, Integer> usedCol = new TableColumn<>("CPU Used"); usedCol.setCellValueFactory(new PropertyValueFactory<>("cpuUsed"));
        TableColumn<Process, Integer> waitCol = new TableColumn<>("Waiting"); waitCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        TableColumn<Process, Integer> turnCol = new TableColumn<>("Turnaround"); turnCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        TableColumn<Process, Integer> allocCpuCol = new TableColumn<>("CPU Asignada"); allocCpuCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAllocatedCpuUnits()).asObject());
        TableColumn<Process, Integer> allocMemCol = new TableColumn<>("Memoria Asignada"); allocMemCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getAllocatedMemoryUnits()).asObject());
        detailTable.getColumns().addAll(pidCol, nameCol, burstCol, usedCol, waitCol, turnCol);
        detailTable.getColumns().addAll(allocCpuCol, allocMemCol);
        detailTable.setItems(processItems);

        CategoryAxis xAxis = new CategoryAxis(); NumberAxis yAxis = new NumberAxis();
        burstsChart = new BarChart<>(xAxis, yAxis);
        burstsChart.setTitle("Rafagas de CPU por proceso"); xAxis.setLabel("PID"); yAxis.setLabel("CPU Burst");

        VBox container = new VBox(10, stepButton, runAllButton, compareButton, suspendButton, resumeButton, terminateButton, new Label("Detalle de procesos"), detailTable, burstsChart);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildComparisonTab() {
        Tab tab = new Tab("Comparacion"); tab.setClosable(false);
        CategoryAxis x = new CategoryAxis(); NumberAxis y = new NumberAxis();
        comparisonChart = new BarChart<>(x, y); comparisonChart.setTitle("Comparacion de algoritmos");
        x.setLabel("Algoritmo"); y.setLabel("Valor");
        Button refresh = new Button("Actualizar comparacion");
        refresh.setOnAction(e -> {
            comparisonChart.getData().clear();
            var results = controller.compareAlgorithms();
            XYChart.Series<String, Number> waitS = new XYChart.Series<>(); waitS.setName("Avg Waiting");
            XYChart.Series<String, Number> turnS = new XYChart.Series<>(); turnS.setName("Avg Turnaround");
            XYChart.Series<String, Number> cpuS = new XYChart.Series<>(); cpuS.setName("CPU%");
            results.forEach((alg, m) -> { waitS.getData().add(new XYChart.Data<>(alg, m.getAverageWaitingTime())); turnS.getData().add(new XYChart.Data<>(alg, m.getAverageTurnaroundTime())); cpuS.getData().add(new XYChart.Data<>(alg, m.getCpuUtilization())); });
            comparisonChart.getData().addAll(waitS, turnS, cpuS);
            refreshAll();
        });
        VBox container = new VBox(10, refresh, comparisonChart);
        tab.setContent(container);
        return tab;
    }

    private Tab buildHistoryTab() {
        Tab tab = new Tab("Historial");
        tab.setClosable(false);
        tab.setContent(new VBox(new Label("Se agregara el historial de ejecuciones en la siguiente iteracion.")));
        return tab;
    }

    private Parent buildStatusBar() {
        Label status = new Label("Listo para simular procesos.");
        HBox bar = new HBox(status);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    /**
     * Refresca la tabla de procesos con los datos actuales del modelo.
     */
    public void refreshProcesses() {
        // La tabla se sincroniza con el estado actual del modelo.
        processItems.setAll(controller.getProcesses());
        // Force table redraw so custom cell factories update styles immediately.
        processTable.refresh();
    }

    /**
     * Refresca el area de log con los eventos mas recientes.
     */
    public void refreshEvents() {
        // El log se reconstruye entero porque el usuario suele leerlo como historial cronologico.
        eventLogArea.setText(String.join(System.lineSeparator(), controller.getEventLog()));
    }

    /**
     * Refresca las metricas visibles en la interfaz.
     */
    public void refreshMetrics() {
        // Se muestran solo las metricas mas relevantes para no saturar la interfaz con datos tecnicos.
        ExecutionMetrics metrics = controller.getMetrics();
        metricsLabel.setText(String.format(
                "Procesos: %d | Terminados: %d | Espera promedio: %.2f | Retorno promedio: %.2f | Cambios de contexto: %d | CPU: %.2f%% | Throughput: %.4f",
                metrics.getTotalProcesses(),
                metrics.getCompletedProcesses(),
                metrics.getAverageWaitingTime(),
                metrics.getAverageTurnaroundTime(),
                metrics.getContextSwitches(),
                metrics.getCpuUtilization(),
                metrics.getThroughput()));
        // Mostrar recursos con unidades (memoria en MB asumida, CPU en unidades)
        var res = controller.getSystemState().getResource();
            resourceLabel.setText(String.format("Recursos: CPU total: %d nucleos | CPU usadas: %d | Memoria total: %d MB | Memoria usada: %d MB",
            res.getTotalCpu(), res.getUsedCpu(), res.getTotalMemory(), res.getUsedMemory()));
    }

    /**
     * Refresca toda la informacion visible en pantalla.
     */
    public void refreshAll() {
        // Un solo metodo evita olvidos cuando se actualizan tabla, log y metricas a la vez.
        refreshProcesses();
        refreshEvents();
        refreshMetrics();
        refreshSynchronizationState();
        refreshBurstsChart();
        refreshComparisonChart();
        if (syncDiagramPane != null) {
            renderSynchronizationDiagram("Estado actual de sincronizacion", Color.SLATEGRAY, 9001, 8001);
        }
        if (timelineRows != null) {
            // show the timeline up to current simulation time when not playing
            playTime = controller.getSystemState().getCurrentTime();
            renderTimeline(timelineRows, playTime);
        }
    }

    private void refreshBurstsChart() {
        if (burstsChart == null) return;
        burstsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("CPU Burst");
        controller.getProcesses().forEach(p -> series.getData().add(new XYChart.Data<>(String.valueOf(p.getPid()), p.getCpuBurst())));
        burstsChart.getData().add(series);
    }

    private void refreshComparisonChart() {
        if (comparisonChart == null) return;
        // keep existing data if user hasn't refreshed explicitly
    }

    private void refreshSynchronizationState() {
        // Se pinta como texto estructurado para facilitar la lectura durante la demo en clase.
        SynchronizationSnapshot snapshot = controller.getSynchronizationSnapshot();
        StringBuilder builder = new StringBuilder();
        builder.append("Comunicacion (SharedMemory)").append(System.lineSeparator());
        builder.append("- Valor actual: ").append(snapshot.getSharedValue()).append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Sincronizacion (Mutex)").append(System.lineSeparator());
        builder.append("- Bloqueado: ").append(snapshot.isMutexLocked()).append(System.lineSeparator());
        builder.append("- Propietario: ")
                .append(snapshot.getMutexOwnerPid() == null ? "ninguno" : snapshot.getMutexOwnerPid())
                .append(System.lineSeparator());
        builder.append("- Cola espera mutex: ").append(snapshot.getMutexWaitingPids()).append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Semaforo empty").append(System.lineSeparator());
        builder.append("- Valor: ").append(snapshot.getEmptySlotsValue()).append(System.lineSeparator());
        builder.append("- Esperando: ").append(snapshot.getEmptySlotsWaitingPids()).append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Semaforo filled").append(System.lineSeparator());
        builder.append("- Valor: ").append(snapshot.getFilledSlotsValue()).append(System.lineSeparator());
        builder.append("- Esperando: ").append(snapshot.getFilledSlotsWaitingPids()).append(System.lineSeparator());

        synchronizationStateArea.setText(builder.toString());
    }

    private void runSafeAction(Action action) {
        // Centraliza el manejo de excepciones de IO para no repetir codigo en cada boton.
        try {
            action.run();
            refreshAll();
        } catch (IOException exception) {
            eventLogArea.appendText(System.lineSeparator() + "Error: " + exception.getMessage());
        }
    }

    private void runProcessAction(ProcessAction action) {
        // Las acciones sobre un proceso requieren que el usuario seleccione uno antes.
        Process selected = processTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            eventLogArea.appendText(System.lineSeparator() + "Seleccione un proceso primero.");
            return;
        }

        action.run(selected);
        refreshAll();
    }

    private String formatNumber(double value) {
        // Se formatea a dos decimales para que las metricas se lean facilmente.
        return String.format("%.2f", value);
    }

    @FunctionalInterface
    private interface Action {
        void run() throws IOException;
    }

    @FunctionalInterface
    private interface ProcessAction {
        void run(Process process);
    }

    /**
     * Obtiene el contenedor raiz de la interfaz.
     *
     * @return nodo raiz de JavaFX.
     */
    public Parent getRoot() {
        return root;
    }
}
