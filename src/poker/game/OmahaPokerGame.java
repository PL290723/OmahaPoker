package poker.game;

import poker.core.*;
import poker.ai.*;
import java.util.*;

/**
 * Motor principal del juego de Omaha Poker con lógica completa de rondas
 */
public class OmahaPokerGame {
    
    public enum GamePhase {
        PRE_FLOP("Pre-Flop"),
        FLOP("Flop"),
        TURN("Turn"),
        RIVER("River"),
        SHOWDOWN("Showdown");
        
        private final String display;
        
        GamePhase(String display) {
            this.display = display;
        }
        
        public String getDisplay() {
            return display;
        }
    }
    
    private Player humanPlayer;
    private Player bossPlayer;
    private BossCharacter boss;
    private Deck deck;
    private List<Card> communityCards;
    private int pot;
    private int currentBet;
    private int smallBlind;
    private int bigBlind;
    private GamePhase phase;
    private int dealerButton; // 0 = humano, 1 = boss
    private boolean gameActive;
    private Queue<String> actionLog;
    private TournamentStats tournamentStats; // Sistema de rankings con PriorityQueue
    
    public OmahaPokerGame(Player human, BossCharacter bossChar, int smallBlind, int bigBlind) {
        this.humanPlayer = human;
        this.boss = bossChar;
        this.bossPlayer = new Player(boss.getName(), boss.getStartingChips(), Player.PlayerType.BOSS);
        this.deck = new Deck();
        this.communityCards = new ArrayList<>();
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.dealerButton = 0;
        this.actionLog = new LinkedList<>();
        this.tournamentStats = new TournamentStats(10); // Top 10 manos
        this.gameActive = false;
    }
    
    /**
     * Inicia una nueva mano
     */
    public void startNewHand() {
        // Reiniciar estado
        deck.reset();
        deck.shuffle();
        communityCards.clear();
        pot = 0;
        currentBet = 0;
        phase = GamePhase.PRE_FLOP;
        gameActive = true;
        actionLog.clear();
        
        humanPlayer.resetForNewHand();
        bossPlayer.resetForNewHand();
        
        // Alternar botón de dealer
        dealerButton = (dealerButton + 1) % 2;
        
        // Repartir cartas (4 por jugador en Omaha)
        dealHands();
        
        // Ciegas
        postBlinds();
        
        logAction("===========================================");
        logAction("      NUEVA MANO - " + phase.getDisplay());
        logAction("===========================================");
    }
    
    private void dealHands() {
        Hand humanHand = new Hand(deck.drawMultiple(4));
        Hand bossHand = new Hand(deck.drawMultiple(4));
        
        humanPlayer.setHand(humanHand);
        bossPlayer.setHand(bossHand);
    }
    
    private void postBlinds() {
        Player smallBlindPlayer = dealerButton == 0 ? humanPlayer : bossPlayer;
        Player bigBlindPlayer = dealerButton == 0 ? bossPlayer : humanPlayer;
        
        // Ciega pequeña
        int sbAmount = Math.min(smallBlind, smallBlindPlayer.getChips());
        smallBlindPlayer.removeChips(sbAmount);
        smallBlindPlayer.addToBet(sbAmount);
        pot += sbAmount;
        logAction(smallBlindPlayer.getName() + " paga ciega pequeña: " + sbAmount);
        
        // Ciega grande
        int bbAmount = Math.min(bigBlind, bigBlindPlayer.getChips());
        bigBlindPlayer.removeChips(bbAmount);
        bigBlindPlayer.addToBet(bbAmount);
        pot += bbAmount;
        currentBet = bbAmount;
        logAction(bigBlindPlayer.getName() + " paga ciega grande: " + bbAmount);
    }
    
    /**
     * Ejecuta una ronda de apuestas
     * Retorna true si la ronda está completa, false si necesita input del jugador
     */
    public boolean executeBettingRound() {
        // Determinar quién actúa primero
        Player currentPlayer = (phase == GamePhase.PRE_FLOP) 
            ? (dealerButton == 0 ? humanPlayer : bossPlayer)
            : (dealerButton == 0 ? bossPlayer : humanPlayer);
        
        int actionCount = 0;
        int maxActions = 20; // Prevenir bucles infinitos
        
        while (gameActive && actionCount < maxActions) {
            actionCount++;
            
            // Verificar si la ronda terminó
            if (bossPlayer.isFolded() || humanPlayer.isFolded()) {
                gameActive = false;
                return true;
            }
            
            // Ronda completa cuando ambos tienen la misma apuesta y ambos han actuado
            if (humanPlayer.getCurrentBet() == bossPlayer.getCurrentBet() 
                && humanPlayer.hasActed() && bossPlayer.hasActed()) {
                return true;
            }
            
            // Si el jugador actual puede y necesita actuar
            if (currentPlayer.canAct() && 
                (!currentPlayer.hasActed() || currentPlayer.getCurrentBet() < getCurrentBet())) {
                
                if (currentPlayer == humanPlayer) {
                    // Es turno del humano
                    return false;
                } else {
                    // Es turno del boss
                    executeBossAction();
                    bossPlayer.setHasActed(true);
                }
            }
            
            // Cambiar al otro jugador
            currentPlayer = (currentPlayer == humanPlayer) ? bossPlayer : humanPlayer;
        }
        
        return true;
    }
    
