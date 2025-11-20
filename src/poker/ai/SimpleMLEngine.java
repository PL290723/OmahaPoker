package poker.ai;

import java.util.*;
import java.io.*;

/**
 * Motor de aprendizaje automático ligero para que los bosses adapten su estrategia
 * basándose en el historial de partidas y patrones del jugador
 */
public class SimpleMLEngine {
    
    /**
     * Representa una experiencia de juego almacenada
     */
    public static class Experience {
        public String situation;      // Descripción de la situación
        public String actionTaken;    // Acción que se tomó
        public double reward;         // Recompensa obtenida (-1 a 1)
        public int frequency;         // Cuántas veces se ha visto esta situación
        
        public Experience(String situation, String actionTaken, double reward) {
            this.situation = situation;
            this.actionTaken = actionTaken;
            this.reward = reward;
            this.frequency = 1;
        }
    }
    
    /**
     * Perfil del jugador humano basado en observaciones
     */
    public static class PlayerProfile {
        public double aggressionLevel;    // 0-1: Nivel de agresión
        public double bluffFrequency;     // 0-1: Frecuencia de faroleos detectados
        public double foldToRaise;        // 0-1: Tendencia a retirarse ante subidas
        public double averageBetSize;     // Tamaño promedio de apuestas
        public int handsPlayed;           // Total de manos jugadas
        
        public Map<String, Integer> actionCounts; // Contador de acciones
        
        public PlayerProfile() {
            this.aggressionLevel = 0.5;
            this.bluffFrequency = 0.3;
            this.foldToRaise = 0.5;
            this.averageBetSize = 0.0;
            this.handsPlayed = 0;
            this.actionCounts = new HashMap<>();
            
            actionCounts.put("FOLD", 0);
            actionCounts.put("CHECK", 0);
            actionCounts.put("CALL", 0);
            actionCounts.put("RAISE", 0);
            actionCounts.put("ALL_IN", 0);
        }
        
        public void recordAction(String action, int betSize, boolean facingRaise) {
            handsPlayed++;
            actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
            
            if (betSize > 0) {
                averageBetSize = (averageBetSize * (handsPlayed - 1) + betSize) / handsPlayed;
            }
            
            // Actualizar nivel de agresión
            if (action.equals("RAISE") || action.equals("ALL_IN")) {
                aggressionLevel = (aggressionLevel * 0.9) + (0.1 * 1.0);
            } else if (action.equals("FOLD")) {
                aggressionLevel = (aggressionLevel * 0.9) + (0.1 * 0.0);
            }
            
            // Actualizar tendencia a fold ante raises
            if (facingRaise && action.equals("FOLD")) {
                foldToRaise = (foldToRaise * 0.9) + (0.1 * 1.0);
            } else if (facingRaise && !action.equals("FOLD")) {
                foldToRaise = (foldToRaise * 0.9) + (0.1 * 0.0);
            }
        }
        
        @Override
        public String toString() {
            return String.format("Perfil: Agresión=%.2f, Bluff=%.2f, Fold a Raise=%.2f, Manos=%d",
                               aggressionLevel, bluffFrequency, foldToRaise, handsPlayed);
        }
    }
    
    private List<Experience> experienceMemory;
    private PlayerProfile playerProfile;
    private Map<String, Double> qTable; // Q-Learning simplificado
    private double learningRate;
    private double discountFactor; // Para futuras recompensas
    private double explorationRate; // Para epsilon-greedy dinámico
    private Random random;
    private int totalDecisions; // Contador para reducir exploración con el tiempo
    
    public SimpleMLEngine() {
        this.experienceMemory = new LinkedList<>();
        this.playerProfile = new PlayerProfile();
        this.qTable = new HashMap<>();
        this.learningRate = 0.15; // Aumentado para aprender más rápido
        this.discountFactor = 0.9; // Factor de descuento para recompensas futuras
        this.explorationRate = 0.3; // Comienza explorando más
        this.random = new Random();
        this.totalDecisions = 0;
    }
    
