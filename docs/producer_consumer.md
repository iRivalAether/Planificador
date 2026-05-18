Productor-Consumidor: descripción, problema y solución

1) Descripción

El problema Productor-Consumidor (producer-consumer) es un patrón clásico de sincronización en sistemas concurrentes. Consiste en dos tipos de agentes:

- Productores: generan datos y los colocan en un buffer (memoria compartida).
- Consumidores: leen y procesan datos del buffer.

El objetivo es coordinar acceso al buffer para evitar condiciones de carrera, lectura de datos inválidos o pérdida de información.

2) Problemas típicos

- Acceso simultáneo: si productor y consumidor acceden al buffer al mismo tiempo sin exclusión mutua, pueden corromperse los datos.
- Lectura sin datos: un consumidor puede intentar leer cuando el buffer está vacío.
- Escritura sin espacio: un productor puede intentar escribir cuando el buffer está lleno.

3) Solución clásica

Se usan tres mecanismos:

- Mutex (exclusión mutua): para proteger la sección crítica que accede al buffer.
- Semaphore "empty" (cuenta de espacios libres): controla si hay espacio para producir.
- Semaphore "filled" (cuenta de elementos disponibles): controla si hay elementos para consumir.

Algoritmo (productor):
1. wait(empty)   // esperar si no hay espacio
2. lock(mutex)   // entrada a sección crítica
3. colocar dato en buffer
4. unlock(mutex) // salida de sección crítica
5. signal(filled)

Algoritmo (consumidor):
1. wait(filled)  // esperar si buffer vacío
2. lock(mutex)
3. tomar dato del buffer
4. unlock(mutex)
5. signal(empty)

4) Cómo lo simulamos aquí

- `SharedMemory`: buffer lógico con una única celda (simplificación didáctica).
- `SynchronizationService`: mantiene un mutex y dos contadores semáforos (empty, filled). Guarda además colas de PIDs en espera para mostrar en la UI.
- `ProducerConsumerSimulation`: orquesta las operaciones de producir y consumir usando los primitives anteriores.
- Eventos: cada intento de producir/consumir deja una entrada en el registro del simulador, por ejemplo `EVENT:PRODUCE`, `EVENT:PRODUCE_BLOCKED`, `EVENT:CONSUME`, `EVENT:CONSUME_BLOCKED`. Estos eventos se muestran en la "Linea de tiempo".

5) Ejemplo ilustrativo (paso a paso)

Escenario: buffer inicialmente vacío.

1. Consumidor C llama a `consume()` → `filled==0` → queda bloqueado: registro `EVENT:CONSUME_BLOCKED PID C`.
2. Productor P llama a `produce(v)` → `empty>0` y `mutex` libre → escribe `v` en `SharedMemory` → registro `EVENT:PRODUCE PID P` y `signal(filled)`.
3. Al hacer `signal(filled)` se despierta al consumidor C (si estaba en cola), este entra a la sección crítica, lee `v`, y devuelve éxito `EVENT:CONSUME PID C`.

6) Visualización en la UI

- La pestaña "Sincronizacion" muestra el estado del `SharedMemory`, el estado del `mutex` y las colas de espera de los semáforos.
- La "Linea de tiempo" marca con puntos naranjas los eventos de produce/consume y dibuja cuando procesos acceden a la CPU.
- Un botón de "Demo Productor-Consumidor" ejecuta una secuencia simplificada: consumidor intenta leer (queda bloqueado), productor escribe (despierta consumidor), consumidor lee y ambos registran eventos para que usted vea la secuencia.

7) Recomendaciones pedagógicas

- Para experimentar: use varios consumidores/productores con PIDs distintos desde la UI y vea las colas de espera.
- Observe la diferencia entre `EVENT:CONSUME_BLOCKED` y `EVENT:CONSUME` en la linea de tiempo para entender quién queda en espera y cuando se le concede el recurso.

8) Referencias breves

- Tanenbaum, A. "Operating Systems" — sección sobre sincronización y semáforos.
- Silberschatz, "Operating System Concepts" — ejemplos clásicos del productor-consumidor.