    /**
     * Ejecuta la acción del boss usando IA
     */
    private void executeBossAction() {
        AIStrategy.GameContext context = createGameContext(bossPlayer);
        AIStrategy.Action action = boss.getStrategy().decideAction(context);
        
        // MOSTRAR LA DECISIÓN DE LA IA ANTES DE EJECUTARLA
        System.out.println("\n>>> " + boss.getName() + " está pensando...");
        try {
            Thread.sleep(800); // Pausa dramática
        } catch (InterruptedException e) {
            // Ignorar
        }
        
        executeAction(bossPlayer, action, context);
    }
    
    private AIStrategy.GameContext createGameContext(Player player) {
        int callAmount = currentBet - player.getCurrentBet();
        int position = (player == humanPlayer) ? (dealerButton == 0 ? 0 : 1) : (dealerButton == 0 ? 1 : 0);
        
        // Calcular agresión del oponente basada en el perfil
        double opponentAggression = 0.5;
        if (boss.getMlEngine().getPlayerProfile().handsPlayed > 0) {
            opponentAggression = boss.getMlEngine().getPlayerProfile().aggressionLevel;
        }
        
        return new AIStrategy.GameContext(
            player.getHand(),
            communityCards,
            pot,
            callAmount,
            player.getChips(),
            position,
            2, // Total de jugadores activos
            opponentAggression,
            bigBlind
        );
    }
    