    /**
     * Registra una experiencia de juego
     */
    public void recordExperience(String situation, String action, double reward) {
        // Buscar si ya existe esta combinación
        boolean found = false;
        for (Experience exp : experienceMemory) {
            if (exp.situation.equals(situation) && exp.actionTaken.equals(action)) {
                exp.frequency++;
                exp.reward = (exp.reward * (exp.frequency - 1) + reward) / exp.frequency;
                found = true;
                break;
            }
        }
        
        if (!found) {
            Experience newExp = new Experience(situation, action, reward);
            experienceMemory.add(newExp);
            
            // Limitar tamaño de memoria (mantener las 1000 más recientes)
            if (experienceMemory.size() > 1000) {
                experienceMemory.remove(0);
            }
        }
        
        // Actualizar Q-Table
        updateQValue(situation, action, reward);
    }
    
    /**
     * Actualiza el valor Q para una situación-acción (Q-Learning mejorado)
     */
    private void updateQValue(String state, String action, double reward) {
        String key = state + "|" + action;
        double oldQ = qTable.getOrDefault(key, 0.0);
        
        // Q(s,a) = Q(s,a) + α[r - Q(s,a)]
        // Con decay del learning rate para convergencia
        double effectiveLearningRate = learningRate / (1 + totalDecisions / 100.0);
        double newQ = oldQ + effectiveLearningRate * (reward - oldQ);
        qTable.put(key, newQ);
        
        totalDecisions++;
        
        // Reducir exploración con el tiempo (de 0.3 a 0.05)
        explorationRate = Math.max(0.05, 0.3 - (totalDecisions / 500.0));
    }
    
    /**
     * Recomienda la mejor acción basada en experiencias pasadas (mejorado)
     */
    public String recommendAction(String situation, List<String> possibleActions) {
        Map<String, Double> actionScores = new HashMap<>();
        
        for (String action : possibleActions) {
            String key = situation + "|" + action;
            double qValue = qTable.getOrDefault(key, 0.0);
            
            // Combinar Q-value con experiencias similares
            double experienceBonus = getExperienceBonus(situation, action);
            actionScores.put(action, qValue + experienceBonus * 0.5);
        }
        
        // Epsilon-greedy dinámico: exploración disminuye con el tiempo
        if (random.nextDouble() < explorationRate) {
            return possibleActions.get(random.nextInt(possibleActions.size()));
        }
        
        // Seleccionar la mejor acción
        return actionScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(possibleActions.get(0));
    }
    
    /**
     * Obtiene un bonus basado en experiencias similares
     */
    private double getExperienceBonus(String situation, String action) {
        double bonus = 0.0;
        int count = 0;
        
        for (Experience exp : experienceMemory) {
            if (exp.situation.contains(situation.substring(0, Math.min(10, situation.length()))) 
                && exp.actionTaken.equals(action)) {
                bonus += exp.reward * (exp.frequency / 100.0);
                count++;
            }
        }
        
        return count > 0 ? bonus / count : 0.0;
    }
    
    /**
     * Adapta el estilo de juego basado en el perfil del oponente
     */
    public AIStrategy.PlayStyle adaptStyleToOpponent(AIStrategy.PlayStyle currentStyle) {
        // Contra jugadores agresivos, jugar más conservador
        if (playerProfile.aggressionLevel > 0.7) {
            return AIStrategy.PlayStyle.CALCULADOR;
        }
        
        // Contra jugadores pasivos, jugar más agresivo
        if (playerProfile.aggressionLevel < 0.3) {
            return AIStrategy.PlayStyle.AGRESIVO;
        }
        
        // Si el jugador hace mucho fold a raises, aumentar agresión
        if (playerProfile.foldToRaise > 0.6) {
            return AIStrategy.PlayStyle.AGRESIVO;
        }
        
        // Si el jugador casi nunca hace fold, jugar más tight
        if (playerProfile.foldToRaise < 0.2) {
            return AIStrategy.PlayStyle.CONSERVADOR;
        }
        
        return currentStyle; // Mantener estilo actual
    }
    
    /**
     * Predice la probabilidad de que el jugador haga fold
     */
    public double predictFoldProbability(int betSize) {
        if (playerProfile.handsPlayed < 5) {
            return 0.5; // Sin suficiente información
        }
        
        // Basado en el tamaño de la apuesta relativo al promedio
        double sizeRatio = playerProfile.averageBetSize > 0 
            ? betSize / playerProfile.averageBetSize 
            : 1.0;
        
        double baseProbability = playerProfile.foldToRaise;
        
        // Apuestas grandes aumentan la probabilidad de fold
        if (sizeRatio > 2.0) {
            baseProbability += 0.2;
        } else if (sizeRatio > 1.5) {
            baseProbability += 0.1;
        }
        
        return Math.min(0.95, baseProbability);
    }
    
