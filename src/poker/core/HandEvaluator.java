package poker.core;

import java.util.*;

/**
 * Evalúa manos de Omaha Poker (2 cartas de mano + 3 del tablero)
 * y determina la mejor combinación posible
 */
public class HandEvaluator {
    
    public enum HandRank {
        CARTA_ALTA(1, "Carta Alta"),
        PAREJA(2, "Pareja"),
        DOS_PARES(3, "Dos Pares"),
        TRIO(4, "Trío"),
        ESCALERA(5, "Escalera"),
        COLOR(6, "Color"),
        FULL_HOUSE(7, "Full House"),
        POKER(8, "Póker"),
        ESCALERA_COLOR(9, "Escalera de Color"),
        ESCALERA_REAL(10, "Escalera Real");
        
        private final int value;
        private final String description;
        
        HandRank(int value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public static class HandResult implements Comparable<HandResult> {
        private HandRank rank;
        private List<Integer> tieBreakers;
        private List<Card> bestCards;
        
        public HandResult(HandRank rank, List<Integer> tieBreakers, List<Card> bestCards) {
            this.rank = rank;
            this.tieBreakers = tieBreakers;
            this.bestCards = bestCards;
        }
        
        public HandRank getRank() {
            return rank;
        }
        
        public List<Card> getBestCards() {
            return bestCards;
        }
        
        @Override
        public int compareTo(HandResult other) {
            if (this.rank.getValue() != other.rank.getValue()) {
                return Integer.compare(this.rank.getValue(), other.rank.getValue());
            }
            
            for (int i = 0; i < Math.min(tieBreakers.size(), other.tieBreakers.size()); i++) {
                int cmp = Integer.compare(this.tieBreakers.get(i), other.tieBreakers.get(i));
                if (cmp != 0) return cmp;
            }
            
            return 0;
        }
        
        @Override
        public String toString() {
            return rank.getDescription();
        }
    }
    
    /**
     * Evalúa la mejor mano de Omaha: exactamente 2 cartas de la mano privada
     * y exactamente 3 cartas del tablero comunitario
     */
    public static HandResult evaluateOmahaHand(List<Card> holeCards, List<Card> communityCards) {
        if (holeCards.size() != 4) {
            throw new IllegalArgumentException("Omaha requiere exactamente 4 cartas privadas");
        }
        if (communityCards.size() < 3) {
            throw new IllegalArgumentException("Se requieren al menos 3 cartas comunitarias");
        }
        
        HandResult bestResult = null;
        
        // Probar todas las combinaciones de 2 cartas de la mano
        for (int i = 0; i < holeCards.size(); i++) {
            for (int j = i + 1; j < holeCards.size(); j++) {
                List<Card> twoFromHand = Arrays.asList(holeCards.get(i), holeCards.get(j));
                
                // Probar todas las combinaciones de 3 cartas del tablero
                List<List<Card>> threeCardCombos = getCombinations(communityCards, 3);
                for (List<Card> threeFromBoard : threeCardCombos) {
                    List<Card> fiveCards = new ArrayList<>();
                    fiveCards.addAll(twoFromHand);
                    fiveCards.addAll(threeFromBoard);
                    
                    HandResult result = evaluateFiveCardHand(fiveCards);
                    if (bestResult == null || result.compareTo(bestResult) > 0) {
                        bestResult = result;
                    }
                }
            }
        }
        
        return bestResult;
    }
    
    /**
     * Evalúa una mano de 5 cartas y determina su rango
     */
    private static HandResult evaluateFiveCardHand(List<Card> cards) {
        if (cards.size() != 5) {
            throw new IllegalArgumentException("Debe haber exactamente 5 cartas");
        }
        
        List<Card> sortedCards = new ArrayList<>(cards);
        sortedCards.sort((a, b) -> Integer.compare(b.getRank().getValue(), a.getRank().getValue()));
        
        // Verificar cada tipo de mano en orden descendente
        HandResult result;
        
        if ((result = checkStraightFlush(sortedCards)) != null) return result;
        if ((result = checkFourOfAKind(sortedCards)) != null) return result;
        if ((result = checkFullHouse(sortedCards)) != null) return result;
        if ((result = checkFlush(sortedCards)) != null) return result;
        if ((result = checkStraight(sortedCards)) != null) return result;
        if ((result = checkThreeOfAKind(sortedCards)) != null) return result;
        if ((result = checkTwoPair(sortedCards)) != null) return result;
        if ((result = checkPair(sortedCards)) != null) return result;
        
        return checkHighCard(sortedCards);
    }
    
    private static HandResult checkStraightFlush(List<Card> cards) {
        if (isFlush(cards) && isStraight(cards)) {
            List<Integer> values = getCardValues(cards);
            boolean isRoyal = values.get(0) == 14; // As alto
            
            HandRank rank = isRoyal ? HandRank.ESCALERA_REAL : HandRank.ESCALERA_COLOR;
            return new HandResult(rank, values, cards);
        }
        return null;
    }
    
    private static HandResult checkFourOfAKind(List<Card> cards) {
        Map<Integer, List<Card>> groups = groupByRank(cards);
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 4) {
                List<Integer> tieBreakers = Arrays.asList(entry.getKey(), getKicker(cards, entry.getValue()));
                return new HandResult(HandRank.POKER, tieBreakers, cards);
            }
        }
        return null;
    }
    