    /**
     * Ejecuta una acción de un jugador
     */
    public void executeAction(Player player, AIStrategy.Action action, AIStrategy.GameContext context) {
        String actionStr = action.name();
        
        switch (action) {
            case FOLD:
                player.fold();
                String foldMsg = player.getName() + " se retira (FOLD)";
                logAction(foldMsg);
                if (player == bossPlayer) {
                    System.out.println(">>> " + foldMsg);
                }
                gameActive = false;
                break;
                
            case CHECK:
                String checkMsg = player.getName() + " hace CHECK";
                logAction(checkMsg);
                if (player == bossPlayer) {
                    System.out.println(">>> " + checkMsg);
                }
                break;
                
            case CALL:
                int callAmount = currentBet - player.getCurrentBet();
                callAmount = Math.min(callAmount, player.getChips());
                player.removeChips(callAmount);
                player.addToBet(callAmount);
                pot += callAmount;
                
                String callMsg;
                if (player.getChips() == 0) {
                    player.setAllIn(true);
                    callMsg = player.getName() + " hace ALL-IN igualando: $" + callAmount;
                } else {
                    callMsg = player.getName() + " iguala (CALL): $" + callAmount;
                }
                logAction(callMsg);
                if (player == bossPlayer) {
                    System.out.println(">>> " + callMsg);
                }
                break;
                
            case RAISE:
                int targetBet;
                
                // Si es el boss, calcular el monto con su estrategia
                if (player == bossPlayer) {
                    targetBet = boss.getStrategy().calculateRaiseAmount(context);
                } else {
                    // Si es el humano, el targetBet ya está en el context o usar un default
                    // Por ahora, usar un raise típico de 2x el big blind
                    int humanCallAmount = context.getCallAmount();
                    if (humanCallAmount == 0) {
                        targetBet = context.getBigBlind() * 2;
                    } else {
                        targetBet = humanCallAmount * 2;
                    }
                }
                
                // Calcular cuánto necesita agregar desde su apuesta actual
                int additionalAmount = targetBet - player.getCurrentBet();
                
                // Asegurar que tiene suficientes fichas
                if (additionalAmount > player.getChips()) {
                    // Si no tiene suficiente, hace ALL-IN
                    additionalAmount = player.getChips();
                    player.setAllIn(true);
                }
                
                // Realizar la apuesta
                player.removeChips(additionalAmount);
                player.addToBet(additionalAmount);
                pot += additionalAmount;
                
                // Actualizar la apuesta actual de la mesa
                currentBet = Math.max(currentBet, player.getCurrentBet());
                
                String raiseMsg = player.getName() + " sube (RAISE) a: $" + player.getCurrentBet() 
                                + " (añade $" + additionalAmount + ")";
                logAction(raiseMsg);
                if (player == bossPlayer) {
                    System.out.println(">>> " + raiseMsg);
                }
                break;
                
            case ALL_IN:
                int allInAmount = player.getChips();
                player.removeChips(allInAmount);
                player.addToBet(allInAmount);
                pot += allInAmount;
                player.setAllIn(true);
                
                if (player.getCurrentBet() > currentBet) {
                    currentBet = player.getCurrentBet();
                }
                
                String allInMsg = player.getName() + " hace ALL-IN: $" + allInAmount;
                logAction(allInMsg);
                if (player == bossPlayer) {
                    System.out.println(">>> " + allInMsg);
                }
                break;
        }
        
        // Aprendizaje del boss (mejorado)
        if (player == bossPlayer) {
            String situation = SimpleMLEngine.encodeSituation(
                phase.name(),
                HandEvaluator.calculateHandStrength(player.getHand().getCards(), communityCards),
                pot,
                player.getChips()
            );
            
            // Recompensa inmediata basada en el resultado de la acción
            double immediateReward = 0.0;
            if (action == AIStrategy.Action.RAISE && pot > 0) {
                immediateReward = 0.1; // Pequeña recompensa por ser agresivo
            } else if (action == AIStrategy.Action.FOLD && currentBet > pot / 2) {
                immediateReward = 0.05; // Recompensa por evitar perder muchas fichas
            }
            
            boss.getMlEngine().recordExperience(situation, actionStr, immediateReward);
            
            // Aprender del contexto más amplio
            if (phase != GamePhase.PRE_FLOP) {
                String phaseContext = phase.name() + "_" + actionStr;
                boss.getMlEngine().recordExperience(phaseContext, "DECISION", immediateReward);
            }
        } else {
            // Registrar acción del jugador humano para análisis (mejorado)
            int betSize = 0;
            if (action == AIStrategy.Action.RAISE || action == AIStrategy.Action.CALL) {
                betSize = player.getCurrentBet();
            } else if (action == AIStrategy.Action.ALL_IN) {
                betSize = player.getCurrentBet();
            }
            
            boss.getMlEngine().recordPlayerAction(actionStr, betSize, currentBet > 0);
            
            // La IA aprende del comportamiento del jugador en tiempo real
            if (boss.getDifficulty() >= 3) {
                String playerPattern = phase.name() + "_PLAYER_" + actionStr;
                double patternValue = action == AIStrategy.Action.RAISE ? 0.15 : 
                                    action == AIStrategy.Action.FOLD ? -0.1 : 0.05;
                boss.getMlEngine().recordExperience(playerPattern, "OBSERVED", patternValue);
            }
        }
    }
    
    /**
     * Avanza a la siguiente fase del juego
     */
    public void advancePhase() {
        // Resetear apuestas y estado de actuación para la nueva fase
        humanPlayer.resetBet();
        bossPlayer.resetBet();
        humanPlayer.resetForNewRound();
        bossPlayer.resetForNewRound();
        currentBet = 0;
        
        switch (phase) {
            case PRE_FLOP:
                // Repartir flop (3 cartas)
                deck.draw(); // Quemar carta
                communityCards.addAll(deck.drawMultiple(3));
                phase = GamePhase.FLOP;
                logAction("\n--- FLOP ---");
                logAction("Cartas comunitarias: " + formatCards(communityCards));
                break;
                
            case FLOP:
                // Repartir turn (1 carta)
                deck.draw(); // Quemar carta
                communityCards.add(deck.draw());
                phase = GamePhase.TURN;
                logAction("\n--- TURN ---");
                logAction("Cartas comunitarias: " + formatCards(communityCards));
                break;
                
            case TURN:
                // Repartir river (1 carta)
                deck.draw(); // Quemar carta
                communityCards.add(deck.draw());
                phase = GamePhase.RIVER;
                logAction("\n--- RIVER ---");
                logAction("Cartas comunitarias: " + formatCards(communityCards));
                break;
                
            case RIVER:
                phase = GamePhase.SHOWDOWN;
                break;
                
            case SHOWDOWN:
                // No hacer nada, ya estamos en showdown
                break;
        }
    }
    
