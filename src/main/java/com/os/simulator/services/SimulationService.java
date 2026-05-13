package com.os.simulator.services;

import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordina la ejecucion del simulador y delega la logica especifica de cada algoritmo
 * en servicios dedicados. Tambien centraliza importacion, exportacion, historial y metricas.
 */
public class SimulationService {
    // Capacidad base de CPU usada en las simulaciones principales.
    private static final int DEFAULT_TOTAL_CPU = 8;
    // Capacidad base de memoria para no depender de configuracion externa al iniciar.
    private static final int DEFAULT_TOTAL_MEMORY = 8192;
    // Quantum por defecto cuando el usuario elige Round Robin sin configurar otro valor.
    private static final int DEFAULT_ROUND_ROBIN_QUANTUM = 2;
    // Limite de seguridad para evitar ciclos infinitos en una simulacion mal configurada.
    private static final int MAX_SIMULATION_STEPS = 100000;

    // Genera procesos aleatorios para pruebas y comparaciones.
    private final ProcessGenerator processGenerator;
    // Gestiona lectura y escritura de archivos txt.
    private final FileManager fileManager;
    // Guarda el historial de ejecuciones en disco.
    private final ExecutionHistoryService executionHistoryService;
    // Estado global que conserva procesos, cola, recursos y reloj.
    private SystemState systemState;
    // Algoritmo activo que decide el orden de atencion.
    private Scheduler scheduler;
    // Service especializado que contiene la logica de ejecucion del algoritmo activo.
    private AlgorithmExecutionService algorithmExecutionService;
    // PID siguiente para garantizar unicidad dentro del simulador.
    private int nextPid;
    // Cuenta los cambios de contexto para calcular una metrica comparativa.
    private int contextSwitches;
    // Acumula las unidades de CPU realmente ejecutadas.
    private int executedCpuUnits;
    // Recuerda el ultimo proceso despachado para medir cambios de contexto.
    private Process lastDispatchedProcess;

    /**
     * Crea el servicio con FCFS por defecto.
     */
    public SimulationService() {
        this(new FCFSScheduler());
    }

    /**
     * Crea el servicio con un planificador inicial especifico.
     *
     * @param initialScheduler planificador inicial.
     */
    public SimulationService(Scheduler initialScheduler) {
        // Se inicializan los servicios auxiliares antes del estado para que todo este listo al arrancar.
        this.processGenerator = new ProcessGenerator();
        this.fileManager = new FileManager();
        this.executionHistoryService = new ExecutionHistoryService(Path.of("data", "execution_history.txt"));
        this.systemState = new SystemState(DEFAULT_TOTAL_CPU, DEFAULT_TOTAL_MEMORY);
        this.nextPid = 1;
        this.contextSwitches = 0;
        this.executedCpuUnits = 0;
        this.lastDispatchedProcess = null;
        setScheduler(initialScheduler == null ? new FCFSScheduler() : initialScheduler);
    }

    /**
     * Reinicia el estado del simulador sin cambiar el algoritmo activo.
     */
    public void resetSimulation() {
        // Se reinicia solo el estado de ejecucion; el algoritmo activo se conserva por conveniencia del usuario.
        this.systemState = new SystemState(DEFAULT_TOTAL_CPU, DEFAULT_TOTAL_MEMORY);
        this.nextPid = 1;
        this.contextSwitches = 0;
        this.executedCpuUnits = 0;
        this.lastDispatchedProcess = null;
    }

    /**
     * Crea y registra un proceso manual en el simulador.
     *
     * @param name nombre del proceso.
     * @param priority prioridad.
     * @param cpuBurst rafaga de CPU.
     * @param memoryRequired memoria requerida.
     */
    public void addProcess(String name, int priority, int cpuBurst, int memoryRequired) {
        // Los procesos manuales se crean con el tiempo actual como referencia de llegada.
        Process process = new Process(nextPid++, name, priority, cpuBurst, memoryRequired, systemState.getCurrentTime());
        addProcess(process);
    }

    /**
     * Registra un proceso existente en el simulador.
     *
     * @param process proceso a registrar.
     */
    public void addProcess(Process process) {
        // Se clona el proceso para que cada simulacion trabaje con sus propios datos.
        if (process == null) {
            return;
        }

        Process copy = process.copy();
        systemState.addProcess(copy);
        nextPid = Math.max(nextPid, copy.getPid() + 1);
    }

