package poker.core;

/**
 * Representa un jugador en el juego con fichas, mano y estado
 */
public class Player {
    private String name;
    private int chips;
    private Hand hand;
    private int currentBet;
    private boolean folded;
    private boolean allIn;
    private boolean hasActed; // Indica si ya actuó en esta ronda de apuestas
    private PlayerType type;
    
    public enum PlayerType {
        HUMAN, AI, BOSS
    }
    
    public Player(String name, int chips, PlayerType type) {
        this.name = name;
        this.chips = chips;
        this.hand = new Hand();
        this.currentBet = 0;
        this.folded = false;
        this.allIn = false;
        this.hasActed = false;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public int getChips() {
        return chips;
    }
    
    public void addChips(int amount) {
        this.chips += amount;
    }
    
    public void removeChips(int amount) {
        this.chips -= amount;
        if (this.chips < 0) {
            this.chips = 0;
        }
    }
    
    public Hand getHand() {
        return hand;
    }
    
    public void setHand(Hand hand) {
        this.hand = hand;
    }
    
    public int getCurrentBet() {
        return currentBet;
    }
    
    public void addToBet(int amount) {
        this.currentBet += amount;
    }
    
    public void resetBet() {
        this.currentBet = 0;
    }
    
    public boolean isFolded() {
        return folded;
    }
    
    public void fold() {
        this.folded = true;
    }
    
    public boolean isAllIn() {
        return allIn;
    }
    
    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }
    
    public PlayerType getType() {
        return type;
    }
    
    public void resetForNewHand() {
        this.hand.clear();
        this.currentBet = 0;
        this.folded = false;
        this.allIn = false;
        this.hasActed = false;
    }
    
    public void resetForNewRound() {
        this.hasActed = false;
    }
    
    public void setHasActed(boolean hasActed) {
        this.hasActed = hasActed;
    }
    
    public boolean hasActed() {
        return this.hasActed;
    }
    
    public boolean canAct() {
        return !folded && !allIn && chips > 0;
    }
    
    @Override
    public String toString() {
        return name + " (Fichas: " + chips + ")";
    }
}