    /**
     * Genera un resumen de lo aprendido (mejorado)
     */
    public String getLearningReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n===============================================\n");
        report.append("   REPORTE DE APRENDIZAJE AUTOMÁTICO\n");
        report.append("===============================================\n\n");
        
        report.append("Perfil del Jugador:\n");
        report.append("  ").append(playerProfile.toString()).append("\n\n");
        
        report.append("Acciones Registradas:\n");
        for (Map.Entry<String, Integer> entry : playerProfile.actionCounts.entrySet()) {
            int count = entry.getValue();
            double percentage = playerProfile.handsPlayed > 0 
                ? (count * 100.0) / playerProfile.handsPlayed 
                : 0;
            report.append(String.format("  %s: %d veces (%.1f%%)\n", 
                                       entry.getKey(), count, percentage));
        }
        
        report.append("\nEstadísticas de Aprendizaje:\n");
        report.append("  Experiencias en Memoria: ").append(experienceMemory.size()).append("\n");
        report.append("  Entradas en Q-Table: ").append(qTable.size()).append("\n");
        report.append("  Total de Decisiones: ").append(totalDecisions).append("\n");
        report.append("  Tasa de Exploración Actual: ").append(String.format("%.1f%%", explorationRate * 100)).append("\n");
        report.append("  Learning Rate Efectivo: ").append(String.format("%.3f", learningRate / (1 + totalDecisions / 100.0))).append("\n");
        
        // Mostrar las mejores estrategias aprendidas
        report.append("\nMejores Estrategias Aprendidas:\n");
        qTable.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(8)
            .forEach(entry -> {
                String[] parts = entry.getKey().split("\\|");
                if (parts.length == 2) {
                    report.append(String.format("  %s → %s (Q: %.3f)\n", 
                                              parts[0], parts[1], entry.getValue()));
                }
            });
        
        // Mostrar las peores estrategias (las que evita)
        report.append("\nEstrategias Evitadas (Aprendidas como malas):\n");
        qTable.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(5)
            .forEach(entry -> {
                String[] parts = entry.getKey().split("\\|");
                if (parts.length == 2 && entry.getValue() < 0) {
                    report.append(String.format("  %s → %s (Q: %.3f)\n", 
                                              parts[0], parts[1], entry.getValue()));
                }
            });
        