    /**
     * Genera y agrega procesos aleatorios al sistema.
     *
     * @param quantity cantidad de procesos.
     */
    public void generateRandomProcesses(int quantity) {
        // Esta variante usa rangos por defecto para probar el sistema rapidamente.
        List<Process> processes = processGenerator.generateProcesses(quantity);
        for (Process process : processes) {
            Process copy = new Process(nextPid++, process.getName(), process.getPriority(), process.getCpuBurst(),
                    process.getMemoryRequired(), process.getArrivalTime());
            addProcess(copy);
        }
    }

    /**
     * Genera procesos aleatorios con rangos configurables.
     *
     * @param quantity cantidad de procesos.
     * @param minPriority prioridad minima.
     * @param maxPriority prioridad maxima.
     * @param minCpu rafaga minima de CPU.
     * @param maxCpu rafaga maxima de CPU.
     * @param minMemory memoria minima.
     * @param maxMemory memoria maxima.
     */
    public void generateRandomProcesses(int quantity, int minPriority, int maxPriority, int minCpu, int maxCpu, int minMemory, int maxMemory) {
        // Esta variante permite construir escenarios controlados para comparar algoritmos.
        List<Process> processes = processGenerator.generateProcesses(quantity, minPriority, maxPriority, minCpu, maxCpu, minMemory, maxMemory);
        for (Process process : processes) {
            Process copy = new Process(nextPid++, process.getName(), process.getPriority(), process.getCpuBurst(),
                    process.getMemoryRequired(), process.getArrivalTime());
            addProcess(copy);
        }
    }

    /**
     * Ejecuta un paso de simulacion delegando la logica al servicio del algoritmo activo.
     *
     * @return proceso ejecutado o null si no habia trabajo disponible.
     */
    public Process runStep() {
        // El coordinador no decide el detalle; solo delega al service del algoritmo activo.
        return algorithmExecutionService.executeStep(this);
    }

    /**
     * Ejecuta la simulacion completa hasta terminar todos los procesos o alcanzar un limite de seguridad.
     *
     * @return metricas finales de la ejecucion.
     */
    public ExecutionMetrics runUntilComplete() {
        return runUntilComplete(true);
    }

    /**
     * Ejecuta la simulacion completa con control opcional de historial.
     *
     * @param recordHistory true si debe guardar la ejecucion final en el historial.
     * @return metricas finales.
     */
    public ExecutionMetrics runUntilComplete(boolean recordHistory) {
        // Se avanza paso a paso porque asi se conserva la misma logica que la ejecucion manual.
        int guard = 0;
        while (hasPendingWork() && guard < MAX_SIMULATION_STEPS) {
            runStep();
            guard++;
        }

        ExecutionMetrics metrics = calculateMetrics();
        if (recordHistory) {
            appendExecutionToHistory();
        }
        return metrics;
    }

    /**
     * Cambia el algoritmo activo.
     *
     * @param scheduler nuevo planificador.
     */
    public void setScheduler(Scheduler scheduler) {
        // Cambiar el scheduler tambien debe cambiar la clase que implementa su comportamiento detallado.
        this.scheduler = scheduler == null ? new FCFSScheduler() : scheduler;
        this.algorithmExecutionService = resolveExecutionService(this.scheduler);
        systemState.logEvent("Planificador activo: " + this.scheduler.getName());
    }

    /**
     * Cambia el algoritmo activo por su nombre.
     *
     * @param schedulerName nombre del algoritmo.
     */
    public void setSchedulerByName(String schedulerName) {
        // El usuario interactua por nombre legible, asi que aqui se traduce a una clase concreta.
        if (schedulerName == null) {
            setScheduler(new FCFSScheduler());
            return;
        }

        switch (schedulerName) {
            case "SJF":
                setScheduler(new SJFScheduler());
                break;
            case "Round Robin":
                setScheduler(new RoundRobinScheduler(DEFAULT_ROUND_ROBIN_QUANTUM));
                break;
            case "Prioridad":
                setScheduler(new PriorityScheduler());
                break;
            default:
                setScheduler(new FCFSScheduler());
                break;
        }
    }

    /**
     * Suspende un proceso por PID.
     *
     * @param pid identificador del proceso.
     * @return true si logro suspenderse.
     */
    public boolean suspendProcess(int pid) {
        // Suspender significa sacar al proceso de la ejecucion sin borrarlo del sistema.
        boolean suspended = systemState.suspendProcess(pid);
        if (suspended && lastDispatchedProcess != null && lastDispatchedProcess.getPid() == pid) {
            lastDispatchedProcess = null;
        }
        return suspended;
    }