    private static HandResult checkFullHouse(List<Card> cards) {
        Map<Integer, List<Card>> groups = groupByRank(cards);
        Integer threeValue = null;
        Integer pairValue = null;
        
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 3) {
                threeValue = entry.getKey();
            } else if (entry.getValue().size() == 2) {
                pairValue = entry.getKey();
            }
        }
        
        if (threeValue != null && pairValue != null) {
            List<Integer> tieBreakers = Arrays.asList(threeValue, pairValue);
            return new HandResult(HandRank.FULL_HOUSE, tieBreakers, cards);
        }
        return null;
    }
    
    private static HandResult checkFlush(List<Card> cards) {
        if (isFlush(cards)) {
            return new HandResult(HandRank.COLOR, getCardValues(cards), cards);
        }
        return null;
    }
    
    private static HandResult checkStraight(List<Card> cards) {
        if (isStraight(cards)) {
            return new HandResult(HandRank.ESCALERA, getCardValues(cards), cards);
        }
        return null;
    }
    
    private static HandResult checkThreeOfAKind(List<Card> cards) {
        Map<Integer, List<Card>> groups = groupByRank(cards);
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 3) {
                List<Integer> tieBreakers = new ArrayList<>();
                tieBreakers.add(entry.getKey());
                tieBreakers.addAll(getKickers(cards, entry.getValue(), 2));
                return new HandResult(HandRank.TRIO, tieBreakers, cards);
            }
        }
        return null;
    }
    
    private static HandResult checkTwoPair(List<Card> cards) {
        Map<Integer, List<Card>> groups = groupByRank(cards);
        List<Integer> pairValues = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 2) {
                pairValues.add(entry.getKey());
            }
        }
        
        if (pairValues.size() == 2) {
            pairValues.sort(Collections.reverseOrder());
            List<Card> pairCards = new ArrayList<>();
            for (Integer value : pairValues) {
                pairCards.addAll(groups.get(value));
            }
            
            List<Integer> tieBreakers = new ArrayList<>(pairValues);
            tieBreakers.add(getKicker(cards, pairCards));
            return new HandResult(HandRank.DOS_PARES, tieBreakers, cards);
        }
        return null;
    }
    
    private static HandResult checkPair(List<Card> cards) {
        Map<Integer, List<Card>> groups = groupByRank(cards);
        for (Map.Entry<Integer, List<Card>> entry : groups.entrySet()) {
            if (entry.getValue().size() == 2) {
                List<Integer> tieBreakers = new ArrayList<>();
                tieBreakers.add(entry.getKey());
                tieBreakers.addAll(getKickers(cards, entry.getValue(), 3));
                return new HandResult(HandRank.PAREJA, tieBreakers, cards);
            }
        }
        return null;
    }
    
    private static HandResult checkHighCard(List<Card> cards) {
        return new HandResult(HandRank.CARTA_ALTA, getCardValues(cards), cards);
    }
    
    // Métodos auxiliares
    
    private static boolean isFlush(List<Card> cards) {
        Card.Suit firstSuit = cards.get(0).getSuit();
        return cards.stream().allMatch(card -> card.getSuit() == firstSuit);
    }
    
    private static boolean isStraight(List<Card> cards) {
        List<Integer> values = getCardValues(cards);
        values.sort(Collections.reverseOrder());
        
        // Verificar secuencia normal
        boolean straight = true;
        for (int i = 0; i < values.size() - 1; i++) {
            if (values.get(i) - values.get(i + 1) != 1) {
                straight = false;
                break;
            }
        }
        
        if (straight) return true;
        
        // Verificar escalera con As bajo (A-2-3-4-5)
        if (values.contains(14) && values.contains(2) && values.contains(3) && 
            values.contains(4) && values.contains(5)) {
            return true;
        }
        
        return false;
    }
    
    private static Map<Integer, List<Card>> groupByRank(List<Card> cards) {
        Map<Integer, List<Card>> groups = new HashMap<>();
        for (Card card : cards) {
            int value = card.getRank().getValue();
            groups.computeIfAbsent(value, k -> new ArrayList<>()).add(card);
        }
        return groups;
    }
    
    private static List<Integer> getCardValues(List<Card> cards) {
        List<Integer> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(card.getRank().getValue());
        }
        values.sort(Collections.reverseOrder());
        return values;
    }
    
    private static Integer getKicker(List<Card> cards, List<Card> exclude) {
        return getKickers(cards, exclude, 1).get(0);
    }
    
    private static List<Integer> getKickers(List<Card> cards, List<Card> exclude, int count) {
        List<Integer> kickers = new ArrayList<>();
        for (Card card : cards) {
            if (!exclude.contains(card)) {
                kickers.add(card.getRank().getValue());
            }
        }
        kickers.sort(Collections.reverseOrder());
        return kickers.subList(0, Math.min(count, kickers.size()));
    }
    
    private static List<List<Card>> getCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        getCombinationsHelper(cards, k, 0, new ArrayList<>(), result);
        return result;
    }
    
    private static void getCombinationsHelper(List<Card> cards, int k, int start, 
                                             List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            getCombinationsHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    /**
     * Calcula la probabilidad de mejorar la mano en las siguientes calles
     */
    public static double calculateHandStrength(List<Card> holeCards, List<Card> communityCards) {
        if (communityCards.size() >= 3) {
            HandResult current = evaluateOmahaHand(holeCards, communityCards);
            return current.getRank().getValue() / 10.0;
        }
        return 0.5; // Pre-flop
    }
    
    /**
     * Calcula los outs (cartas que mejoran la mano)
     */
    public static int calculateOuts(List<Card> holeCards, List<Card> communityCards) {
        if (communityCards.size() < 3) {
            return 20; // Estimación pre-flop
        }
        
        HandResult current = evaluateOmahaHand(holeCards, communityCards);
        
        // Heurística simplificada
        switch (current.getRank()) {
            case CARTA_ALTA:
                return 15;
            case PAREJA:
                return 10;
            case DOS_PARES:
                return 8;
            case TRIO:
                return 7;
            case ESCALERA:
            case COLOR:
                return 4;
            default:
                return 2;
        }
    }
}
