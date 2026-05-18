package com.os.simulator.services;

import com.os.simulator.model.ExecutionMetrics;
import com.os.simulator.model.Process;
import com.os.simulator.model.ProcessState;
import com.os.simulator.model.SystemState;
import com.os.simulator.services.synchronization.ProducerConsumerSimulation;
import com.os.simulator.services.synchronization.SynchronizationSnapshot;
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
    // Simulador de productor-consumidor para demostraciones de sincronizacion.
    private ProducerConsumerSimulation producerConsumerSimulation;

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
        this.producerConsumerSimulation = new ProducerConsumerSimulation();
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
     * Obtiene el simulador de productor-consumidor.
     *
     * @return instancia de ProducerConsumerSimulation.
     */
    public ProducerConsumerSimulation getProducerConsumerSimulation() {
        return producerConsumerSimulation;
    }

    /**
     * Reinicia la simulacion de productor-consumidor para dejar memoria, mutex y semaforos limpios.
     */
    public void resetProducerConsumerSimulation() {
        resetProducerConsumerSimulation(1);
    }

    /** Reinicia la demo de productor-consumidor con tamaño de buffer configurado. */
    public void resetProducerConsumerSimulation(int bufferSize) {
        this.producerConsumerSimulation = new ProducerConsumerSimulation(Math.max(1, bufferSize));
        systemState.logEvent("Demo Productor-Consumidor reiniciada (buffer=" + Math.max(1, bufferSize) + ").");
    }

    /**
     * Obtiene un snapshot del estado de sincronizacion/comunicacion para la UI.
     *
     * @return snapshot del estado actual.
     */
    public SynchronizationSnapshot getSynchronizationSnapshot() {
        var sync = producerConsumerSimulation.getSynchronizationService();
        var mem = producerConsumerSimulation.getSharedMemory();
        return new SynchronizationSnapshot(
            mem.dumpContents(),
            sync.isMutexLocked(),
            sync.getMutexOwnerPid(),
            new ArrayList<>(sync.getMutexWaiting()),
            sync.getEmptyValue(),
            new ArrayList<>(sync.getEmptyWaiting()),
            sync.getFilledValue(),
            new ArrayList<>(sync.getFilledWaiting()));
    }

    public int getProducerConsumerBufferCapacity() {
        try {
            return producerConsumerSimulation.getSharedMemory().getCapacity();
        } catch (Exception ex) {
            return 1;
        }
    }

    /**
     * Ejecuta una produccion en la simulacion de sincronizacion.
     *
     * @param pid identificador del productor.
     * @param value valor a producir.
     * @return true si logro producir.
     */
    public boolean produceInSharedMemory(int pid, String value) {
        // More detailed logging for producer actions
        var sync = producerConsumerSimulation.getSynchronizationService();
        systemState.logEvent("EVENT:PRODUCE_ATTEMPT PID " + pid);
        boolean acquired = sync.acquireProducerSlot(pid);
        if (!acquired) {
            systemState.logEvent("Productor PID " + pid + " bloqueado por empty (sin espacio)");
            systemState.logEvent("EVENT:PRODUCE_BLOCKED PID " + pid);
            return false;
        }

        systemState.logEvent("EVENT:PRODUCE_START PID " + pid);
        boolean entered = sync.enterCriticalSection(pid);
        if (!entered) {
            systemState.logEvent("Productor PID " + pid + " bloqueado por mutex");
            systemState.logEvent("EVENT:PRODUCE_BLOCKED_MUTEX PID " + pid);
            return false;
        }

        boolean ok = producerConsumerSimulation.getCommunicationService().write(pid, value);
        systemState.logEvent("Productor PID " + pid + " escribio en memoria compartida: " + value);
        systemState.logEvent("EVENT:PRODUCE PID " + pid);
        sync.leaveCriticalSection(pid);
        Integer released = sync.signalProduced();
        if (released != null) {
            systemState.logEvent("EVENT:UNBLOCK PID " + released + " BY PRODUCE PID " + pid);
        }
        systemState.logEvent("EVENT:PRODUCE_END PID " + pid);
        return ok;
    }

    /**
     * Ejecuta una lectura en la simulacion de sincronizacion.
     *
     * @param pid identificador del consumidor.
     * @return valor consumido o null.
     */
    public String consumeFromSharedMemory(int pid) {
        var sync = producerConsumerSimulation.getSynchronizationService();
        systemState.logEvent("EVENT:CONSUME_ATTEMPT PID " + pid);
        boolean acquired = sync.acquireConsumerSlot(pid);
        if (!acquired) {
            systemState.logEvent("Consumidor PID " + pid + " bloqueado por filled (sin datos)");
            systemState.logEvent("EVENT:CONSUME_BLOCKED PID " + pid);
            return null;
        }

        systemState.logEvent("EVENT:CONSUME_START PID " + pid);
        boolean entered = sync.enterCriticalSection(pid);
        if (!entered) {
            systemState.logEvent("Consumidor PID " + pid + " bloqueado por mutex");
            systemState.logEvent("EVENT:CONSUME_BLOCKED_MUTEX PID " + pid);
            return null;
        }

        String value = producerConsumerSimulation.getCommunicationService().read(pid);
        systemState.logEvent("Consumidor PID " + pid + " leyo desde memoria compartida: " + value);
        systemState.logEvent("EVENT:CONSUME PID " + pid);
        sync.leaveCriticalSection(pid);
        Integer released = sync.signalConsumed();
        if (released != null) {
            systemState.logEvent("EVENT:UNBLOCK PID " + released + " BY CONSUME PID " + pid);
        }
        systemState.logEvent("EVENT:CONSUME_END PID " + pid);
        return value;
    }

    /**
     * Ejecuta un escenario automatico de productor-consumidor.
     * Crea `numProducers` productores y `numConsumers` consumidores (PIDs separados),
     * y realiza `cycles` pasos donde cada actor intenta producir/consumir.
     */
    public void runProducerConsumerScenario(int numProducers, int numConsumers, int cycles) {
        int baseProducerPid = 8000; int baseConsumerPid = 9000;
        // Register demo processes for visibility in UI
        for (int i = 0; i < numProducers; i++) {
            int pid = baseProducerPid + i;
            addProcess(new com.os.simulator.model.Process(pid, "Producer-" + pid, 1, 1, 1, systemState.getCurrentTime()));
        }
        for (int i = 0; i < numConsumers; i++) {
            int pid = baseConsumerPid + i;
            addProcess(new com.os.simulator.model.Process(pid, "Consumer-" + pid, 1, 1, 1, systemState.getCurrentTime()));
        }

        // Simple round-robin attempts
        for (int c = 0; c < cycles; c++) {
            for (int i = 0; i < numProducers; i++) {
                int pid = baseProducerPid + i;
                String value = "val-" + c + "-p" + i;
                // log and attempt
                produceInSharedMemory(pid, value);
                systemState.advanceTime();
                if (systemState.detectDeadlock()) {
                    systemState.logEvent("DEADLOCK DETECTED durante escenario productor-consumidor.");
                    return;
                }
            }
            for (int i = 0; i < numConsumers; i++) {
                int pid = baseConsumerPid + i;
                consumeFromSharedMemory(pid);
                systemState.advanceTime();
                if (systemState.detectDeadlock()) {
                    systemState.logEvent("DEADLOCK DETECTED durante escenario productor-consumidor.");
                    return;
                }
            }
        }
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
        Process p = algorithmExecutionService.executeStep(this);
        if (systemState.detectDeadlock()) {
            systemState.logEvent("DEADLOCK DETECTED durante ejecucion de algoritmo.");
        }
        return p;
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
