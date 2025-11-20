package poker.ai;

import poker.core.*;
import java.util.*;

/**
 * Estrategia de IA con diferentes estilos de juego y decisiones basadas en heurísticas
 */
public class AIStrategy {
    
    public enum PlayStyle {
        AGRESIVO(0.7, 0.8, 0.3),      // Apuesta y sube con frecuencia
        CONSERVADOR(0.3, 0.2, 0.6),   // Juega seguro, hace fold frecuente
        EQUILIBRADO(0.5, 0.5, 0.45),  // Balance entre agresión y precaución
        MANIACO(0.9, 0.9, 0.1),       // All-in constante, muy arriesgado
        CALCULADOR(0.4, 0.6, 0.5);    // Basado en probabilidades matemáticas
        
        private final double aggressionFactor;
        private final double bluffFrequency;
        private final double foldThreshold;
        
        PlayStyle(double aggressionFactor, double bluffFrequency, double foldThreshold) {
            this.aggressionFactor = aggressionFactor;
            this.bluffFrequency = bluffFrequency;
            this.foldThreshold = foldThreshold;
        }
        
        public double getAggressionFactor() {
            return aggressionFactor;
        }
        
        public double getBluffFrequency() {
            return bluffFrequency;
        }
        
        public double getFoldThreshold() {
            return foldThreshold;
        }
    }
    
    public enum Action {
        FOLD, CHECK, CALL, RAISE, ALL_IN
    }
    
    private PlayStyle style;
    private Random random;
    private Map<String, Double> learningWeights; // Para aprendizaje adaptativo
    
    public AIStrategy(PlayStyle style) {
        this.style = style;
        this.random = new Random();
        this.learningWeights = new HashMap<>();
        initializeWeights();
    }
    
    private void initializeWeights() {
        learningWeights.put("pre_flop_strength", 1.0);
        learningWeights.put("post_flop_strength", 1.0);
        learningWeights.put("pot_odds", 1.0);
        learningWeights.put("position", 1.0);
        learningWeights.put("opponent_aggression", 1.0);
    }
    
    /**
     * Decide la acción a tomar basada en el contexto del juego
     * VERSIÓN BALANCEADA - Apuestas inteligentes
     */
    public Action decideAction(GameContext context) {
        int callAmount = context.getCallAmount();
        int playerChips = context.getPlayerChips();
        double handStrength = evaluateHandStrength(context);
        double rand = random.nextDouble();
        double aggression = style.getAggressionFactor();
        
        // ============================================
        // CASO 1: SIN APUESTA (callAmount = 0)
        // ============================================
        if (callAmount == 0) {
            // Mano fuerte (0.60+) -> RAISE frecuente
            if (handStrength >= 0.60) {
                return (rand < 0.75 + aggression * 0.15) ? Action.RAISE : Action.CHECK;
            }
            
            // Mano buena (0.50-0.60) -> RAISE ocasional
            if (handStrength >= 0.50) {
                return (rand < 0.55 + aggression * 0.20) ? Action.RAISE : Action.CHECK;
            }
            
            // Mano decente (0.40-0.50) -> RAISE raro o CHECK
            if (handStrength >= 0.40) {
                return (rand < 0.30 + aggression * 0.15) ? Action.RAISE : Action.CHECK;
            }
            
            // Mano media-baja -> CHECK mayormente
            if (handStrength >= 0.30) {
                return (rand < 0.15 + aggression * 0.10) ? Action.RAISE : Action.CHECK;
            }
            
            // Mano baja -> CHECK casi siempre
            return (rand < 0.05 + aggression * 0.05) ? Action.RAISE : Action.CHECK;
        }
        
        // ============================================
        // CASO 2: CON APUESTA (callAmount > 0)
        // ============================================
        
        boolean bigBet = callAmount > playerChips * 0.30;
        boolean allInBet = callAmount >= playerChips * 0.90;
        
        // Mano muy fuerte (0.70+)
        if (handStrength >= 0.70) {
            if (allInBet) {
                return Action.ALL_IN;
            }
            return (rand < 0.70 + aggression * 0.20) ? Action.RAISE : Action.CALL;
        }
        
        // Mano fuerte (0.60-0.70)
        if (handStrength >= 0.60) {
            if (allInBet) {
                return (rand < 0.60) ? Action.CALL : Action.FOLD;
            }
            if (bigBet) {
                return (rand < 0.70) ? Action.CALL : Action.FOLD;
            }
            return (rand < 0.50 + aggression * 0.20) ? Action.RAISE : Action.CALL;
        }
        
        // Mano buena (0.50-0.60)
        if (handStrength >= 0.50) {
            if (allInBet) {
                return (rand < 0.30) ? Action.CALL : Action.FOLD;
            }
            if (bigBet) {
                return (rand < 0.50) ? Action.CALL : Action.FOLD;
            }
            return (rand < 0.30 + aggression * 0.15) ? Action.RAISE : Action.CALL;
        }
        
        // Mano decente (0.40-0.50)
        if (handStrength >= 0.40) {
            if (allInBet || bigBet) {
                return Action.FOLD;
            }
            return (rand < 0.70) ? Action.CALL : Action.FOLD;
        }
        
        // Mano media (0.30-0.40)
        if (handStrength >= 0.30) {
            if (bigBet) {
                return Action.FOLD;
            }
            return (rand < 0.40) ? Action.CALL : Action.FOLD;
        }
        
        // Mano baja -> FOLD casi siempre
        if (callAmount <= playerChips / 10) {
            return (rand < 0.20) ? Action.CALL : Action.FOLD;
        }
        
        return Action.FOLD;
    }
    