    /**
     * Reanuda un proceso por PID.
     *
     * @param pid identificador del proceso.
     * @return true si logro reanudarse.
     */
    public boolean resumeProcess(int pid) {
        // Reanudar intenta devolver al proceso a READY respetando la disponibilidad de memoria.
        return systemState.resumeProcess(pid);
    }

    /**
     * Termina un proceso por PID.
     *
     * @param pid identificador del proceso.
     * @param reason motivo de la terminacion.
     * @return true si existia y se termino.
     */
    public boolean terminateProcess(int pid, String reason) {
        // La terminacion manual debe ser controlada para liberar memoria y cerrar el ciclo del proceso.
        boolean terminated = systemState.terminateProcess(pid, reason);
        if (terminated && lastDispatchedProcess != null && lastDispatchedProcess.getPid() == pid) {
            lastDispatchedProcess = null;
        }
        return terminated;
    }

    /**
     * Importa procesos desde un archivo y reemplaza el estado actual.
     *
     * @param path ruta del archivo.
     * @throws IOException si ocurre un error de lectura.
     */
    public void importProcesses(Path path) throws IOException {
        // Importar reemplaza la corrida actual porque el archivo representa una nueva carga de trabajo.
        List<Process> processes = fileManager.loadProcesses(path);
        resetSimulation();
        for (Process process : processes) {
            addProcess(process);
        }
    }

    /**
     * Exporta los procesos actuales a un archivo de texto plano.
     *
     * @param path ruta de salida.
     * @throws IOException si ocurre un error de escritura.
     */
    public void exportProcesses(Path path) throws IOException {
        // Exportar copia el estado actual para que pueda reutilizarse luego.
        fileManager.saveProcesses(path, new ArrayList<>(systemState.getProcesses()));
    }

    /**
     * Calcula las metricas actuales del sistema.
     *
     * @return metricas de ejecucion.
     */
    public ExecutionMetrics calculateMetrics() {
        // Las metricas se calculan desde el estado global para reflejar exactamente la simulacion visible.
        ExecutionMetrics metrics = new ExecutionMetrics();
        List<Process> processes = systemState.getProcesses();

        metrics.setTotalProcesses(processes.size());
        metrics.setCompletedProcesses((int) processes.stream().filter(Process::isFinished).count());
        metrics.setAverageWaitingTime(processes.stream().mapToInt(Process::getWaitingTime).average().orElse(0));
        metrics.setAverageTurnaroundTime(processes.stream()
                .filter(Process::isFinished)
                .mapToInt(Process::getTurnaroundTime)
                .average()
                .orElse(0));
        metrics.setContextSwitches(contextSwitches);
        metrics.setTotalTime(systemState.getCurrentTime());
        metrics.setCpuUtilization(systemState.getCurrentTime() == 0
                ? 0
                : (executedCpuUnits * 100.0) / systemState.getCurrentTime());
        metrics.setThroughput(systemState.getCurrentTime() == 0
                ? 0
                : metrics.getCompletedProcesses() / (double) systemState.getCurrentTime());
        return metrics;
    }

    /**
     * Compara los algoritmos principales con los procesos actuales.
     *
     * @return mapa con el nombre del algoritmo y sus metricas.
     */
    public Map<String, ExecutionMetrics> compareAlgorithms() {
        return compareAlgorithms(new ArrayList<>(systemState.getProcesses()));
    }

    /**
     * Compara los algoritmos principales usando una lista de procesos base.
     *
     * @param baselineProcesses procesos base.
     * @return mapa con el nombre del algoritmo y sus metricas.
     */
    public Map<String, ExecutionMetrics> compareAlgorithms(List<Process> baselineProcesses) {
        // Cada algoritmo se ejecuta sobre una copia independiente para no mezclar resultados.
        Map<String, ExecutionMetrics> results = new LinkedHashMap<>();
        results.put("FCFS", simulateWithScheduler(baselineProcesses, new FCFSScheduler()));
        results.put("SJF", simulateWithScheduler(baselineProcesses, new SJFScheduler()));
        results.put("Round Robin", simulateWithScheduler(baselineProcesses, new RoundRobinScheduler(DEFAULT_ROUND_ROBIN_QUANTUM)));
        results.put("Prioridad", simulateWithScheduler(baselineProcesses, new PriorityScheduler()));
        return results;
    }

    /**
     * Guarda la ejecucion actual en el historial.
     */
    public void appendExecutionToHistory() {
        // El historial permite comparar corridas en distintos momentos sin depender de memoria.
        try {
            executionHistoryService.appendRecord(getSchedulerName(), systemState.getProcesses().size(), calculateMetrics());
        } catch (IOException exception) {
            systemState.logEvent("No se pudo guardar el historial: " + exception.getMessage());
        }
    }

