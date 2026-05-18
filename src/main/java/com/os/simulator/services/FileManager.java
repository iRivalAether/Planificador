package com.os.simulator.services;

import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Maneja la importacion y exportacion simple de procesos en archivos de texto plano.
 */
public class FileManager {
    // Se usa formato CSV simple para que el archivo sea legible y fácil de editar manualmente.

    /**
     * Guarda procesos en un archivo de texto con formato CSV simple.
     * Cada proceso se serializa con columnas que representan su estado completo.
     *
     * @param path ruta de salida.
     * @param processes procesos a guardar.
     * @throws IOException si ocurre un error de escritura.
     */
    public void saveProcesses(Path path, List<Process> processes) throws IOException {
        // Abrimos un BufferedWriter en UTF-8; try-with-resources asegura cierre automático.
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // Cabecera CSV que describe cada columna para facilitar interpretación manual.
            writer.write("pid,name,state,priority,cpuBurst,cpuUsed,memoryRequired,arrivalTime,waitingTime,turnaroundTime,terminationReason,memoryAllocated");
            writer.newLine();

            // Para cada proceso escribimos una línea CSV con valores escapados donde es necesario.
            for (Process process : processes) {
                writer.write(process.getPid() + "," + process.getName() + "," + process.getState().name() + ","
                        + process.getPriority() + "," + process.getCpuBurst() + "," + process.getCpuUsed() + ","
                        + process.getMemoryRequired() + "," + process.getArrivalTime() + ","
                        + process.getWaitingTime() + "," + process.getTurnaroundTime() + ","
                        + escape(process.getTerminationReason()) + "," + process.isMemoryAllocated());
                writer.newLine(); // Salto de línea para la siguiente entrada.
            }
        }
    }

    /**
     * Carga procesos desde un archivo de texto plano con el formato esperado.
     * Lee todo el archivo y reconstruye instancias de `Process` desde las columnas.
     *
     * @param path ruta del archivo.
     * @return lista de procesos recuperados.
     * @throws IOException si ocurre un error de lectura.
     */
    public List<Process> loadProcesses(Path path) throws IOException {
        // Se leen todas las líneas porque el archivo suele ser pequeño y simplifica el código.
        List<Process> processes = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // Empezamos en 1 para saltar la cabecera CSV.
        for (int index = 1; index < lines.size(); index++) {
            String[] columns = lines.get(index).split(",");
            // Validación básica: si la línea no contiene suficientes columnas, la ignoramos.
            if (columns.length < 12) {
                continue;
            }

            // Reconstrucción del proceso leyendo columnas por índice según la cabecera.
            Process process = new Process(
                    Integer.parseInt(columns[0]), // pid
                    columns[1], // name
                    Integer.parseInt(columns[3]), // priority
                    Integer.parseInt(columns[4]), // cpuBurst
                    Integer.parseInt(columns[6]), // memoryRequired
                    Integer.parseInt(columns[7]) // arrivalTime
            );

            // Restaurar estado mutables desde las columnas restantes.
            process.setState(ProcessState.valueOf(columns[2]));
            process.setCpuUsed(Integer.parseInt(columns[5]));
            process.setWaitingTime(Integer.parseInt(columns[8]));
            process.setTurnaroundTime(Integer.parseInt(columns[9]));
            process.setTerminationReason(unescape(columns[10]));
            process.setMemoryAllocated(Boolean.parseBoolean(columns[11]));
            processes.add(process);
        }

        return processes;
    }

    // Escapa comas y saltos de línea en un string para que no rompan el CSV.
    private String escape(String value) {
        if (value == null) {
            return ""; // Normalizamos null a cadena vacía en el CSV.
        }
        return value.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }

    // Recuperación simple del valor escapado (en este diseño no invertimos el reemplazo).
    private String unescape(String value) {
        return value == null ? "" : value;
    }
}
