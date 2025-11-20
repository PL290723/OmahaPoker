package poker.core;

import java.util.*;

/**
 * Representa una baraja de 52 cartas con funcionalidad de mezcla
 */
public class Deck {
    private LinkedList<Card> cards;
    private Random random;
    
    public Deck() {
        this.random = new Random();
        reset();
    }
    
    public void reset() {
        cards = new LinkedList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
    }
    
    public void shuffle() {
        Collections.shuffle(cards, random);
    }
    
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("La baraja está vacía");
        }
        return cards.removeFirst();
    }
    
    public List<Card> drawMultiple(int count) {
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            drawn.add(draw());
        }
        return drawn;
    }
    
    public int size() {
        return cards.size();
    }
    
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