        report.append("\n===============================================\n");
        return report.toString();
    }
    
    /**
     * Codifica la situación del juego en una cadena para aprendizaje
     */
    public static String encodeSituation(String phase, double handStrength, 
                                        int potSize, int playerChips) {
        String strength = handStrength > 0.7 ? "FUERTE" : 
                         handStrength > 0.4 ? "MEDIA" : "DEBIL";
        String pot = potSize > playerChips * 0.5 ? "GRANDE" : "PEQUENO";
        
        return phase + "_" + strength + "_" + pot;
    }
    
    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }
    
    public void recordPlayerAction(String action, int betSize, boolean facingRaise) {
        playerProfile.recordAction(action, betSize, facingRaise);
    }
    
    public int getTotalExperiences() {
        return experienceMemory.size();
    }
    
    public void reset() {
        experienceMemory.clear();
        playerProfile = new PlayerProfile();
        qTable.clear();
        totalDecisions = 0;
        explorationRate = 0.3;
    }
    
    public double getExplorationRate() {
        return explorationRate;
    }
    
    public int getTotalDecisions() {
        return totalDecisions;
    }
    
    // ==================== PERSISTENCIA ====================
    
    /**
     * Guarda el aprendizaje de la IA en un archivo
     */
    public void saveToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Guardar metadata
            writer.println("# SIMPLE ML ENGINE DATA v1.0");
            writer.println("TOTAL_DECISIONS=" + totalDecisions);
            writer.println("EXPLORATION_RATE=" + explorationRate);
            writer.println("LEARNING_RATE=" + learningRate);
            writer.println("DISCOUNT_FACTOR=" + discountFactor);
            
            // Guardar Q-Table
            writer.println("\n# Q-TABLE");
            for (Map.Entry<String, Double> entry : qTable.entrySet()) {
                writer.println("Q|" + entry.getKey() + "|" + entry.getValue());
            }
            
            // Guardar experiencias (últimas 1000)
            writer.println("\n# EXPERIENCES");
            for (Experience exp : experienceMemory) {
                writer.println("E|" + exp.situation + "|" + exp.actionTaken + "|" + 
                             exp.reward + "|" + exp.frequency);
            }
            
            // Guardar perfil del jugador
            writer.println("\n# PLAYER_PROFILE");
            writer.println("P|AGGRESSION|" + playerProfile.aggressionLevel);
            writer.println("P|BLUFF_FREQ|" + playerProfile.bluffFrequency);
            writer.println("P|FOLD_TO_RAISE|" + playerProfile.foldToRaise);
            writer.println("P|AVG_BET|" + playerProfile.averageBetSize);
            writer.println("P|HANDS_PLAYED|" + playerProfile.handsPlayed);
            
            for (Map.Entry<String, Integer> entry : playerProfile.actionCounts.entrySet()) {
                writer.println("P|ACTION|" + entry.getKey() + "|" + entry.getValue());
            }
            
            System.out.println("[OK] Datos guardados en: " + filename);
        } catch (IOException e) {
            System.err.println("[X] Error al guardar datos: " + e.getMessage());
        }
    }
    
    /**
     * Carga el aprendizaje de la IA desde un archivo
     */
    public void loadFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("[i] No hay datos previos. Empezando desde cero.");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int qLoaded = 0, expLoaded = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                // Metadata
                if (line.startsWith("TOTAL_DECISIONS=")) {
                    totalDecisions = Integer.parseInt(line.split("=")[1]);
                } else if (line.startsWith("EXPLORATION_RATE=")) {
                    explorationRate = Double.parseDouble(line.split("=")[1]);
                } else if (line.startsWith("LEARNING_RATE=")) {
                    learningRate = Double.parseDouble(line.split("=")[1]);
                } else if (line.startsWith("DISCOUNT_FACTOR=")) {
                    discountFactor = Double.parseDouble(line.split("=")[1]);
                }
                
                // Q-Table
                else if (line.startsWith("Q|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 4) {
                        // Formato: Q|STATE|ACTION|VALUE
                        String key = parts[1] + "|" + parts[2];
                        double value = Double.parseDouble(parts[3]);
                        qTable.put(key, value);
                        qLoaded++;
                    }
                }
                
                // Experiencias
                else if (line.startsWith("E|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5) {
                        Experience exp = new Experience(parts[1], parts[2], 
                                                       Double.parseDouble(parts[3]));
                        exp.frequency = Integer.parseInt(parts[4]);
                        experienceMemory.add(exp);
                        if (experienceMemory.size() > 1000) {
                            experienceMemory.remove(0);
                        }
                        expLoaded++;
                    }
                }
                
                // Perfil del jugador
                else if (line.startsWith("P|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        switch (parts[1]) {
                            case "AGGRESSION":
                                playerProfile.aggressionLevel = Double.parseDouble(parts[2]);
                                break;
                            case "BLUFF_FREQ":
                                playerProfile.bluffFrequency = Double.parseDouble(parts[2]);
                                break;
                            case "FOLD_TO_RAISE":
                                playerProfile.foldToRaise = Double.parseDouble(parts[2]);
                                break;
                            case "AVG_BET":
                                playerProfile.averageBetSize = Double.parseDouble(parts[2]);
                                break;
                            case "HANDS_PLAYED":
                                playerProfile.handsPlayed = Integer.parseInt(parts[2]);
                                break;
                            case "ACTION":
                                if (parts.length >= 4) {
                                    playerProfile.actionCounts.put(parts[2], 
                                                                  Integer.parseInt(parts[3]));
                                }
                                break;
                        }
                    }
                }
            }
            
            System.out.println("[OK] Datos cargados: " + qLoaded + " estrategias, " + 
                             expLoaded + " experiencias");
            System.out.println("[i] Decisiones previas: " + totalDecisions + 
                             " | Exploración: " + String.format("%.1f%%", explorationRate * 100));
        } catch (IOException e) {
            System.err.println("[X] Error al cargar datos: " + e.getMessage());
        }
    }
    
    // ==================== PRE-ENTRENAMIENTO ====================
    
    /**
     * Entrena la IA mediante auto-juego simulado (sin GUI)
     * @param numHands Número de manos a simular
     */
    public void preTrain(int numHands) {
        System.out.println("\n+================================================+");
        System.out.println("|     PRE-ENTRENAMIENTO DE IA EN PROGRESO       |");
        System.out.println("+================================================+");
        System.out.println("Simulando " + numHands + " manos...");
        
        Random rand = new Random();
        int progressInterval = Math.max(1, numHands / 20); // Mostrar 20 actualizaciones
        
        for (int hand = 0; hand < numHands; hand++) {
            // Simular diferentes fases del juego
            String[] phases = {"PREFLOP", "FLOP", "TURN", "RIVER"};
            
            for (String phase : phases) {
                // Generar situación aleatoria
                double handStrength = rand.nextDouble();
                int potSize = 50 + rand.nextInt(950);
                int chips = 1000 + rand.nextInt(4000);
                
                String situation = encodeSituation(phase, handStrength, potSize, chips);
                
                // IA toma decisión
                String[] possibleActionsArray = {"FOLD", "CHECK", "CALL", "RAISE_SMALL", "RAISE_BIG", "ALL_IN"};
                List<String> possibleActions = Arrays.asList(possibleActionsArray);
                String action = recommendAction(situation, possibleActions);
                
                // Simular resultado basado en heurística
                double reward = calculateTrainingReward(handStrength, action, phase);
                
                // Aprender
                recordExperience(situation, action, reward);
                
                // Simular acción del oponente (varía el perfil)
                if (hand % 10 < 3) {
                    // Oponente agresivo
                    recordPlayerAction("RAISE", 100, false);
                } else if (hand % 10 < 7) {
                    // Oponente balanceado
                    String[] balancedActions = {"CALL", "CHECK", "RAISE", "FOLD"};
                    recordPlayerAction(balancedActions[rand.nextInt(4)], 50, rand.nextBoolean());
                } else {
                    // Oponente conservador
                    String[] conservativeActions = {"FOLD", "CHECK", "CALL"};
                    recordPlayerAction(conservativeActions[rand.nextInt(3)], 20, false);
                }
            }
            
            // Mostrar progreso
            if ((hand + 1) % progressInterval == 0) {
                int percent = (hand + 1) * 100 / numHands;
                System.out.print("\r[");
                int bars = percent / 5;
                for (int i = 0; i < 20; i++) {
                    System.out.print(i < bars ? "█" : "░");
                }
                System.out.print("] " + percent + "% - Decisiones: " + totalDecisions + 
                               " | Exploración: " + String.format("%.1f%%", explorationRate * 100));
            }
        }
        
        System.out.println("\n\n[OK] Pre-entrenamiento completado!");
        System.out.println("    Total de decisiones aprendidas: " + totalDecisions);
        System.out.println("    Estrategias en Q-Table: " + qTable.size());
        System.out.println("    Tasa de exploración final: " + String.format("%.1f%%", explorationRate * 100));
    }
    
    /**
     * Calcula recompensa simulada basada en heurísticas
     */
    private double calculateTrainingReward(double handStrength, String action, String phase) {
        double reward = 0.0;
        
        // Recompensas por jugar según la fuerza de la mano
        if (action.equals("FOLD")) {
            reward = handStrength < 0.3 ? 0.3 : -0.5; // Bien si mano débil
        } else if (action.contains("RAISE")) {
            reward = handStrength > 0.6 ? 0.6 : -0.4; // Bien si mano fuerte
        } else if (action.equals("CALL")) {
            reward = handStrength > 0.4 && handStrength < 0.7 ? 0.4 : -0.2;
        } else if (action.equals("CHECK")) {
            reward = 0.1; // Neutral
        }
        
        // Ajuste por fase (early vs late)
        if (phase.equals("PREFLOP")) {
            reward *= 0.8; // Menos certeza en preflop
        } else if (phase.equals("RIVER")) {
            reward *= 1.2; // Más certeza en river
        }
        
        return reward;
    }
}
