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
    // Se usa formato CSV simple para que el archivo sea legible y facil de editar manualmente.
    /**
     * Guarda procesos en un archivo de texto con formato CSV simple.
     *
     * @param path ruta de salida.
     * @param processes procesos a guardar.
     * @throws IOException si ocurre un error de escritura.
     */
    public void saveProcesses(Path path, List<Process> processes) throws IOException {
        // Se escribe una cabecera porque facilita la importacion posterior y hace el archivo autoexplicativo.
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("pid,name,state,priority,cpuBurst,cpuUsed,memoryRequired,arrivalTime,waitingTime,turnaroundTime,terminationReason,memoryAllocated");
            writer.newLine();

            for (Process process : processes) {
                writer.write(process.getPid() + "," + process.getName() + "," + process.getState().name() + ","
                        + process.getPriority() + "," + process.getCpuBurst() + "," + process.getCpuUsed() + ","
                        + process.getMemoryRequired() + "," + process.getArrivalTime() + ","
                        + process.getWaitingTime() + "," + process.getTurnaroundTime() + ","
                        + escape(process.getTerminationReason()) + "," + process.isMemoryAllocated());
                writer.newLine();
            }
        }
    }

    /**
     * Carga procesos desde un archivo de texto plano.
     *
     * @param path ruta del archivo.
     * @return lista de procesos recuperados.
     * @throws IOException si ocurre un error de lectura.
     */
    public List<Process> loadProcesses(Path path) throws IOException {
        // Se leen todas las lineas porque el archivo es pequeno y la simplicidad importa mas que optimizar memoria.
        List<Process> processes = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (int index = 1; index < lines.size(); index++) {
            String[] columns = lines.get(index).split(",");
            if (columns.length < 12) {
                continue;
            }

            Process process = new Process(
                    Integer.parseInt(columns[0]),
                    columns[1],
                    Integer.parseInt(columns[3]),
                    Integer.parseInt(columns[4]),
                    Integer.parseInt(columns[6]),
                    Integer.parseInt(columns[7])
            );
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

    private String escape(String value) {
        // Se reemplazan comas y saltos de linea para no romper la estructura CSV.
        if (value == null) {
            return "";
        }
        return value.replace(",", " ").replace("\n", " ").replace("\r", " ");
    }

    private String unescape(String value) {
        // En esta version el escape es simple, por eso la recuperacion tambien es directa.
        return value == null ? "" : value;
    }
}