    /**
     * Evalúa la fuerza de la mano actual
     */
    private double evaluateHandStrength(GameContext context) {
        if (context.getCommunityCards().size() < 3) {
            return evaluatePreFlop(context.getPlayerHand());
        }
        
        HandEvaluator.HandResult result = HandEvaluator.evaluateOmahaHand(
            context.getPlayerHand().getCards(),
            context.getCommunityCards()
        );
        
        return result.getRank().getValue() / 10.0;
    }
    
    /**
     * Evalúa la mano pre-flop basada en pares y cartas altas
     * VERSIÓN ULTRA AGRESIVA - Valores muy altos
     */
    private double evaluatePreFlop(Hand hand) {
        List<Card> cards = hand.getCards();
        Map<Integer, Integer> rankCount = new HashMap<>();
        Map<Card.Suit, Integer> suitCount = new HashMap<>();
        int highCards = 0;
        int totalValue = 0;
        
        for (Card card : cards) {
            int rank = card.getRank().getValue();
            rankCount.put(rank, rankCount.getOrDefault(rank, 0) + 1);
            suitCount.put(card.getSuit(), suitCount.getOrDefault(card.getSuit(), 0) + 1);
            totalValue += rank;
            
            if (rank >= 10) { // Cartas altas (10, J, Q, K, A)
                highCards++;
            }
        }
        
        // Base moderada y realista
        double strength = 0.35;
        
        // Pares y tríos - Bonus moderado
        int pairCount = 0;
        for (int count : rankCount.values()) {
            if (count == 2) {
                pairCount++;
                strength += 0.15;
            }
            if (count == 3) {
                strength += 0.25;
            }
            if (count == 4) {
                strength += 0.35;
            }
        }
        
        // Múltiples pares
        if (pairCount >= 2) {
            strength += 0.12;
        }
        
        // Cartas altas - Bonus moderado
        strength += highCards * 0.08;
        
        // Si todas son altas
        if (highCards >= 3) {
            strength += 0.12;
        }
        
        // Conectividad - Bonus pequeño
        List<Integer> ranks = new ArrayList<>(rankCount.keySet());
        ranks.sort(Collections.reverseOrder());
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i) - ranks.get(i + 1) <= 3) {
                strength += 0.08;
            }
        }
        
        // Suited - Bonus moderado
        int maxSuited = 0;
        for (int count : suitCount.values()) {
            maxSuited = Math.max(maxSuited, count);
        }
        if (maxSuited == 2) strength += 0.08;
        if (maxSuited == 3) strength += 0.15;
        if (maxSuited == 4) strength += 0.22;
        
        // Valor promedio alto
        double avgValue = totalValue / 4.0;
        if (avgValue >= 8) {
            strength += 0.10;
        }
        if (avgValue >= 10) {
            strength += 0.08;
        }
        
        return Math.min(strength, 1.0);
    }
    
    /**
     * Calcula las probabilidades del pozo (pot odds)
     */
    private double calculatePotOdds(GameContext context) {
        int potSize = context.getPotSize();
        int callAmount = context.getCallAmount();
        
        if (callAmount == 0) return 1.0;
        if (potSize == 0) return 0.5;
        
        double odds = (double) potSize / (potSize + callAmount);
        
        // Comparar con probabilidad de mejorar
        int outs = HandEvaluator.calculateOuts(
            context.getPlayerHand().getCards(),
            context.getCommunityCards()
        );
        
        int cardsLeft = 52 - context.getPlayerHand().size() - context.getCommunityCards().size();
        double improvementProbability = (double) outs / cardsLeft;
        
        return improvementProbability > odds ? 0.8 : 0.3;
    }
    
    /**
     * Evalúa la ventaja posicional
     */
    private double evaluatePosition(GameContext context) {
        int position = context.getPlayerPosition();
        int totalPlayers = context.getTotalActivePlayers();
        
        // Posición tardía es mejor (más información)
        return (double) position / totalPlayers;
    }
    
    /**
     * Analiza el comportamiento de los oponentes
     */
    private double analyzeOpponents(GameContext context) {
        double factor = 0.5;
        
        // Si los oponentes son agresivos, jugar más conservador
        if (context.getOpponentAggression() > 0.7) {
            factor = 0.3;
        } else if (context.getOpponentAggression() < 0.3) {
            factor = 0.7; // Aprovechar jugadores pasivos
        }
        
        return factor;
    }
    
    /**
     * Ajusta la decisión según el estilo de juego
     */
    private double adjustForPlayStyle(double baseDecision, GameContext context) {
        double adjusted = baseDecision;
        
        // Aplicar factor de agresión
        adjusted += (random.nextDouble() - 0.5) * style.getAggressionFactor() * 0.3;
        
        // Posibilidad de bluff
        if (random.nextDouble() < style.getBluffFrequency()) {
            adjusted += 0.2;
        }
        
        // Ajustar según umbral de fold
        if (adjusted < style.getFoldThreshold() && context.getCallAmount() > 0) {
            adjusted *= 0.5; // Más propenso a fold
        }
        
        return Math.max(0.0, Math.min(1.0, adjusted));
    }
    
    /**
     * Selecciona la acción final basada en el valor de decisión
     */
    private Action selectAction(double decision, GameContext context) {
        int callAmount = context.getCallAmount();
        int playerChips = context.getPlayerChips();
        
        // No hay que igualar apuesta
        if (callAmount == 0) {
            if (decision > 0.7) {
                return Action.RAISE;
            } else if (decision > 0.4) {
                return Action.CHECK;
            } else {
                return Action.CHECK; // No puede hacer fold si no hay apuesta
            }
        }
        
        // Hay que igualar apuesta
        if (decision < 0.25) {
            return Action.FOLD;
        } else if (decision < 0.5) {
            return callAmount <= playerChips ? Action.CALL : Action.FOLD;
        } else if (decision < 0.8) {
            int raiseAmount = (int) (callAmount * (1 + style.getAggressionFactor()));
            return raiseAmount <= playerChips ? Action.RAISE : Action.CALL;
        } else {
            // Decisión muy fuerte - considerar all-in
            if (decision > 0.9 && random.nextDouble() < style.getAggressionFactor()) {
                return Action.ALL_IN;
            }
            return Action.RAISE;
        }
    }
    
    /**
     * Calcula el monto TOTAL a apostar para una subida
     * VERSIÓN BALANCEADA - Apuestas razonables
     */
    public int calculateRaiseAmount(GameContext context) {
        int potSize = context.getPotSize();
        int callAmount = context.getCallAmount();
        int playerChips = context.getPlayerChips();
        int bigBlind = context.getBigBlind();
        double aggression = style.getAggressionFactor();
        
        // Multiplicador basado en agresividad (1.0 a 1.6)
        double aggressionMultiplier = 1.0 + (aggression * 0.6);
        
        int minTotalBet;
        if (callAmount == 0) {
            // Sin apuesta previa: apostar entre 2x y 3x el big blind
            minTotalBet = (int) (bigBlind * 2.0 * aggressionMultiplier);
        } else {
            // Con apuesta: subir al menos 2x la apuesta actual
            minTotalBet = (int) (callAmount * 2.0 * aggressionMultiplier);
        }
        
        // Apostar basado en el pozo (entre 0.50x y 0.75x el pozo)
        int potBasedBet = (int) (potSize * (0.50 + aggression * 0.25));
        
        // Tomar el mayor entre mínimo y basado en pozo
        int targetBet = Math.max(minTotalBet, potBasedBet);
        
        // Asegurar un mínimo razonable
        targetBet = Math.max(targetBet, bigBlind * 2);
        
        // Si es muy grande (>50% del stack), reducir un poco
        if (targetBet > playerChips * 0.50) {
            targetBet = (int) (playerChips * 0.40);
        }
        
        // Si es >70% del stack, hacer all-in directamente
        if (targetBet > playerChips * 0.70) {
            return playerChips; // ALL-IN
        }
        
        return Math.min(targetBet, playerChips);
    }
    
    /**
     * Actualiza los pesos de aprendizaje basado en el resultado
     */
    public void updateWeights(String factor, boolean won) {
        double currentWeight = learningWeights.get(factor);
        double adjustment = won ? 0.05 : -0.03;
        
        learningWeights.put(factor, Math.max(0.5, Math.min(1.5, currentWeight + adjustment)));
    }
    
    public PlayStyle getStyle() {
        return style;
    }
    
    public void setStyle(PlayStyle style) {
        this.style = style;
    }
    
    /**
     * Contexto del juego para la toma de decisiones
     */
    public static class GameContext {
        private Hand playerHand;
        private List<Card> communityCards;
        private int potSize;
        private int callAmount;
        private int playerChips;
        private int playerPosition;
        private int totalActivePlayers;
        private double opponentAggression;
        private int bigBlind;
        
        public GameContext(Hand playerHand, List<Card> communityCards, int potSize,
                          int callAmount, int playerChips, int playerPosition,
                          int totalActivePlayers, double opponentAggression, int bigBlind) {
            this.playerHand = playerHand;
            this.communityCards = communityCards;
            this.potSize = potSize;
            this.callAmount = callAmount;
            this.playerChips = playerChips;
            this.playerPosition = playerPosition;
            this.totalActivePlayers = totalActivePlayers;
            this.opponentAggression = opponentAggression;
            this.bigBlind = bigBlind;
        }
        
        // Getters
        public Hand getPlayerHand() { return playerHand; }
        public List<Card> getCommunityCards() { return communityCards; }
        public int getPotSize() { return potSize; }
        public int getCallAmount() { return callAmount; }
        public int getPlayerChips() { return playerChips; }
        public int getPlayerPosition() { return playerPosition; }
        public int getTotalActivePlayers() { return totalActivePlayers; }
        public double getOpponentAggression() { return opponentAggression; }
        public int getBigBlind() { return bigBlind; }
    }
}
