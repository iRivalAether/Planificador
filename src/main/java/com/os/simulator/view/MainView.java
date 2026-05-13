package com.os.simulator.view;

import com.os.simulator.controllers.MainController;
import com.os.simulator.model.Process;
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
    private final BorderPane root;
    private final ObservableList<Process> processItems;
    private final MainController controller;
    private final TextArea eventLogArea;
    private final TableView<Process> processTable;

    /**
     * Crea la vista principal y arma todos los componentes visuales.
     *
     * @param controller controlador principal del simulador.
     */
    public MainView(MainController controller) {
        this.controller = controller;
        this.root = new BorderPane();
        this.processItems = FXCollections.observableArrayList();
        this.eventLogArea = new TextArea();
        this.processTable = new TableView<>();
        buildInterface();
    }

    private void buildInterface() {
        root.setPadding(new Insets(10));
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        root.setBottom(buildStatusBar());
    }

    private Parent buildHeader() {
        Label title = new Label("Simulador de Gestor de Procesos");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox header = new VBox(title);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private Parent buildTabs() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(buildDashboardTab());
        tabPane.getTabs().add(buildConfigurationTab());
        tabPane.getTabs().add(buildExecutionTab());
        tabPane.getTabs().add(buildComparisonTab());
        tabPane.getTabs().add(buildHistoryTab());
        return tabPane;
    }

    private Tab buildDashboardTab() {
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

        VBox container = new VBox(10, processTable, new Label("Registro de eventos"), eventLogArea);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildConfigurationTab() {
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
            controller.generateRandomProcesses(Integer.parseInt(randomCountField.getText()));
            refreshProcesses();
            refreshEvents();
        });

        Button schedulerButton = new Button("Aplicar planificador");
        schedulerButton.setOnAction(event -> controller.setScheduler(schedulerCombo.getValue()));

        HBox buttons = new HBox(10, addButton, randomButton, schedulerButton);
        VBox container = new VBox(15, form, buttons);
        container.setPadding(new Insets(10));
        tab.setContent(container);
        return tab;
    }

    private Tab buildExecutionTab() {
        Tab tab = new Tab("Ejecucion");
        tab.setClosable(false);

        Button stepButton = new Button("Ejecutar paso");
        stepButton.setOnAction(event -> {
            controller.runStep();
            refreshProcesses();
            refreshEvents();
        });

        Label metricsLabel = new Label("Las metricas se actualizaran al ejecutar pasos.");
        VBox container = new VBox(10, stepButton, metricsLabel);
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
        processItems.setAll(controller.getProcesses());
    }

    /**
     * Refresca el area de log con los eventos mas recientes.
     */
    public void refreshEvents() {
        eventLogArea.setText(String.join(System.lineSeparator(), controller.getEventLog()));
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
