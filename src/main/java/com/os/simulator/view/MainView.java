package com.os.simulator.view;

import com.os.simulator.controllers.MainController;
import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import java.io.IOException;
import java.nio.file.Path;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;

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
        tabPane.getTabs().add(buildComparisonTab());
        tabPane.getTabs().add(buildHistoryTab());
        return tabPane;
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

        processTable.getColumns().setAll(pidColumn, nameColumn, stateColumn, priorityColumn, cpuColumn);
        processTable.setItems(processItems);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        eventLogArea.setEditable(false);
        eventLogArea.setPrefRowCount(10);

        VBox container = new VBox(10,
            processTable,
            new Label("Metricas actuales"),
            metricsLabel,
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
        form.add(new Label("CPU"), 0, 2);
        form.add(cpuField, 1, 2);
        form.add(new Label("Memoria"), 0, 3);
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

        VBox container = new VBox(10, stepButton, runAllButton, compareButton, suspendButton, resumeButton, terminateButton);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildComparisonTab() {
        Tab tab = new Tab("Comparacion");
        tab.setClosable(false);
        tab.setContent(new VBox(new Label("Se agregara la comparacion de algoritmos en la siguiente iteracion.")));
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
    }

    /**
     * Refresca toda la informacion visible en pantalla.
     */
    public void refreshAll() {
        // Un solo metodo evita olvidos cuando se actualizan tabla, log y metricas a la vez.
        refreshProcesses();
        refreshEvents();
        refreshMetrics();
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
