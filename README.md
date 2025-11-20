# Omaha Poker con IA Q-Learning

Proyecto de Estructura de Datos - Sistema de póker Omaha con inteligencia artificial basada en Q-Learning y estructuras de datos avanzadas.

## Estructura del Proyecto

```
proyectoED/
├── src/                          # Código fuente Java
│   ├── Main.java                 # Punto de entrada principal
│   └── poker/
│       ├── core/                 # Clases fundamentales del juego
│       │   ├── Card.java
│       │   ├── Deck.java
│       │   ├── Hand.java
│       │   └── HandEvaluator.java
│       ├── ai/                   # Sistema de inteligencia artificial
│       │   ├── AIStrategy.java
│       │   ├── BossCharacter.java
│       │   └── SimpleMLEngine.java
│       └── game/                 # Lógica del juego
│           ├── OmahaPokerGame.java
│           ├── Player.java
│           ├── LevelManager.java
│           ├── ProfileManager.java
│           └── TournamentStats.java
│
├── bin/                          # Archivos compilados (.class)
├── profiles/                     # Perfiles de usuario (.profile)
├── data/                         # Datos de IA entrenados (.dat)
│
├── docs/                         # Documentación
│   ├── 1.tex                     # Documento LaTeX académico
│   ├── 1.pdf                     # PDF generado (93 páginas)
│   ├── *.md                      # Documentación técnica
│   └── *.txt                     # Guías y resúmenes
│
├── assets/                       # Recursos gráficos
│   ├── *.png                     # Capturas y diagramas
│   └── lince.png                 # Logo del proyecto
│
├── scripts/                      # Scripts auxiliares
│   ├── *.bat                     # Scripts de Windows
│   ├── *.ps1                     # Scripts PowerShell
│   └── *.py                      # Scripts Python
│
├── .gitignore                    # Archivos ignorados por Git
└── README.md                     # Este archivo
```

## Inicio Rápido

### Compilar el proyecto

```bash
javac -d bin -encoding UTF-8 src\Main.java src\poker\core\*.java src\poker\ai\*.java src\poker\game\*.java
```

O usar el script de compilación:
```bash
scripts\compilar.bat
```

### Ejecutar el juego

```bash
java -cp bin Main
```

O usar el script de ejecución:
```bash
scripts\ejecutar.bat
```

## Características

- **Juego completo de Omaha Poker** con reglas oficiales
- **6 bosses con IA Q-Learning** que aprenden tu estilo de juego
- **Sistema de perfiles múltiples** con persistencia
- **Estadísticas avanzadas** con PriorityQueue (Top-K hands)
- **Ranking de jugadores** con ordenamiento automático
- **Distribución de manos** usando TreeMap
- **Sistema de niveles progresivo**

## Estructuras de Datos Utilizadas

| Estructura | Uso | Complejidad |
|------------|-----|-------------|
| `HashMap<State, Double>` | Q-table para IA | O(1) |
| `PriorityQueue<HandRecord>` | Top-K mejores manos | O(log k) |
| `TreeMap<HandRank, Integer>` | Distribución ordenada | O(log n) |
| `ArrayList<Card>` | Manos y cartas comunitarias | O(1) amortizado |
| `LinkedList<String>` | Log de acciones | O(1) inserción |

## Algoritmos Implementados

- **Q-Learning** para entrenamiento de IA
- **Backtracking** para evaluación de manos Omaha
- **Heap Sort** (implícito en PriorityQueue)
- **Top-K optimizado** con heap limiting (83x más rápido que sort)
- **Epsilon-greedy** para exploration/exploitation

## Documentación

La documentación completa (93 páginas) está disponible en `docs/1.pdf` e incluye:

1. Planteamiento del problema
2. Justificación académica
3. Objetivos del proyecto
4. Marco teórico (ED + Q-Learning + Poker)
5. Elaboración del proyecto (UML, sprints, arquitectura)
6. Sistema de estadísticas con PriorityQueue
7. Demostración de funcionamiento
8. Visión del prototipo final
9. Análisis de resultados
10. Referencias bibliográficas
11. Anexos con código completo

## Requisitos

- **Java 8+** (recomendado Java 11 o superior)
- **Windows** (scripts .bat) o adaptable a Linux/Mac
- **Consola con soporte UTF-8** para caracteres especiales

## Perfiles de Usuario

El juego soporta hasta 3 perfiles simultáneos. Los datos se guardan en `profiles/`:
- Victorias y derrotas totales
- Nivel actual y progreso
- Datos de entrenamiento de IA por boss

## Sistema de Bosses

1. **El Novato** - Principiante (ε=0.3)
2. **La Calculadora** - Conservador (ε=0.25)
3. **El Tiburón** - Agresivo (ε=0.2)
4. **El Maestro Zen** - Balanceado (ε=0.15)
5. **El Loco** - Impredecible (ε=0.35)
6. **El Campeón Mundial** - Experto (ε=0.1)

## Métricas de Rendimiento

- Evaluación de mano Omaha: **0.003 ms**
- Decisión de IA: **0.15 ms**
- Registro en PriorityQueue: **0.0001 ms**
- Mejora sobre ArrayList+sort: **83x más rápido**

## Scripts Disponibles

- `scripts/compilar.bat` - Compilar proyecto
- `scripts/ejecutar.bat` - Ejecutar juego
- `scripts/jugar_v1.3.bat` - Versión específica
- `scripts/eliminar_unicode.py` - Limpieza de archivos
- `scripts/eliminar_simbolos_unicode.ps1` - Limpieza PowerShell

## Licencia

Proyecto académico - Universidad Tecnológica de la Costa  
Materia: Estructura de Datos  
Fecha: Noviembre 2025

## Autor

**Paulo Cesar Rivera Lara**  
Ingeniería en Desarrollo de Software

---

**Nota:** Los archivos `.dat` (datos de IA) y `.profile` (perfiles de usuario) se generan automáticamente y no deben versionarse en Git.
