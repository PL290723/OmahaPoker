import poker.core.*;
import poker.game.*;
import poker.game.LevelManager.Level;
import poker.ai.*;
import java.util.*;

/**
 * Clase principal del juego de Omaha Poker con interfaz de consola
 */
public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static LevelManager levelManager;
    private static Player humanPlayer;
    private static OmahaPokerGame currentGame;
    private static ProfileManager profileManager;
    private static ProfileManager.Profile currentProfile;
    private static final String SAVE_FILE = "ia_learning_data.txt";
    
    public static void main(String[] args) {
        showWelcome();
        
        // Inicializar gestor de perfiles
        profileManager = new ProfileManager();
        
        // Seleccionar o crear perfil
        if (!selectOrCreateProfile()) {
            System.out.println("No se pudo iniciar el juego.");
            return;
        }
        
        // Inicializar jugador humano con datos del perfil
        humanPlayer = new Player(currentProfile.name, currentProfile.totalChips, Player.PlayerType.HUMAN);
        
        // Inicializar sistema de niveles
        levelManager = new LevelManager();
        
        // Cargar progreso del perfil en el level manager
        loadProfileProgress();
        
        // Preguntar si quiere pre-entrenar la IA (solo si es nuevo o no tiene datos)
        askForPreTraining();
        
        // Menú principal
        boolean running = true;
        while (running) {
            showMainMenu();
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    playLevel();
                    break;
                case "2":
                    showLevels();
                    break;
                case "3":
                    showStatistics();
                    break;
                case "4":
                    showTutorial();
                    break;
                case "5":
                    showMLReport();
                    break;
                case "6":
                    showCardSymbols();
                    break;
                case "7":
                    showProfilesMenu();
                    break;
                case "8":
                    showTournamentStats();
                    break;
                case "9":
                    saveAllBossesLearning();
                    saveProfileProgress();
                    System.out.println("\nGracias por jugar! Hasta pronto.");
                    running = false;
                    break;
                default:
                    System.out.println("Opción inválida. Intenta nuevamente.");
            }
        }
        
        scanner.close();
    }
    
    private static void showWelcome() {
        System.out.println("\n");
        System.out.println("+===========================================================+");
        System.out.println("|                                                           |");
        System.out.println("|          OMAHA POKER - DESAFIO DE CAMPEONES               |");
        System.out.println("|                                                           |");
        System.out.println("|     Un juego con IA adaptativa y aprendizaje automatico   |");
        System.out.println("|                                                           |");
        System.out.println("+===========================================================+");
        System.out.println();
    }
    
    private static void showMainMenu() {
        System.out.println("\n+===============================================+");
        System.out.println("|              MENU PRINCIPAL                   |");
        System.out.println("+===============================================+");
        System.out.println("|  Perfil: " + String.format("%-36s", currentProfile.name) + "|");
        System.out.println("|  Fichas: $" + String.format("%-34d", humanPlayer.getChips()) + "|");
        System.out.println("+===============================================+");
        System.out.println("|  1. Jugar Nivel                               |");
        System.out.println("|  2. Ver Niveles y Progreso                    |");
        System.out.println("|  3. Estadisticas                              |");
        System.out.println("|  4. Tutorial de Omaha Poker                   |");
        System.out.println("|  5. Ver Reporte de Aprendizaje de IA          |");
        System.out.println("|  6. Acerca de los Simbolos de Cartas          |");
        System.out.println("|  7. Gestionar Perfiles                        |");
        System.out.println("|  8. Ver Estadisticas del Torneo (PriorityQueue)|");
        System.out.println("|  9. Salir                                     |");
        System.out.println("+===============================================+");
        System.out.print("\nSelecciona una opcion: ");
    }
    
    private static void playLevel() {
        System.out.println("\n" + levelManager.getProgressReport());
        System.out.print("\nSelecciona un nivel (1-" + levelManager.getTotalLevels() + "): ");
        
        try {
            int levelNum = Integer.parseInt(scanner.nextLine());
            
            if (!levelManager.selectLevel(levelNum)) {
                System.out.println("\n No puedes jugar ese nivel todavía. Debe estar desbloqueado.");
                return;
            }
            
            LevelManager.Level level = levelManager.getCurrentLevel();
            BossCharacter boss = level.getBoss();
            
            // Mostrar información del boss
            System.out.println("\n+===============================================+");
            System.out.println("|          !DESAFIO DE NIVEL!                   |");
            System.out.println("+===============================================+");
            System.out.println("|  " + level.getName());
            System.out.println("|");
            System.out.println("|  Boss: " + boss.getName());
            System.out.println("|  " + boss.getDescription());
            System.out.println("|  Dificultad: " + "*".repeat(boss.getDifficulty()));
            System.out.println("|  Estilo: " + boss.getPrimaryStyle().name());
            System.out.println("|");
            System.out.println("|  Habilidad Especial:");
            System.out.println("|  " + boss.getSpecialAbility());
            System.out.println("|");
            System.out.println("|  Ciegas: " + level.getBigBlind()/2 + "/" + level.getBigBlind());
            System.out.println("|  Victorias requeridas: " + level.getRequiredWins());
            System.out.println("+===============================================+");
            
            System.out.print("\nListo para jugar? (s/n): ");
            if (!scanner.nextLine().equalsIgnoreCase("s")) {
                return;
            }
            
            // Reiniciar fichas del jugador para este nivel
            humanPlayer.addChips(Math.max(0, 5000 - humanPlayer.getChips()));
            
            // Jugar serie de manos
            playSession(level);
            
        } catch (NumberFormatException e) {
            System.out.println("Entrada inválida.");
        }
    }
    
    private static void playSession(LevelManager.Level level) {
        BossCharacter boss = level.getBoss();
        int handsPlayed = 0;
        int sessionWins = 0;
        int sessionLosses = 0;
        
        // Mostrar estado inicial de la IA
        System.out.println("\n[IA] Estado inicial - Exploración: " + 
                          String.format("%.0f%%", boss.getMlEngine().getExplorationRate() * 100));
        System.out.println("[IA] Decisiones aprendidas: " + boss.getMlEngine().getTotalDecisions());
        
        while (humanPlayer.getChips() > 0 && boss.getStartingChips() > 0) {
            System.out.println("\n\n");
            System.out.println("===============================================");
            System.out.println("    MANO #" + (handsPlayed + 1));
            System.out.println("===============================================");
            System.out.println(humanPlayer.getName() + ": $" + humanPlayer.getChips());
            System.out.println(boss.getName() + ": $" + boss.getStartingChips());
            
            // Mostrar si la IA ha adaptado su estrategia
            if (handsPlayed > 0 && handsPlayed % 3 == 0) {
                System.out.println("\n[IA] Estado de aprendizaje:");
                System.out.println("    Estilo: " + boss.getAdaptiveStyle().name());
                System.out.println("    Exploración: " + 
                                 String.format("%.0f%%", boss.getMlEngine().getExplorationRate() * 100));
                System.out.println("    Experiencias: " + boss.getMlEngine().getTotalExperiences());
                
                if (boss.getAdaptiveStyle() != boss.getPrimaryStyle()) {
                    System.out.println("    [!] La IA ha detectado tu estilo y se está adaptando!");
                }
            }
            
            // Crear nuevo juego
            currentGame = new OmahaPokerGame(
                humanPlayer,
                boss,
                level.getBigBlind() / 2,
                level.getBigBlind()
            );
            
            currentGame.startNewHand();
            
            // Mostrar mano del jugador
            showPlayerHand();
            
            // Jugar las fases
            boolean handComplete = false;
            while (!handComplete && currentGame.isGameActive()) {
                // Mostrar estado del juego
                System.out.println(currentGame.getGameState());
                
                // Si ya estamos en showdown, salir del loop
                if (currentGame.getPhase() == OmahaPokerGame.GamePhase.SHOWDOWN) {
                    handComplete = true;
                    break;
                }
                
                // Ronda de apuestas usando el método correcto del juego
                boolean roundComplete = false;
                while (!roundComplete && currentGame.isGameActive()) {
                    // executeBettingRound retorna false si necesita input del jugador
                    boolean needsPlayerAction = !currentGame.executeBettingRound();
                    
                    if (needsPlayerAction) {
                        // Es turno del jugador humano
                        handlePlayerAction();
                    } else {
                        // La ronda está completa
                        roundComplete = true;
                    }
                }
                
                if (!currentGame.isGameActive()) {
                    handComplete = true;
                    break;
                }
                
                // Avanzar a siguiente fase
                currentGame.advancePhase();
                if (currentGame.getPhase() != OmahaPokerGame.GamePhase.PRE_FLOP 
                    && currentGame.getPhase() != OmahaPokerGame.GamePhase.SHOWDOWN) {
                    showPlayerHand();
                }
            }
            
            // Showdown
            Player winner = currentGame.determineWinner();
            
            // Mostrar log de acciones
            System.out.println("\n--- RESUMEN DE LA MANO ---");
            for (String action : currentGame.getActionLog()) {
                System.out.println(action);
            }
            
            // Actualizar estadísticas
            handsPlayed++;
            if (winner == humanPlayer) {
                sessionWins++;
                levelManager.recordWin();
                System.out.println("\n[IA] Aprendiendo de esta derrota...");
            } else if (winner != null) {
                sessionLosses++;
                levelManager.recordLoss();
                System.out.println("\n[IA] Reforzando esta estrategia ganadora...");
            }
            
            // Mostrar progreso de aprendizaje cada 3 manos
            if (handsPlayed % 3 == 0 && boss.getDifficulty() >= 2) {
                System.out.println("\n+===============================================+");
                System.out.println("|    PROGRESO DE APRENDIZAJE DE LA IA          |");
                System.out.println("+===============================================+");
                System.out.println("|  Decisiones totales: " + boss.getMlEngine().getTotalDecisions());
                System.out.println("|  Tasa de exploración: " + 
                                 String.format("%.1f%%", boss.getMlEngine().getExplorationRate() * 100));
                System.out.println("|  Estilo adaptado: " + boss.getAdaptiveStyle().name());
                System.out.println("|  Tu perfil detectado:");
                System.out.println("|    - Agresión: " + 
                                 String.format("%.1f%%", boss.getMlEngine().getPlayerProfile().aggressionLevel * 100));
                System.out.println("|    - Fold a Raise: " + 
                                 String.format("%.1f%%", boss.getMlEngine().getPlayerProfile().foldToRaise * 100));
                System.out.println("+===============================================+");
            }
            
            // Verificar si el nivel está completo
            if (level.isCompleted()) {
                System.out.println("\n+===============================================+");
                System.out.println("|     FELICIDADES! NIVEL COMPLETADO            |");
                System.out.println("+===============================================+");
                
                if (levelManager.hasNextLevel()) {
                    System.out.println("\nNuevo nivel desbloqueado!");
                }
                break;
            }
            
            // Verificar si el jugador se quedó sin fichas
            if (humanPlayer.getChips() <= level.getBigBlind()) {
                System.out.println("\nTe has quedado sin fichas suficientes para continuar.");
                System.out.println("Reintentando nivel...");
                humanPlayer.addChips(5000);
            }
            
            System.out.print("\nContinuar jugando? (s/n): ");
            if (!scanner.nextLine().equalsIgnoreCase("s")) {
                break;
            }
        }
        
        // Resumen de la sesión
        System.out.println("\n+===============================================+");
        System.out.println("|         RESUMEN DE LA SESIÓN                  |");
        System.out.println("+===============================================+");
        System.out.println("|  Manos jugadas: " + handsPlayed);
        System.out.println("|  Victorias: " + sessionWins);
        System.out.println("|  Derrotas: " + sessionLosses);
        System.out.println("|  Fichas finales: $" + humanPlayer.getChips());
        System.out.println("+===============================================+");
    }
    
    private static void showPlayerHand() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│  Tu mano: " + humanPlayer.getHand() + "  │");
        System.out.println("└─────────────────────────────────────┘");
    }
    
    private static boolean handlePlayerAction() {
        Player boss = currentGame.getBossPlayer();
        int callAmount = currentGame.getCurrentBet() - humanPlayer.getCurrentBet();
        
        System.out.println("\n+===============================================+");
        System.out.println("|              TU TURNO                         |");
        System.out.println("+===============================================+");
        System.out.println("| POZO TOTAL: $" + currentGame.getPot());
        System.out.println("| Apuesta actual en mesa: $" + currentGame.getCurrentBet());
        System.out.println("+===============================================+");
        System.out.println("| TU ESTADO:");
        System.out.println("| - Fichas disponibles: $" + humanPlayer.getChips());
        System.out.println("| - Tu apuesta actual: $" + humanPlayer.getCurrentBet());
        System.out.println("| - Para igualar: $" + callAmount);
        System.out.println("+===============================================+");
        System.out.println("| ESTADO DEL BOSS:");
        System.out.println("| - " + boss.getName() + ": $" + boss.getChips() + " fichas");
        System.out.println("| - Apuesta del boss: $" + boss.getCurrentBet());
        System.out.println("+===============================================+");
        System.out.println("\nAcciones disponibles:");
        
        List<String> actions = new ArrayList<>();
        if (callAmount == 0) {
            System.out.println("1. CHECK");
            System.out.println("2. RAISE (Subir)");
            actions.add("CHECK");
            actions.add("RAISE");
        } else {
            System.out.println("1. FOLD (Retirarse)");
            System.out.println("2. CALL (Igualar $" + callAmount + ")");
            System.out.println("3. RAISE (Subir)");
            actions.add("FOLD");
            actions.add("CALL");
            actions.add("RAISE");
        }
        System.out.println("4. ALL-IN");
        
        System.out.print("\nSelecciona acción: ");
        String choice = scanner.nextLine();
        
        AIStrategy.Action action = null;
        AIStrategy.GameContext context = new AIStrategy.GameContext(
            humanPlayer.getHand(),
            currentGame.getCommunityCards(),
            currentGame.getPot(),
            callAmount,
            humanPlayer.getChips(),
            0,
            2,
            0.5,
            currentGame.getBigBlind()
        );
        
        try {
            int actionNum = Integer.parseInt(choice);
            
            if (callAmount == 0) {
                switch (actionNum) {
                    case 1: action = AIStrategy.Action.CHECK; break;
                    case 2: action = AIStrategy.Action.RAISE; break;
                    case 4: action = AIStrategy.Action.ALL_IN; break;
                }
            } else {
                switch (actionNum) {
                    case 1: action = AIStrategy.Action.FOLD; break;
                    case 2: action = AIStrategy.Action.CALL; break;
                    case 3: action = AIStrategy.Action.RAISE; break;
                    case 4: action = AIStrategy.Action.ALL_IN; break;
                }
            }
            
            if (action != null) {
                currentGame.executeAction(humanPlayer, action, context);
                humanPlayer.setHasActed(true); // Marcar que el jugador ya actuó
                return true;
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }
        
        System.out.println("Acción inválida.");
        return false;
    }
    
    private static void showLevels() {
        System.out.println(levelManager.getProgressReport());
        
        System.out.print("\nDeseas ver detalles de un boss específico? (1-" + 
                        levelManager.getTotalLevels() + " o enter para volver): ");
        String input = scanner.nextLine();
        
        if (!input.isEmpty()) {
            try {
                int levelNum = Integer.parseInt(input);
                LevelManager.Level level = levelManager.getLevel(levelNum - 1);
                if (level != null) {
                    System.out.println("\n" + level.getBoss().toString());
                }
            } catch (NumberFormatException e) {
                // Ignorar
            }
        }
    }
    
    private static void showStatistics() {
        System.out.println("\n+===============================================+");
        System.out.println("|              ESTADÍSTICAS                     |");
        System.out.println("+===============================================+");
        System.out.println("|  Jugador: " + humanPlayer.getName());
        System.out.println("|  Fichas actuales: $" + humanPlayer.getChips());
        System.out.println("|");
        System.out.println("|  Victorias totales: " + levelManager.getPlayerTotalWins());
        System.out.println("|  Derrotas totales: " + levelManager.getPlayerTotalLosses());
        System.out.printf("|  Tasa de victoria: %.1f%%\n", levelManager.getWinRate() * 100);
        System.out.println("|");
        System.out.println("|  Nivel actual: " + (levelManager.getCurrentLevel().getLevelNumber()));
        
        long completedLevels = levelManager.getAllLevels().stream()
            .filter(LevelManager.Level::isCompleted)
            .count();
        System.out.println("|  Niveles completados: " + completedLevels + "/" + 
                          levelManager.getTotalLevels());
        
        if (levelManager.isGameCompleted()) {
            System.out.println("|");
            System.out.println("|  * HAS COMPLETADO TODOS LOS NIVELES! *");
        }
        
        System.out.println("+===============================================+");
    }
    
    private static void showTutorial() {
        System.out.println("\n+===========================================================+");
        System.out.println("|            TUTORIAL DE OMAHA POKER                        |");
        System.out.println("+===========================================================+");
        System.out.println("|                                                           |");
        System.out.println("|  REGLAS BÁSICAS:                                          |");
        System.out.println("|  • Cada jugador recibe 4 cartas privadas                  |");
        System.out.println("|  • Se reparten 5 cartas comunitarias (flop, turn, river)  |");
        System.out.println("|  • Debes usar EXACTAMENTE 2 de tus 4 cartas privadas      |");
        System.out.println("|  • Y EXACTAMENTE 3 de las 5 cartas comunitarias           |");
        System.out.println("|                                                           |");
        System.out.println("|  RANGOS DE MANOS (de menor a mayor):                      |");
        System.out.println("|  1. Carta Alta                                            |");
        System.out.println("|  2. Pareja                                                |");
        System.out.println("|  3. Dos Pares                                             |");
        System.out.println("|  4. Trío                                                  |");
        System.out.println("|  5. Escalera                                              |");
        System.out.println("|  6. Color                                                 |");
        System.out.println("|  7. Full House                                            |");
        System.out.println("|  8. Póker                                                 |");
        System.out.println("|  9. Escalera de Color                                     |");
        System.out.println("|  10. Escalera Real                                        |");
        System.out.println("|                                                           |");
        System.out.println("|  ACCIONES:                                                |");
        System.out.println("|  • FOLD: Retirarse de la mano                             |");
        System.out.println("|  • CHECK: Pasar sin apostar (solo si no hay apuesta)      |");
        System.out.println("|  • CALL: Igualar la apuesta actual                        |");
        System.out.println("|  • RAISE: Subir la apuesta                                |");
        System.out.println("|  • ALL-IN: Apostar todas tus fichas                       |");
        System.out.println("|                                                           |");
        System.out.println("|  CARACTERÍSTICAS DEL JUEGO:                               |");
        System.out.println("|  • Sistema de progresión por niveles                      |");
        System.out.println("|  • Bosses con diferentes estilos de juego                 |");
        System.out.println("|  • IA con aprendizaje automático adaptativo               |");
        System.out.println("|  • Los bosses aprenden de tus patrones de juego           |");
        System.out.println("|                                                           |");
        System.out.println("+===========================================================+");
        
        System.out.print("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
    
    private static void showMLReport() {
        LevelManager.Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel != null) {
            BossCharacter boss = currentLevel.getBoss();
            System.out.println(boss.getMlEngine().getLearningReport());
            
            System.out.println("\nAdaptación de Estrategia:");
            System.out.println("  Estilo Original: " + boss.getPrimaryStyle().name());
            System.out.println("  Estilo Adaptado: " + boss.getAdaptiveStyle().name());
            
            if (boss.getMlEngine().getTotalDecisions() > 10) {
                System.out.println("\n+===============================================+");
                System.out.println("|  ANÁLISIS DE APRENDIZAJE                      |");
                System.out.println("+===============================================+");
                System.out.println("|  La IA está aprendiendo activamente:");
                System.out.println("|  - Cada acción que tomas es analizada");
                System.out.println("|  - La IA ajusta su estrategia cada 3 manos");
                System.out.println("|  - El % de exploración disminuye con el tiempo");
                System.out.println("|  - Las decisiones malas tienen valor Q negativo");
                System.out.println("+===============================================+");
            }
        } else {
            System.out.println("\nNo hay datos de aprendizaje disponibles todavía.");
            System.out.println("Juega algunas manos para que la IA aprenda tus patrones.");
        }
        
        System.out.print("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
    
    private static void showCardSymbols() {
        System.out.println("\n+===============================================+");
        System.out.println("|        SÍMBOLOS DE CARTAS                     |");
        System.out.println("+===============================================+");
        System.out.println("|                                               |");
        System.out.println("|  Para mejor compatibilidad con la consola,    |");
        System.out.println("|  usamos símbolos de texto simples:            |");
        System.out.println("|                                               |");
        System.out.println("|  PALOS:                                       |");
        System.out.println("|    C = Corazones   (Hearts)                   |");
        System.out.println("|    D = Diamantes   (Diamonds)                 |");
        System.out.println("|    T = Tréboles    (Clubs)                    |");
        System.out.println("|    P = Picas       (Spades)                   |");
        System.out.println("|                                               |");
        System.out.println("|  VALORES:                                     |");
        System.out.println("|    2-10 = Números                             |");
        System.out.println("|    J = Jota (Jack)                            |");
        System.out.println("|    Q = Reina (Queen)                          |");
        System.out.println("|    K = Rey (King)                             |");
        System.out.println("|    A = As (Ace)                               |");
        System.out.println("|                                               |");
        System.out.println("|  EJEMPLOS:                                    |");
        System.out.println("|    AC = As de Corazones                       |");
        System.out.println("|    KP = Rey de Picas                          |");
        System.out.println("|    10D = Diez de Diamantes                    |");
        System.out.println("|    JT = Jota de Tréboles                      |");
        System.out.println("|                                               |");
        System.out.println("+===============================================+");
        
        System.out.print("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
    
    // ==================== FUNCIONES DE PERSISTENCIA Y PRE-ENTRENAMIENTO ====================
    
    /**
     * Pregunta al usuario si desea pre-entrenar la IA
     */
    private static void askForPreTraining() {
        System.out.println("\n+===============================================+");
        System.out.println("|     CONFIGURACIÓN DE INTELIGENCIA ARTIFICIAL  |");
        System.out.println("+===============================================+");
        
        // Intentar cargar datos previos
        boolean hasData = loadAllBossesLearning();
        
        if (!hasData) {
            System.out.println("\nQuieres que la IA se entrene antes de jugar?");
            System.out.println("(Esto hará que la IA juegue miles de manos simuladas)");
            System.out.println();
            System.out.println("  1. Sí - Entrenamiento RÁPIDO   (500 manos ~10 seg)");
            System.out.println("  2. Sí - Entrenamiento NORMAL   (2000 manos ~30 seg)");
            System.out.println("  3. Sí - Entrenamiento INTENSO  (5000 manos ~60 seg)");
            System.out.println("  4. No - Empezar desde cero");
            System.out.print("\nOpción: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    preTrainAllBosses(500);
                    break;
                case "2":
                    preTrainAllBosses(2000);
                    break;
                case "3":
                    preTrainAllBosses(5000);
                    break;
                case "4":
                    System.out.println("\n[i] La IA aprenderá desde cero mientras juegas.");
                    break;
                default:
                    System.out.println("\n[i] Opción inválida. La IA empezará desde cero.");
            }
        }
    }
    
    /**
     * Pre-entrena todos los bosses
     */
    private static void preTrainAllBosses(int handsPerBoss) {
        System.out.println("\n+================================================+");
        System.out.println("|   ENTRENANDO INTELIGENCIA ARTIFICIAL...        |");
        System.out.println("+================================================+");
        
        for (int i = 1; i <= levelManager.getTotalLevels(); i++) {
            Level level = levelManager.getAllLevels().get(i - 1);
            BossCharacter boss = level.getBoss();
            
            System.out.println("\n[" + i + "/" + levelManager.getTotalLevels() + "] " + 
                             boss.getName() + " (" + boss.getPrimaryStyle().name() + ")");
            boss.getMlEngine().preTrain(handsPerBoss);
        }
        
        System.out.println("\n\n+================================================+");
        System.out.println("|   OK ENTRENAMIENTO COMPLETADO                  |");
        System.out.println("+================================================+");
        System.out.println("\n[!] La IA está lista para desafiarte con estrategias avanzadas.");
        
        // Guardar automáticamente
        saveAllBossesLearning();
        
        System.out.print("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
    
    // ==================== GESTIÓN DE PERFILES ====================
    
    /**
     * Muestra menú de selección/creación de perfiles
     */
    private static boolean selectOrCreateProfile() {
        List<ProfileManager.Profile> profiles = profileManager.loadProfiles();
        
        System.out.println("\n+===============================================+");
        System.out.println("|           SELECCIÓN DE PERFIL                |");
        System.out.println("+===============================================+\n");
        
        if (profiles.isEmpty()) {
            System.out.println("No hay perfiles. Vamos a crear uno.");
            return createNewProfile();
        }
        
        System.out.println("Perfiles disponibles:\n");
        for (int i = 0; i < profiles.size(); i++) {
            System.out.println((i + 1) + ". " + profiles.get(i).toString());
        }
        
        if (profiles.size() < 3) {
            System.out.println((profiles.size() + 1) + ". [Crear Nuevo Perfil]");
        }
        
        System.out.print("\nSelecciona perfil: ");
        String choice = scanner.nextLine();
        
        try {
            int num = Integer.parseInt(choice);
            
            if (num > 0 && num <= profiles.size()) {
                currentProfile = profiles.get(num - 1);
                currentProfile.lastPlayed = System.currentTimeMillis();
                System.out.println("\n[OK] Perfil cargado: " + currentProfile.name);
                return true;
            } else if (num == profiles.size() + 1 && profiles.size() < 3) {
                return createNewProfile();
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }
        
        System.out.println("Opción inválida.");
        return selectOrCreateProfile();
    }
    
    /**
     * Crea un nuevo perfil
     */
    private static boolean createNewProfile() {
        System.out.print("\nNombre del perfil (máx 15 caracteres): ");
        String name = scanner.nextLine().trim();
        
        if (name.isEmpty() || name.length() > 15) {
            System.out.println("Nombre inválido.");
            return createNewProfile();
        }
        
        currentProfile = profileManager.createProfile(name);
        
        if (currentProfile == null) {
            System.out.println("No se pudo crear el perfil.");
            return false;
        }
        
        System.out.println("\n[OK] Perfil creado: " + name);
        return true;
    }
    
    /**
     * Menú de gestión de perfiles
     */
    private static void showProfilesMenu() {
        List<ProfileManager.Profile> profiles = profileManager.loadProfiles();
        
        System.out.println("\n+===============================================+");
        System.out.println("|           GESTIÓN DE PERFILES                |");
        System.out.println("+===============================================+\n");
        
        System.out.println("Perfil actual: " + currentProfile.name + "\n");
        System.out.println("Todos los perfiles:\n");
        
        for (int i = 0; i < profiles.size(); i++) {
            String marker = profiles.get(i).name.equals(currentProfile.name) ? " [ACTUAL]" : "";
            System.out.println((i + 1) + ". " + profiles.get(i).toString() + marker);
        }
        
        System.out.println("\nOpciones:");
        System.out.println("1. Cambiar de perfil");
        System.out.println("2. Borrar un perfil");
        System.out.println("3. Volver");
        System.out.print("\nSelecciona: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                changeProfile();
                break;
            case "2":
                deleteProfile();
                break;
            case "3":
                return;
        }
    }
    
    /**
     * Cambia al perfil seleccionado
     */
    private static void changeProfile() {
        saveAllBossesLearning();
        saveProfileProgress();
        
        System.out.println("\nGuardando progreso actual...");
        
        if (selectOrCreateProfile()) {
            // Reiniciar jugador y cargar progreso
            humanPlayer = new Player(currentProfile.name, currentProfile.totalChips, Player.PlayerType.HUMAN);
            levelManager = new LevelManager();
            loadProfileProgress();
            
            System.out.println("[OK] Perfil cambiado exitosamente.");
        }
    }
    
    /**
     * Borra un perfil (no el actual)
     */
    private static void deleteProfile() {
        List<ProfileManager.Profile> profiles = profileManager.loadProfiles();
        
        System.out.println("\nQué perfil deseas borrar?");
        
        for (int i = 0; i < profiles.size(); i++) {
            ProfileManager.Profile p = profiles.get(i);
            if (!p.name.equals(currentProfile.name)) {
                System.out.println((i + 1) + ". " + p.name);
            }
        }
        
        System.out.print("\nNúmero (0 para cancelar): ");
        String choice = scanner.nextLine();
        
        try {
            int num = Integer.parseInt(choice);
            if (num > 0 && num <= profiles.size()) {
                ProfileManager.Profile toDelete = profiles.get(num - 1);
                
                if (toDelete.name.equals(currentProfile.name)) {
                    System.out.println("[X] No puedes borrar el perfil actual.");
                    return;
                }
                
                System.out.print("Seguro? (S/N): ");
                String confirm = scanner.nextLine();
                
                if (confirm.equalsIgnoreCase("S")) {
                    profileManager.deleteProfile(toDelete.name);
                    System.out.println("[OK] Perfil borrado.");
                }
            }
        } catch (NumberFormatException e) {
            // Ignorar
        }
    }
    
    /**
     * Guarda el progreso del perfil actual
     */
    private static void saveProfileProgress() {
        currentProfile.totalChips = humanPlayer.getChips();
        currentProfile.totalWins = levelManager.getPlayerTotalWins();
        currentProfile.totalLosses = levelManager.getPlayerTotalLosses();
        currentProfile.currentLevel = levelManager.getCurrentLevelNumber() + 1;
        currentProfile.lastPlayed = System.currentTimeMillis();
        
        profileManager.saveProfile(currentProfile);
    }
    
    /**
     * Carga el progreso del perfil actual
     */
    private static void loadProfileProgress() {
        // Desbloquear y completar niveles hasta el nivel actual del perfil
        int targetLevel = Math.min(currentProfile.currentLevel, levelManager.getTotalLevels());
        
        for (int i = 1; i <= targetLevel; i++) {
            Level level = levelManager.getLevel(i - 1);
            if (level != null) {
                level.unlock();
                
                // Marcar como completado solo los niveles anteriores al actual
                if (i < targetLevel) {
                    level.complete();
                }
            }
        }
        
        // Seleccionar el nivel actual del perfil
        levelManager.setCurrentLevel(targetLevel - 1);
        
        // Restaurar estadísticas directamente
        levelManager.setPlayerTotalWins(currentProfile.totalWins);
        levelManager.setPlayerTotalLosses(currentProfile.totalLosses);
    }
    
    /**
     * Guarda el aprendizaje de todos los bosses usando nombres específicos del perfil
     */
    private static void saveAllBossesLearning() {
        System.out.println("\n[...] Guardando progreso de la IA...");
        
        for (int i = 1; i <= levelManager.getTotalLevels(); i++) {
            Level level = levelManager.getAllLevels().get(i - 1);
            BossCharacter boss = level.getBoss();
            String filename = ProfileManager.getBossDataFilename(
                currentProfile.name, i, boss.getName()
            );
            boss.getMlEngine().saveToFile(filename);
        }
        
        System.out.println("[OK] Progreso guardado exitosamente.");
    }
    
    /**
     * Carga el aprendizaje de todos los bosses usando nombres específicos del perfil
     */
    private static boolean loadAllBossesLearning() {
        boolean anyDataLoaded = false;
        
        System.out.println("\n[...] Buscando datos previos de la IA para " + currentProfile.name + "...");
        
        for (int i = 1; i <= levelManager.getTotalLevels(); i++) {
            Level level = levelManager.getAllLevels().get(i - 1);
            BossCharacter boss = level.getBoss();
            String filename = ProfileManager.getBossDataFilename(
                currentProfile.name, i, boss.getName()
            );
            
            java.io.File file = new java.io.File(filename);
            if (file.exists()) {
                boss.getMlEngine().loadFromFile(filename);
                anyDataLoaded = true;
            }
        }
        
        if (anyDataLoaded) {
            System.out.println("\n[OK] Se cargaron datos previos de la IA.");
            System.out.println("[!] La IA continuará aprendiendo de donde quedó.\n");
        } else {
            System.out.println("[i] No se encontraron datos previos.");
        }
        
        return anyDataLoaded;
    }
    
    /**
     * Muestra las estadísticas del torneo usando PriorityQueue
     */
    private static void showTournamentStats() {
        if (currentGame == null) {
            System.out.println("\n[!] No hay estadísticas disponibles. Juega algunas manos primero.");
            return;
        }
        
        TournamentStats stats = currentGame.getTournamentStats();
        System.out.println(stats.generateReport());
        
        System.out.print("\nPresiona ENTER para continuar...");
        scanner.nextLine();
    }
}

