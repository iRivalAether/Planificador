package com.os.simulator.services;

import com.os.simulator.model.ExecutionMetrics;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y carga historiales simples de ejecucion en archivos de texto plano.
 */
public class ExecutionHistoryService {
    private final Path historyFile;

    public ExecutionHistoryService(Path historyFile) {
        this.historyFile = historyFile;
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
        if (historyFile.getParent() != null) Files.createDirectories(historyFile.getParent());
        String line = String.join(";",
            String.valueOf(System.currentTimeMillis()), algorithm, String.valueOf(processCount),
            String.valueOf(metrics.getAverageWaitingTime()), String.valueOf(metrics.getAverageTurnaroundTime()),
            String.valueOf(metrics.getContextSwitches()), String.valueOf(metrics.getCpuUtilization()));
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
        if (!Files.exists(historyFile)) return new ArrayList<>();
        List<String> records = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            String ln; while ((ln = r.readLine()) != null) if (!ln.isBlank()) records.add(ln);
        }
        return records;
    }
}
