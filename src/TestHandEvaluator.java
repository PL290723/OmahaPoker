import poker.core.*;
import java.util.*;

public class TestHandEvaluator {
    
    public static void main(String[] args) {
        System.out.println("+===============================================+");
        System.out.println("|   PRUEBA DE RENDIMIENTO - HAND EVALUATOR     |");
        System.out.println("+===============================================+");
        System.out.println();
        
        Deck deck = new Deck();
        deck.shuffle();
        
        System.out.println("Evaluando 10 manos aleatorias de Omaha Poker...\n");
        
        for (int i = 1; i <= 10; i++) {
            System.out.println("-----------------------------------------------");
            System.out.println("MANO #" + i);
            System.out.println("-----------------------------------------------");
            
            List<Card> hand = deck.drawMultiple(4);
            System.out.print("Cartas privadas: ");
            for (Card c : hand) {
                System.out.print(c + " ");
            }
            System.out.println();
            
            List<Card> community = deck.drawMultiple(5);
            System.out.print("Cartas comunitarias: ");
            for (Card c : community) {
                System.out.print(c + " ");
            }
            System.out.println();
            
            HandEvaluator.HandResult result = HandEvaluator.evaluateOmahaHand(hand, community);
            
            System.out.println(">>> RESULTADO: " + result.getRank().getDescription());
            System.out.print(">>> Mejor mano de 5 cartas: ");
            for (Card c : result.getBestCards()) {
                System.out.print(c + " ");
            }
            System.out.println();
            System.out.println(">>> Fuerza: " + result.getRank().getValue() + "/10");
            System.out.println();
            
            deck.reset();
            deck.shuffle();
        }
        
        System.out.println("+===============================================+");
        System.out.println("|   PRUEBA COMPLETADA - 10 MANOS EVALUADAS     |");
        System.out.println("+===============================================+");
        
        System.out.println("\n\nPRUEBAS ESPECIFICAS DE JUGADAS:");
        System.out.println("===============================================\n");
        
        testSpecificHand();
    }
    
    private static void testSpecificHand() {
        System.out.println("Ejemplo: Evaluando un FULL HOUSE");
        System.out.println("-----------------------------------------------");
        
        List<Card> hand = Arrays.asList(
            new Card(Card.Rank.REY, Card.Suit.CORAZONES),
            new Card(Card.Rank.REY, Card.Suit.DIAMANTES),
            new Card(Card.Rank.JOTA, Card.Suit.TREBOLES),
            new Card(Card.Rank.JOTA, Card.Suit.PICAS)
        );
        
        List<Card> community = Arrays.asList(
            new Card(Card.Rank.REY, Card.Suit.PICAS),
            new Card(Card.Rank.JOTA, Card.Suit.CORAZONES),
            new Card(Card.Rank.CINCO, Card.Suit.DIAMANTES),
            new Card(Card.Rank.TRES, Card.Suit.TREBOLES),
            new Card(Card.Rank.DOS, Card.Suit.PICAS)
        );
        
        System.out.print("Cartas privadas: ");
        for (Card c : hand) {
            System.out.print(c + " ");
        }
        System.out.println();
        
        System.out.print("Cartas comunitarias: ");
        for (Card c : community) {
            System.out.print(c + " ");
        }
        System.out.println();
        
        HandEvaluator.HandResult result = HandEvaluator.evaluateOmahaHand(hand, community);
        
        System.out.println("\n>>> RESULTADO: " + result.getRank().getDescription());
        System.out.print(">>> Mejor mano de 5 cartas: ");
        for (Card c : result.getBestCards()) {
            System.out.print(c + " ");
        }
        System.out.println();
        System.out.println(">>> Fuerza: " + result.getRank().getValue() + "/10");
        System.out.println("\nExplicacion: Se usa EXACTAMENTE 2 cartas de la mano");
        System.out.println("y 3 del tablero (reglas de Omaha Poker)");
    }
}
