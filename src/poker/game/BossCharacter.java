package poker.game;

import poker.ai.*;
import poker.core.Player;

/**
 * Representa un jefe (boss) con características especiales y estilo único
 */
public class BossCharacter {
    private String name;
    private String description;
    private AIStrategy.PlayStyle primaryStyle;
    private AIStrategy.PlayStyle adaptiveStyle; // Estilo que cambia según el juego
    private int difficulty; // 1-5
    private int startingChips;
    private AIStrategy strategy;
    private SimpleMLEngine mlEngine;
    private String specialAbility;
    private boolean abilityUsed;
    
    public BossCharacter(String name, String description, AIStrategy.PlayStyle style,
                        int difficulty, int startingChips, String specialAbility) {
        this.name = name;
        this.description = description;
        this.primaryStyle = style;
        this.adaptiveStyle = style;
        this.difficulty = difficulty;
        this.startingChips = startingChips;
        this.strategy = new AIStrategy(style);
        this.mlEngine = new SimpleMLEngine();
        this.specialAbility = specialAbility;
        this.abilityUsed = false;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public AIStrategy.PlayStyle getPrimaryStyle() {
        return primaryStyle;
    }
    
    public AIStrategy.PlayStyle getAdaptiveStyle() {
        return adaptiveStyle;
    }
    
    public int getDifficulty() {
        return difficulty;
    }
    
    public int getStartingChips() {
        return startingChips;
    }
    
    public AIStrategy getStrategy() {
        return strategy;
    }
    
    public SimpleMLEngine getMlEngine() {
        return mlEngine;
    }
    
    public String getSpecialAbility() {
        return specialAbility;
    }
    
    public boolean isAbilityUsed() {
        return abilityUsed;
    }
    
    public void useAbility() {
        this.abilityUsed = true;
    }
    
    /**
     * Adapta el estilo de juego basándose en el aprendizaje
     */
    public void adaptStrategy() {
        if (difficulty >= 3) {
            adaptiveStyle = mlEngine.adaptStyleToOpponent(primaryStyle);
            strategy.setStyle(adaptiveStyle);
        }
    }
    
    /**
     * Registra el resultado de una mano para aprendizaje (mejorado)
     */
    public void learnFromHand(String situation, String action, boolean won) {
        double reward = won ? 1.0 : -0.5;
        mlEngine.recordExperience(situation, action, reward);
        
        // Aprender también de las acciones intermedias si ganó
        if (won && difficulty >= 2) {
            // Recompensa extra por ganar con esa estrategia
            String aimedAction = strategy.getStyle().name();
            mlEngine.recordExperience(situation + "_STYLE", aimedAction, 0.3);
        }
        
        // Adaptar estrategia más frecuentemente para aprender más rápido
        if (mlEngine.getTotalExperiences() % 3 == 0) {
            adaptStrategy();
        }
        
        // Ajustar pesos de la estrategia basándose en el resultado
        if (difficulty >= 3) {
            strategy.updateWeights("post_flop_strength", won);
            strategy.updateWeights("pot_odds", won);
        }
    }
    
    public void reset() {
        abilityUsed = false;
        adaptiveStyle = primaryStyle;
        strategy.setStyle(primaryStyle);
    }
    
    @Override
    public String toString() {
        return String.format("%s (*%d) - %s\nEstilo: %s\nHabilidad: %s",
                           name, difficulty, description, 
                           adaptiveStyle.name(), specialAbility);
    }
}