    /**
     * Carga el historial almacenado.
     *
     * @return registros del historial.
     * @throws IOException si ocurre un error de lectura.
     */
    public List<String> loadHistory() throws IOException {
        return executionHistoryService.loadRecords();
    }

    /**
     * Obtiene el estado global del sistema.
     *
     * @return estado del sistema.
     */
    public SystemState getSystemState() {
        return systemState;
    }

    /**
     * Obtiene el planificador activo.
     *
     * @return planificador actual.
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Obtiene el nombre del algoritmo activo.
     *
     * @return nombre del algoritmo.
     */
    public String getSchedulerName() {
        return scheduler.getName();
    }

    /**
     * Indica si existe trabajo pendiente.
     *
     * @return true si aun hay procesos sin terminar.
     */
    public boolean hasPendingWork() {
        return systemState.getProcesses().stream().anyMatch(process -> process.getState() != ProcessState.TERMINATED);
    }

    /**
     * Incrementa el contador de cambios de contexto si el proceso cambia respecto al ultimo ejecutado.
     *
     * @param process proceso que esta por ejecutarse.
     */
    void registerContextSwitch(Process process) {
        // Un cambio de proceso en CPU incrementa el contador de cambios de contexto.
        if (lastDispatchedProcess != null && lastDispatchedProcess.getPid() != process.getPid()) {
            contextSwitches++;
        }
    }

    /**
     * Aumenta el conteo de CPU ejecutada.
     *
     * @param units unidades de CPU ejecutadas.
     */
    void incrementExecutedCpuUnits(int units) {
        // Se acumula CPU para calcular utilizacion real respecto al tiempo transcurrido.
        executedCpuUnits += Math.max(units, 0);
    }

    /**
     * Define el ultimo proceso despachado.
     *
     * @param process proceso despachado.
     */
    void setLastDispatchedProcess(Process process) {
        // El ultimo proceso despachado se usa para detectar el siguiente cambio de contexto.
        this.lastDispatchedProcess = process;
    }

    /**
     * Limpia el ultimo proceso despachado.
     */
    void clearLastDispatchedProcess() {
        // Se limpia cuando no hubo proceso valido o cuando termina una interaccion de ejecucion.
        this.lastDispatchedProcess = null;
    }

    /**
     * Obtiene el quantum de Round Robin.
     *
     * @return quantum configurado.
     */
    int getRoundRobinQuantum() {
        // Si el scheduler activo no es Round Robin, se retorna el valor por defecto para no romper la logica.
        if (scheduler instanceof RoundRobinScheduler) {
            return Math.max(((RoundRobinScheduler) scheduler).getQuantum(), 1);
        }
        return DEFAULT_ROUND_ROBIN_QUANTUM;
    }

    /**
     * Finaliza un proceso y libera sus recursos asociados.
     *
     * @param process proceso finalizado.
     * @param reason causa de terminacion.
     */
    void finishProcess(Process process, String reason) {
        // El cierre del proceso se centraliza para no repetir liberacion y registro en cada algoritmo.
        process.setState(ProcessState.TERMINATED);
        process.setTerminationReason(reason);
        process.setTurnaroundTime(systemState.getCurrentTime() - process.getArrivalTime());
        systemState.releaseResourcesIfNeeded(process);
        systemState.logEvent("Proceso terminado: PID " + process.getPid() + " - " + reason);
    }

    private AlgorithmExecutionService resolveExecutionService(Scheduler scheduler) {
        // Cada scheduler tiene un service de ejecucion dedicado para separar seleccion de comportamiento.
        if (scheduler instanceof SJFScheduler) {
            return new SjfExecutionService();
        }

        if (scheduler instanceof RoundRobinScheduler) {
            return new RoundRobinExecutionService();
        }

        if (scheduler instanceof PriorityScheduler) {
            return new PriorityExecutionService();
        }

        return new FcfsExecutionService();
    }

    private ExecutionMetrics simulateWithScheduler(List<Process> baselineProcesses, Scheduler schedulerToUse) {
        // Se crea una simulacion temporal para comparar algoritmos sin alterar la corrida principal.
        SimulationService temporaryService = new SimulationService(schedulerToUse);
        temporaryService.resetSimulation();
        for (Process process : baselineProcesses) {
            temporaryService.addProcess(process.copy());
        }
        return temporaryService.runUntilComplete(false);
    }
}
