package com.os.simulator.services;

// Paquete que agrupa clases relacionadas con los servicios de la simulación.

import com.os.simulator.model.ExecutionMetrics; // Modelo que contiene las métricas de ejecución calculadas.
import java.io.BufferedReader; // Lectura eficiente de texto desde un archivo.
import java.io.IOException; // Excepción lanzada para errores de E/S.
import java.nio.charset.StandardCharsets; // Charset UTF-8 para lectura/escritura de archivos.
import java.nio.file.Files; // Utilidades de E/S para archivos y directorios.
import java.nio.file.Path; // Representación de rutas en el sistema de ficheros.
import java.nio.file.StandardOpenOption; // Opciones para apertura/creación de archivos.
import java.util.ArrayList; // Implementación de lista dinámica.
import java.util.List; // Interfaz de listas.

/**
 * Guarda y carga historiales simples de ejecucion en archivos de texto plano.
 */
public class ExecutionHistoryService {
    // Ruta al archivo donde se almacena el historial de ejecuciones.
    // Se declara como final porque la ruta no debe cambiar una vez creada la instancia.
    private final Path historyFile;

    // Constructor: recibe la ruta del archivo de historial y la guarda en el campo.
    public ExecutionHistoryService(Path historyFile) {
        this.historyFile = historyFile; // Asignación directa de la ruta proporcionada.
    }

    /**
     * Agrega una ejecucion al historial.
     *
     * @param algorithm nombre del algoritmo ejecutado.
     * @param processCount cantidad de procesos.
     * @param metrics metricas calculadas.
     * @throws IOException si ocurre un error de escritura.
     */
    public void appendRecord(String algorithm, int processCount, ExecutionMetrics metrics) throws IOException {
        // Si el archivo está dentro de un directorio, aseguramos que dicho directorio exista.
        // `getParent()` devuelve la carpeta contenedora; si es null significa que la ruta es relativa a la raíz.
        if (historyFile.getParent() != null) Files.createDirectories(historyFile.getParent());

        // Construimos una única línea separada por ';' con los campos que queremos guardar.
        // Incluimos un timestamp (millis), el nombre del algoritmo, la cantidad de procesos
        // y las métricas (espera media, turnaround medio, cambios de contexto, utilización CPU).
        String line = String.join(";",
            String.valueOf(System.currentTimeMillis()), // Marca temporal de la ejecución en ms.
            algorithm, // Nombre del algoritmo ejecutado.
            String.valueOf(processCount), // Número de procesos usados en la ejecución.
            String.valueOf(metrics.getAverageWaitingTime()), // Tiempo de espera medio.
            String.valueOf(metrics.getAverageTurnaroundTime()), // Tiempo de retorno medio.
            String.valueOf(metrics.getContextSwitches()), // Número de cambios de contexto.
            String.valueOf(metrics.getCpuUtilization()) // Porcentaje o fracción de uso de CPU.
        );

        // Escribimos la línea en el archivo en UTF-8, creando el archivo si no existe
        // y añadiendo la línea al final (`APPEND`). Se añade también un salto de línea.
        Files.writeString(historyFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Carga el historial previamente almacenado en texto plano.
     *
     * @return lineas del historial; cada linea representa una ejecucion.
     * @throws IOException si ocurre un error de lectura.
     */
    public List<String> loadRecords() throws IOException {
        // Si el archivo de historial no existe todavía, devolvemos una lista vacía
        // en lugar de lanzar una excepción. Esto facilita el uso del servicio cuando
        // aún no se han registrado ejecuciones.
        if (!Files.exists(historyFile)) return new ArrayList<>();

        // Lista donde acumularemos las líneas leídas del archivo.
        List<String> records = new ArrayList<>();

        // Abrimos un BufferedReader con UTF-8 para leer el archivo de forma eficiente.
        try (BufferedReader r = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            // Leemos línea a línea; si la línea no está en blanco, la añadimos a la lista.
            String ln;
            while ((ln = r.readLine()) != null) {
                if (!ln.isBlank()) records.add(ln); // Ignora líneas vacías para mantener limpieza.
            }
        }

        // Devolvemos todas las líneas leídas (cada una representa un registro de ejecución).
        return records;
    }
}
