package poker.game;

import poker.ai.AIStrategy;
import java.util.*;

/**
 * Gestiona la progresión de niveles y los jefes del juego
 */
public class LevelManager {
    private List<Level> levels;
    private int currentLevel;
    private int playerTotalWins;
    private int playerTotalLosses;
    
    public static class Level {
        private int levelNumber;
        private String name;
        private BossCharacter boss;
        private int requiredWins; // Victorias necesarias para avanzar
        private int bigBlind;
        private boolean unlocked;
        private boolean completed;
        
        public Level(int levelNumber, String name, BossCharacter boss, 
                    int requiredWins, int bigBlind) {
            this.levelNumber = levelNumber;
            this.name = name;
            this.boss = boss;
            this.requiredWins = requiredWins;
            this.bigBlind = bigBlind;
            this.unlocked = (levelNumber == 1);
            this.completed = false;
        }
        
        public int getLevelNumber() { return levelNumber; }
        public String getName() { return name; }
        public BossCharacter getBoss() { return boss; }
        public int getRequiredWins() { return requiredWins; }
        public int getBigBlind() { return bigBlind; }
        public boolean isUnlocked() { return unlocked; }
        public boolean isCompleted() { return completed; }
        
        public void unlock() { this.unlocked = true; }
        public void complete() { this.completed = true; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        @Override
        public String toString() {
            String status = completed ? "[OK COMPLETADO]" : unlocked ? "[→ DISPONIBLE]" : "[X BLOQUEADO]";
            return String.format("Nivel %d: %s %s\n  Boss: %s (Dificultad: *%d)\n  Ciegas: %d/%d",
                               levelNumber, name, status, boss.getName(), 
                               boss.getDifficulty(), bigBlind/2, bigBlind);
        }
    }
    
    public LevelManager() {
        this.levels = new ArrayList<>();
        this.currentLevel = 0;
        this.playerTotalWins = 0;
        this.playerTotalLosses = 0;
        initializeLevels();
    }
    
    private void initializeLevels() {
        // Nivel 1: Principiante
        BossCharacter rookie = new BossCharacter(
            "El Novato",
            "Un jugador inexperto que apenas conoce las reglas",
            AIStrategy.PlayStyle.CONSERVADOR,
            1,
            1000,
            "Suerte del Principiante: Puede ganar con manos débiles ocasionalmente"
        );
        levels.add(new Level(1, "Sala de Novatos", rookie, 2, 20));
        
        // Nivel 2: Jugador Cauteloso
        BossCharacter cautious = new BossCharacter(
            "La Calculadora",
            "Juega basándose en matemáticas y probabilidades",
            AIStrategy.PlayStyle.CALCULADOR,
            2,
            1500,
            "Análisis Profundo: Calcula con precisión las probabilidades del pozo"
        );
        levels.add(new Level(2, "Casino Provincial", cautious, 3, 40));
        
        // Nivel 3: Agresivo
        BossCharacter aggro = new BossCharacter(
            "El Tiburón",
            "Jugador extremadamente agresivo que presiona constantemente",
            AIStrategy.PlayStyle.AGRESIVO,
            3,
            2000,
            "Presión Constante: Aumenta la agresión a medida que avanza el juego"
        );
        levels.add(new Level(3, "Club de Póker Underground", aggro, 3, 60));
        
        // Nivel 4: Equilibrado Avanzado
        BossCharacter balanced = new BossCharacter(
            "El Maestro Zen",
            "Jugador perfectamente equilibrado que se adapta a cualquier situación",
            AIStrategy.PlayStyle.EQUILIBRADO,
            4,
            2500,
            "Armonía Perfecta: Adapta su estilo al del oponente en tiempo real"
        );
        levels.add(new Level(4, "Torneo Regional", balanced, 4, 100));
        
        // Nivel 5: Impredecible
        BossCharacter maniac = new BossCharacter(
            "El Loco",
            "Impredecible y peligroso, sus movimientos no siguen lógica aparente",
            AIStrategy.PlayStyle.MANIACO,
            4,
            3000,
            "Caos Total: Sus acciones aleatorias confunden a los oponentes"
        );
        levels.add(new Level(5, "Mesa VIP Internacional", maniac, 4, 150));
        
        // Nivel 6: Campeón Final
        BossCharacter champion = new BossCharacter(
            "El Campeón Mundial",
            "El mejor jugador del mundo, domina todas las técnicas y estrategias",
            AIStrategy.PlayStyle.CALCULADOR,
            5,
            5000,
            "Maestría Total: Combina todos los estilos y aprende rápidamente"
        );
        levels.add(new Level(6, "Final Mundial de Omaha", champion, 5, 200));
    }
    
