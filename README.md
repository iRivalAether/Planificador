# Simulador de Gestor de Procesos

Proyecto educativo en Java con JavaFX para simular la planificacion de procesos de un sistema operativo.

## Estructura
- `model`: entidades y estados del simulador
- `view`: interfaz grafica
- `controllers`: controladores de la interfaz
- `services`: logica de simulacion, planificacion, sincronizacion e importacion/exportacion

## Requisitos
- Java 17
- Maven

## Ejecucion
```bash
mvn javafx:run
```

## Caracteristicas iniciales
- Creacion manual de procesos
- Generacion aleatoria de procesos
- Planificadores FCFS, SJF, Round Robin y Prioridades
- Simulacion basica paso a paso
- Memoria compartida y mutex simulados
- Guardado simple en archivos de texto

## Documentacion
La documentacion tecnica esta separada en dos carpetas dentro de `docs`.

### `docs/explicacion`
Diagramas que explican el sistema, sus casos de uso y la sincronizacion.

- `casos_de_uso.puml`
- `flujo_aplicacion.puml`
- `sincronizacion_general.puml`
- `comunicacion_memoria_compartida.puml`
- `productor_consumidor.puml`
- `memoria_compartida_lectura_escritura.puml`
- `estados_proceso.puml`

### `docs/funcionamiento`
Diagramas que muestran como opera internamente el programa y cada algoritmo.

- `algoritmo_fcfs.puml`
- `algoritmo_sjf.puml`
- `algoritmo_round_robin.puml`
- `algoritmo_prioridad.puml`
- `flujo_motor_simulacion.puml`

Cada carpeta tiene un proposito distinto para evitar mezclar la explicacion conceptual con el funcionamiento interno del simulador.
