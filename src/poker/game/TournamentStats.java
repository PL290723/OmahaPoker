package poker.game;

import poker.core.*;
import java.util.*;

/**
 * Sistema de estadísticas del torneo usando PriorityQueue (Heap)
 * Demuestra: Heaps, Comparators, operaciones O(log n), y ordenamiento automático
 */
public class TournamentStats {
    
    /**
     * Registro de una mano jugada con su resultado
     */
    public static class HandRecord implements Comparable<HandRecord> {
        private final String playerName;
        private final HandEvaluator.HandRank rank;
        private final List<Card> cards;
        private final int potWon;
        private final long timestamp;
        private final String phase;
        
        public HandRecord(String playerName, HandEvaluator.HandRank rank, 
                         List<Card> cards, int potWon, String phase) {
            this.playerName = playerName;
            this.rank = rank;
            this.cards = new ArrayList<>(cards);
            this.potWon = potWon;
            this.timestamp = System.currentTimeMillis();
            this.phase = phase;
        }
        
        @Override
        public int compareTo(HandRecord other) {
            // Ordenar por: 1) Rank de mano, 2) Pot ganado, 3) Timestamp
            int rankCmp = Integer.compare(this.rank.getValue(), other.rank.getValue());
            if (rankCmp != 0) return rankCmp;
            
            int potCmp = Integer.compare(this.potWon, other.potWon);
            if (potCmp != 0) return potCmp;
            
            return Long.compare(this.timestamp, other.timestamp);
        }
        
        public String getPlayerName() { return playerName; }
        public HandEvaluator.HandRank getRank() { return rank; }
        public List<Card> getCards() { return new ArrayList<>(cards); }
        public int getPotWon() { return potWon; }
        public String getPhase() { return phase; }
        
        @Override
        public String toString() {
            return String.format("%s ganó $%d con %s - %s", 
                playerName, potWon, rank.getDescription(), formatCards());
        }
        
        private String formatCards() {
            StringBuilder sb = new StringBuilder();
            for (Card c : cards) {
                sb.append(c.toString()).append(" ");
            }
            return sb.toString().trim();
        }
    }
    
    /**
     * Estadísticas de un jugador
     */
    public static class PlayerStats implements Comparable<PlayerStats> {
        private final String playerName;
        private int handsPlayed;
        private int handsWon;
        private int totalWinnings;
        private int biggestPot;
        private HandEvaluator.HandRank bestHand;
        private double winRate;
        
        public PlayerStats(String playerName) {
            this.playerName = playerName;
            this.handsPlayed = 0;
            this.handsWon = 0;
            this.totalWinnings = 0;
            this.biggestPot = 0;
            this.bestHand = HandEvaluator.HandRank.CARTA_ALTA;
            this.winRate = 0.0;
        }
        
        public void recordHand(boolean won, int potAmount, HandEvaluator.HandRank rank) {
            handsPlayed++;
            if (won) {
                handsWon++;
                totalWinnings += potAmount;
                if (potAmount > biggestPot) {
                    biggestPot = potAmount;
                }
            }
            
            if (rank.getValue() > bestHand.getValue()) {
                bestHand = rank;
            }
            
            winRate = (double) handsWon / handsPlayed;
        }
        
        @Override
        public int compareTo(PlayerStats other) {
            // Ordenar por: 1) WinRate, 2) Total de ganancias, 3) Mejor mano
            int winRateCmp = Double.compare(this.winRate, other.winRate);
            if (winRateCmp != 0) return winRateCmp;
            
            int winningsCmp = Integer.compare(this.totalWinnings, other.totalWinnings);
            if (winningsCmp != 0) return winningsCmp;
            
            return Integer.compare(this.bestHand.getValue(), other.bestHand.getValue());
        }
        
        public String getPlayerName() { return playerName; }
        public int getHandsPlayed() { return handsPlayed; }
        public int getHandsWon() { return handsWon; }
        public int getTotalWinnings() { return totalWinnings; }
        public int getBiggestPot() { return biggestPot; }
        public HandEvaluator.HandRank getBestHand() { return bestHand; }
        public double getWinRate() { return winRate; }
        
        @Override
        public String toString() {
            return String.format("%s: %.1f%% WR | $%d ganados | Mejor: %s | Manos: %d/%d",
                playerName, winRate * 100, totalWinnings, bestHand.getDescription(), 
                handsWon, handsPlayed);
        }
    }
    
    // ============= PRIORITY QUEUES (HEAPS) =============
    
    /**
     * Top manos del torneo - Max Heap (mejores primero)
     * Complejidad: O(log n) para inserción, O(1) para peek
     */
    private final PriorityQueue<HandRecord> topHands;
    
    /**
     * Ranking de jugadores - Max Heap (mejores jugadores primero)
     * Complejidad: O(log n) para inserción, O(1) para peek
     */
    private final PriorityQueue<PlayerStats> playerRanking;
    
    /**
     * Historial completo (para análisis) - usando TreeMap para orden cronológico
     */
    private final Map<String, PlayerStats> playerStatsMap;
    private final List<HandRecord> allHandsHistory;
    
    private final int maxTopHands;
    
    public TournamentStats(int maxTopHands) {
        this.maxTopHands = maxTopHands;
        
        // Max Heap: orden inverso (Collections.reverseOrder())
        this.topHands = new PriorityQueue<>(maxTopHands, Collections.reverseOrder());
        this.playerRanking = new PriorityQueue<>(Collections.reverseOrder());
        
        this.playerStatsMap = new HashMap<>();
        this.allHandsHistory = new ArrayList<>();
    }
    