    public List<Level> getAllLevels() {
        return new ArrayList<>(levels);
    }
    
    public Level getCurrentLevel() {
        if (currentLevel >= 0 && currentLevel < levels.size()) {
            return levels.get(currentLevel);
        }
        return null;
    }
    
    public Level getLevel(int index) {
        if (index >= 0 && index < levels.size()) {
            return levels.get(index);
        }
        return null;
    }
    
    public boolean selectLevel(int levelNumber) {
        if (levelNumber < 1 || levelNumber > levels.size()) {
            return false;
        }
        
        Level level = levels.get(levelNumber - 1);
        if (!level.isUnlocked()) {
            return false;
        }
        
        currentLevel = levelNumber - 1;
        return true;
    }
    
    public void recordWin() {
        playerTotalWins++;
        Level level = getCurrentLevel();
        
        if (level != null && !level.isCompleted()) {
            // Verificar si completó el nivel
            if (playerTotalWins - getWinsAtLevelStart() >= level.getRequiredWins()) {
                completeCurrentLevel();
            }
        }
    }
    
    public void recordLoss() {
        playerTotalLosses++;
    }
    
    private int getWinsAtLevelStart() {
        int winsNeeded = 0;
        for (int i = 0; i < currentLevel; i++) {
            winsNeeded += levels.get(i).getRequiredWins();
        }
        return winsNeeded;
    }
    
    public void completeCurrentLevel() {
        Level level = getCurrentLevel();
        if (level != null) {
            level.complete();
            
            // Desbloquear siguiente nivel
            if (currentLevel + 1 < levels.size()) {
                levels.get(currentLevel + 1).unlock();
            }
        }
    }
    
    public boolean hasNextLevel() {
        return currentLevel + 1 < levels.size();
    }
    
    public boolean advanceToNextLevel() {
        if (hasNextLevel() && getCurrentLevel().isCompleted()) {
            currentLevel++;
            return true;
        }
        return false;
    }
    
    public int getTotalLevels() {
        return levels.size();
    }
    
    public int getCurrentLevelNumber() {
        return currentLevel;
    }
    
    public int getPlayerTotalWins() {
        return playerTotalWins;
    }
    
    public int getPlayerTotalLosses() {
        return playerTotalLosses;
    }
    
    public double getWinRate() {
        int total = playerTotalWins + playerTotalLosses;
        return total > 0 ? (double) playerTotalWins / total : 0.0;
    }
    
    public boolean isGameCompleted() {
        return levels.stream().allMatch(Level::isCompleted);
    }
    
    public String getProgressReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n+===============================================+\n");
        report.append("|          PROGRESO DEL JUGADOR                 |\n");
        report.append("+===============================================+\n");
        report.append(String.format("| Victorias Totales: %-26d |\n", playerTotalWins));
        report.append(String.format("| Derrotas Totales: %-27d |\n", playerTotalLosses));
        report.append(String.format("| Tasa de Victoria: %.1f%%%-25s |\n", 
                                   getWinRate() * 100, ""));
        report.append(String.format("| Nivel Actual: %-31d |\n", currentLevel + 1));
        report.append("+===============================================+\n\n");
        
        report.append("NIVELES:\n");
        report.append("─────────────────────────────────────────────────\n");
        for (Level level : levels) {
            report.append(level.toString()).append("\n\n");
        }
        
        return report.toString();
    }
    
    public void setPlayerTotalWins(int wins) {
        this.playerTotalWins = wins;
    }
    
    public void setPlayerTotalLosses(int losses) {
        this.playerTotalLosses = losses;
    }
    
    public void setCurrentLevel(int levelIndex) {
        if (levelIndex >= 0 && levelIndex < levels.size()) {
            this.currentLevel = levelIndex;
        }
    }
    
    public void reset() {
        currentLevel = 0;
        playerTotalWins = 0;
        playerTotalLosses = 0;
        
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            level.completed = false;
            level.unlocked = (i == 0);
            level.getBoss().reset();
        }
    }
}
