package com.os.simulator;

import com.os.simulator.controllers.MainController;
import com.os.simulator.services.SimulationService;
import com.os.simulator.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicacion.
 * Inicia la ventana principal del simulador y conecta la vista con los controladores y servicios.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Se crean primero los servicios porque la vista depende de ellos para reaccionar a eventos.
        SimulationService simulationService = new SimulationService();
        MainController mainController = new MainController(simulationService);
        MainView mainView = new MainView(mainController);

        // La escena principal contiene toda la aplicacion para mantener una sola ventana consistente.
        Scene scene = new Scene(mainView.getRoot(), 1200, 800);

        primaryStage.setTitle("Simulador de Gestor de Procesos");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Lanzar JavaFX aqui es el punto de entrada clasico de una aplicacion de escritorio.
        launch(args);
    }
}