    /**
     * Determina el ganador en el showdown
     */
    public Player determineWinner() {
        logAction("\n===========================================");
        logAction("              SHOWDOWN");
        logAction("===========================================");
        
        // Si alguien hizo fold
        if (humanPlayer.isFolded()) {
            logAction(bossPlayer.getName() + " gana por fold del oponente");
            bossPlayer.addChips(pot);
            return bossPlayer;
        }
        
        if (bossPlayer.isFolded()) {
            logAction(humanPlayer.getName() + " gana por fold del oponente");
            humanPlayer.addChips(pot);
            return humanPlayer;
        }
        
        // Evaluar manos
        HandEvaluator.HandResult humanResult = HandEvaluator.evaluateOmahaHand(
            humanPlayer.getHand().getCards(),
            communityCards
        );
        
        HandEvaluator.HandResult bossResult = HandEvaluator.evaluateOmahaHand(
            bossPlayer.getHand().getCards(),
            communityCards
        );
        
        logAction("\n" + humanPlayer.getName() + " tiene: " + humanResult);
        logAction("Mano: " + humanPlayer.getHand());
        
        logAction("\n" + bossPlayer.getName() + " tiene: " + bossResult);
        logAction("Mano: " + bossPlayer.getHand());
        
        logAction("\nCartas comunitarias: " + formatCards(communityCards));
        
        int comparison = humanResult.compareTo(bossResult);
        
        if (comparison > 0) {
            logAction("\n" + humanPlayer.getName() + " GANA con " + humanResult + "!");
            humanPlayer.addChips(pot);
            
            // Registrar mano ganadora en estadísticas
            tournamentStats.recordHand(
                humanPlayer.getName(),
                humanResult.getRank(),
                humanResult.getBestCards(),
                pot,
                phase.name()
            );
            
            // Aprendizaje del boss (perdió)
            String situation = SimpleMLEngine.encodeSituation(
                phase.name(),
                HandEvaluator.calculateHandStrength(bossPlayer.getHand().getCards(), communityCards),
                pot,
                bossPlayer.getChips()
            );
            boss.learnFromHand(situation, "SHOWDOWN", false);
            
            return humanPlayer;
        } else if (comparison < 0) {
            logAction("\n" + bossPlayer.getName() + " GANA con " + bossResult + "!");
            bossPlayer.addChips(pot);
            
            // Registrar mano ganadora en estadísticas
            tournamentStats.recordHand(
                bossPlayer.getName(),
                bossResult.getRank(),
                bossResult.getBestCards(),
                pot,
                phase.name()
            );
            
            // Aprendizaje del boss (ganó)
            String situation = SimpleMLEngine.encodeSituation(
                phase.name(),
                HandEvaluator.calculateHandStrength(bossPlayer.getHand().getCards(), communityCards),
                pot,
                bossPlayer.getChips()
            );
            boss.learnFromHand(situation, "SHOWDOWN", true);
            
            return bossPlayer;
        } else {
            logAction("\nEMPATE - El pozo se divide");
            int split = pot / 2;
            humanPlayer.addChips(split);
            bossPlayer.addChips(pot - split);
            
            // Registrar ambas manos en caso de empate
            tournamentStats.recordHand(
                humanPlayer.getName(),
                humanResult.getRank(),
                humanResult.getBestCards(),
                split,
                phase.name()
            );
            tournamentStats.recordHand(
                bossPlayer.getName(),
                bossResult.getRank(),
                bossResult.getBestCards(),
                pot - split,
                phase.name()
            );
            
            return null;
        }
    }
    
    private String formatCards(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            sb.append(cards.get(i));
            if (i < cards.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
    
    private void logAction(String action) {
        actionLog.offer(action);
        if (actionLog.size() > 50) {
            actionLog.poll();
        }
    }
    
    // Getters
    public Player getHumanPlayer() { return humanPlayer; }
    public Player getBossPlayer() { return bossPlayer; }
    public BossCharacter getBoss() { return boss; }
    public List<Card> getCommunityCards() { return new ArrayList<>(communityCards); }
    public int getPot() { return pot; }
    public int getCurrentBet() { return currentBet; }
    public GamePhase getPhase() { return phase; }
    public boolean isGameActive() { return gameActive; }
    public Queue<String> getActionLog() { return new LinkedList<>(actionLog); }
    public int getBigBlind() { return bigBlind; }
    public TournamentStats getTournamentStats() { return tournamentStats; }
    
    public String getGameState() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n+===============================================+\n");
        sb.append(String.format("| Fase: %-39s |\n", phase.getDisplay()));
        sb.append("+===============================================+\n");
        sb.append(String.format("| Pozo: $%-38d |\n", pot));
        sb.append(String.format("| Apuesta Actual: $%-29d |\n", currentBet));
        sb.append("+===============================================+\n");
        
        if (!communityCards.isEmpty()) {
            sb.append(String.format("| Cartas Comunitarias: %-24s |\n", formatCards(communityCards)));
        }
        
        sb.append("+===============================================+\n");
        return sb.toString();
    }
}
