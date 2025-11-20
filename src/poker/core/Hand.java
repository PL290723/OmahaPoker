package poker.core;

import java.util.*;

/**
 * Representa una mano de cartas en Omaha (4 cartas privadas)
 */
public class Hand {
    private List<Card> cards;
    
    public Hand() {
        this.cards = new ArrayList<>();
    }
    
    public Hand(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }
    
    public void addCard(Card card) {
        cards.add(card);
    }
    
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
    
    public int size() {
        return cards.size();
    }
    
    public void clear() {
        cards.clear();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            sb.append(cards.get(i));
            if (i < cards.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