    /**
     * Registra una mano jugada
     * O(log n) por las operaciones de heap
     */
    public void recordHand(String playerName, HandEvaluator.HandRank rank, 
                          List<Card> cards, int potWon, String phase) {
        
        HandRecord record = new HandRecord(playerName, rank, cards, potWon, phase);
        allHandsHistory.add(record);
        
        // Actualizar top hands (mantener solo las mejores)
        topHands.offer(record);
        if (topHands.size() > maxTopHands) {
            topHands.poll(); // Remover la peor
        }
        
        // Actualizar estadísticas del jugador
        PlayerStats stats = playerStatsMap.computeIfAbsent(playerName, PlayerStats::new);
        stats.recordHand(potWon > 0, potWon, rank);
        
        // Reconstruir ranking (en implementación real usaríamos update-heap)
        rebuildPlayerRanking();
    }
    
    /**
     * Reconstruye el heap de jugadores
     * O(n log n) - se puede optimizar con heapify
     */
    private void rebuildPlayerRanking() {
        playerRanking.clear();
        playerRanking.addAll(playerStatsMap.values());
    }
    
    /**
     * Obtiene las TOP N mejores manos
     * O(k log n) donde k = cantidad a obtener
     */
    public List<HandRecord> getTopHands(int count) {
        List<HandRecord> result = new ArrayList<>();
        PriorityQueue<HandRecord> copy = new PriorityQueue<>(topHands);
        
        int toGet = Math.min(count, copy.size());
        for (int i = 0; i < toGet; i++) {
            HandRecord record = copy.poll();
            if (record != null) {
                result.add(record);
            }
        }
        
        return result;
    }
    
    /**
     * Obtiene el TOP N de jugadores
     * O(k log n) donde k = cantidad a obtener
     */
    public List<PlayerStats> getTopPlayers(int count) {
        List<PlayerStats> result = new ArrayList<>();
        PriorityQueue<PlayerStats> copy = new PriorityQueue<>(playerRanking);
        
        int toGet = Math.min(count, copy.size());
        for (int i = 0; i < toGet; i++) {
            PlayerStats stats = copy.poll();
            if (stats != null) {
                result.add(stats);
            }
        }
        
        return result;
    }
    
    /**
     * Obtiene estadísticas de un jugador específico
     */
    public PlayerStats getPlayerStats(String playerName) {
        return playerStatsMap.get(playerName);
    }
    
    /**
     * Obtiene todas las manos jugadas (orden cronológico)
     */
    public List<HandRecord> getAllHandsHistory() {
        return new ArrayList<>(allHandsHistory);
    }
    
    /**
     * Análisis de distribución de manos (para demostrar Map + PriorityQueue)
     */
    public Map<HandEvaluator.HandRank, Integer> getHandDistribution() {
        Map<HandEvaluator.HandRank, Integer> distribution = new TreeMap<>(
            Comparator.comparingInt(HandEvaluator.HandRank::getValue).reversed()
        );
        
        for (HandRecord record : allHandsHistory) {
            distribution.merge(record.getRank(), 1, Integer::sum);
        }
        
        return distribution;
    }
    
    /**
     * Encuentra la racha más larga de victorias
     * Demuestra: algoritmo de análisis de secuencias
     */
    public int getLongestWinStreak(String playerName) {
        int maxStreak = 0;
        int currentStreak = 0;
        
        for (HandRecord record : allHandsHistory) {
            if (record.getPlayerName().equals(playerName) && record.getPotWon() > 0) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else if (record.getPlayerName().equals(playerName)) {
                currentStreak = 0;
            }
        }
        
        return maxStreak;
    }
    
    /**
     * Genera reporte completo para consola
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║         ESTADÍSTICAS DEL TORNEO (PriorityQueue)         ║\n");
        sb.append("╚══════════════════════════════════════════════════════════╝\n\n");
        
        // Top 5 mejores manos
        sb.append("🏆 TOP 5 MEJORES MANOS:\n");
        sb.append("─────────────────────────────────────────────────────────\n");
        List<HandRecord> topHandsList = getTopHands(5);
        for (int i = 0; i < topHandsList.size(); i++) {
            sb.append(String.format("  %d. %s\n", i + 1, topHandsList.get(i)));
        }
        
        // Ranking de jugadores
        sb.append("\n📊 RANKING DE JUGADORES:\n");
        sb.append("─────────────────────────────────────────────────────────\n");
        List<PlayerStats> topPlayersList = getTopPlayers(10);
        for (int i = 0; i < topPlayersList.size(); i++) {
            sb.append(String.format("  %d. %s\n", i + 1, topPlayersList.get(i)));
        }
        
        // Distribución de manos
        sb.append("\n📈 DISTRIBUCIÓN DE MANOS:\n");
        sb.append("─────────────────────────────────────────────────────────\n");
        Map<HandEvaluator.HandRank, Integer> dist = getHandDistribution();
        for (Map.Entry<HandEvaluator.HandRank, Integer> entry : dist.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / allHandsHistory.size();
            sb.append(String.format("  %-20s: %3d veces (%.1f%%)\n", 
                entry.getKey().getDescription(), entry.getValue(), percentage));
        }
        
        sb.append("\n═══════════════════════════════════════════════════════════\n");
        sb.append(String.format("Total de manos jugadas: %d\n", allHandsHistory.size()));
        sb.append("═══════════════════════════════════════════════════════════\n");
        
        return sb.toString();
    }
    
    /**
     * Limpia todas las estadísticas
     */
    public void reset() {
        topHands.clear();
        playerRanking.clear();
        playerStatsMap.clear();
        allHandsHistory.clear();
    }
}
