package poker.core;

/**
 * Representa una carta de poker con valor y palo
 */
public class Card {
    public enum Suit {
        CORAZONES("C"), DIAMANTES("D"), TREBOLES("T"), PICAS("P");
        
        private final String symbol;
        
        Suit(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
    
    public enum Rank {
        DOS(2, "2"), TRES(3, "3"), CUATRO(4, "4"), CINCO(5, "5"), 
        SEIS(6, "6"), SIETE(7, "7"), OCHO(8, "8"), NUEVE(9, "9"), 
        DIEZ(10, "10"), JOTA(11, "J"), REINA(12, "Q"), REY(13, "K"), AS(14, "A");
        
        private final int value;
        private final String display;
        
        Rank(int value, String display) {
            this.value = value;
            this.display = display;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDisplay() {
            return display;
        }
    }
    
    private final Rank rank;
    private final Suit suit;
    
    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }
    
    public Rank getRank() {
        return rank;
    }
    
    public Suit getSuit() {
        return suit;
    }
    
    @Override
    public String toString() {
        return rank.getDisplay() + suit.getSymbol();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return rank == card.rank && suit == card.suit;
    }
    
    @Override
    public int hashCode() {
        return 31 * rank.hashCode() + suit.hashCode();
    }
}
