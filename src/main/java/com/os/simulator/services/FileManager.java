package com.os.simulator.services;

import com.os.simulator.model.Process;
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
    /**
     * Guarda procesos en un archivo de texto con formato CSV simple.
     *
     * @param path ruta de salida.
     * @param processes procesos a guardar.
     * @throws IOException si ocurre un error de escritura.
     */
    public void saveProcesses(Path path, List<Process> processes) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("pid,name,priority,cpuBurst,memoryRequired,arrivalTime");
            writer.newLine();

            for (Process process : processes) {
                writer.write(process.getPid() + "," + process.getName() + "," + process.getPriority() + ","
                        + process.getCpuBurst() + "," + process.getMemoryRequired() + "," + process.getArrivalTime());
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
        List<Process> processes = new ArrayList<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (int index = 1; index < lines.size(); index++) {
            String[] columns = lines.get(index).split(",");
            if (columns.length < 6) {
                continue;
            }

            Process process = new Process(
                    Integer.parseInt(columns[0]),
                    columns[1],
                    Integer.parseInt(columns[2]),
                    Integer.parseInt(columns[3]),
                    Integer.parseInt(columns[4]),
                    Integer.parseInt(columns[5])
            );
            processes.add(process);
        }

        return processes;
    }
}
