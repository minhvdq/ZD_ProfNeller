/* ZDOptimalReachability - recursively computes the reachability of game states assuming that one player is optimal and the other is free to make any decision */
public class ZDOptimalReachability {
    final int GOAL_SCORE = ZombieDiceSolver.GOAL_SCORE, MAX_SCORE = ZombieDiceSolver.MAX_SCORE; 
    final ZDPolicy optimalPolicy = new ZombieDiceSolver();
    public boolean[][][][][] isReachable1; // whether or not and optimal player 1 could reach the given state, indexed by player, score, opponent score, turn total (brains), dice state index
    public boolean[][][][][] isReachable2; // whether or not and optimal player 2 could reach the given state, indexed by player, score, opponent score, turn total (brains), dice state index

    public ZDOptimalReachability() {
        isReachable1 = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates];
        isReachable2 = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates];
        computeReachability(1, 0, 0, 0, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
        computeReachability(2, 0, 0, 0, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
    }

    private void computeReachability(int optimalPlayer, int player, int currentPlayerScore, int opponentScore, int turnTotal, int diceStateIndex) { // computes the reachability for the given optimal player (1 or 2) and ZombieDiceState
        boolean[][][][][] isReachable = (optimalPlayer == 1) ? isReachable1 : isReachable2;
        // Truncate state variables to valid ranges.
        if (currentPlayerScore > MAX_SCORE) {
            currentPlayerScore = MAX_SCORE;
        }
        if (opponentScore > MAX_SCORE) {
            opponentScore = MAX_SCORE;
        }
        if (turnTotal > MAX_SCORE - currentPlayerScore) {
            turnTotal = MAX_SCORE - currentPlayerScore;
        }
        if (isReachable[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex]) {
            return; // already computed
        }
        isReachable[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] = true; // mark this state as reachable

        // Check if the game is over, and return if true.
        if (player == 0 && diceStateIndex == ZombieDiceSolver.INITIAL_DICE_STATE_INDEX // start of new round
            && (currentPlayerScore >= GOAL_SCORE || opponentScore >= GOAL_SCORE) // a player has achieved the goal score
            && (currentPlayerScore != opponentScore || currentPlayerScore == MAX_SCORE)) { // no continuing tie-breaker rounds
            return; // game is over, no further states to compute   
        }

        // Compute whether the optimal player would roll or hold in this state.
        int diceStateInteger = ZombieDiceSolver.diceStates[diceStateIndex];
        int[] diceStateArray = ZombieDiceSolver.getDiceStateArray(diceStateInteger);
        boolean willRoll = optimalPolicy.willRoll(player, currentPlayerScore, opponentScore, turnTotal,
            diceStateArray[ZombieDiceSolver.SHOTGUN_GREEN], diceStateArray[ZombieDiceSolver.SHOTGUN_YELLOW], diceStateArray[ZombieDiceSolver.SHOTGUN_RED],
            diceStateArray[ZombieDiceSolver.FOOTPRINT_GREEN], diceStateArray[ZombieDiceSolver.FOOTPRINT_YELLOW], diceStateArray[ZombieDiceSolver.FOOTPRINT_RED],
            diceStateArray[ZombieDiceSolver.SUPPLY_GREEN], diceStateArray[ZombieDiceSolver.SUPPLY_YELLOW], diceStateArray[ZombieDiceSolver.SUPPLY_RED]);

        // If rolling could be chosen by the current player, recursively compute the states reachable by rolling.
        if ((optimalPlayer - 1 != player) || willRoll) {
            // For each possible roll transition, recursively compute the state reachable by rolling.
			int numTransitions = ZombieDiceSolver.transitionFrequencies[diceStateIndex].length;
            for (int i = 0; i < numTransitions; i++) {
                int nextDiceStateIndex = ZombieDiceSolver.nextDiceStateIndices[diceStateIndex][i];
                int changeToTurnTotal = ZombieDiceSolver.changesToTurnTotal[diceStateIndex][i];
                if (nextDiceStateIndex == ZombieDiceSolver.SHOTGUN_BUSTED) {
                    computeReachability(optimalPlayer, 1 - player, opponentScore, currentPlayerScore, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
                } else {    
                    computeReachability(optimalPlayer, player, currentPlayerScore, opponentScore, turnTotal + changeToTurnTotal, nextDiceStateIndex);
                }
            }
        }

        // If holding could be chosen by the current player, recursively compute the states reachable by holding.
        if ((optimalPlayer - 1 != player) || !willRoll) {
            // If the current player holds, they score the turn total and the opponent's turn begins.
            computeReachability(optimalPlayer, 1 - player, opponentScore, currentPlayerScore + turnTotal, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
        }
    }

    public void printReachableStates(int tieScore, boolean printOnlyRoll) {
        System.out.println("action,current_optimal_player,current_score,opponent_score,turn_total,sg,sy,sr,fg,fy,fr,cg,cy,cr");
        for (int player = 0; player < 2; player++) {
            for (int turnTotal = 0; turnTotal <= MAX_SCORE - tieScore; turnTotal++) {
                for (int diceStateIndex = 0; diceStateIndex < ZombieDiceSolver.numDiceStates; diceStateIndex++) {
                    boolean isReachable = (player == 0) ? isReachable1[player][tieScore][tieScore][turnTotal][diceStateIndex]
                                                           : isReachable2[player][tieScore][tieScore][turnTotal][diceStateIndex];
                    if (isReachable) {
                        // Compute the dice state array for the current dice state index.
                        int diceStateInteger = ZombieDiceSolver.diceStates[diceStateIndex];
                        int[] diceState = ZombieDiceSolver.getDiceStateArray(diceStateInteger);
                        boolean willRoll = optimalPolicy.willRoll(player, tieScore, tieScore, turnTotal, 
                            diceState[ZombieDiceSolver.SHOTGUN_GREEN], diceState[ZombieDiceSolver.SHOTGUN_YELLOW], diceState[ZombieDiceSolver.SHOTGUN_RED],
                            diceState[ZombieDiceSolver.FOOTPRINT_GREEN], diceState[ZombieDiceSolver.FOOTPRINT_YELLOW], diceState[ZombieDiceSolver.FOOTPRINT_RED],
                            diceState[ZombieDiceSolver.SUPPLY_GREEN], diceState[ZombieDiceSolver.SUPPLY_YELLOW], diceState[ZombieDiceSolver.SUPPLY_RED]);
                        if (willRoll || !printOnlyRoll) {
                            // Print the reachable state.
                            System.out.printf("%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
                                (willRoll ? "roll" : "hold"), player, tieScore, tieScore, turnTotal,
                                diceState[ZombieDiceSolver.SHOTGUN_GREEN], diceState[ZombieDiceSolver.SHOTGUN_YELLOW], diceState[ZombieDiceSolver.SHOTGUN_RED],
                                diceState[ZombieDiceSolver.FOOTPRINT_GREEN], diceState[ZombieDiceSolver.FOOTPRINT_YELLOW], diceState[ZombieDiceSolver.FOOTPRINT_RED],
                                diceState[ZombieDiceSolver.SUPPLY_GREEN], diceState[ZombieDiceSolver.SUPPLY_YELLOW], diceState[ZombieDiceSolver.SUPPLY_RED]);
                        } // printing reachable state
                    } // isReachable                            
                } // diceStateIndex
            } // turnTotal
        } // player
    }

    public static void main(String[] args) {
        ZDOptimalReachability reachability = new ZDOptimalReachability();
        // Example usage: print reachable states for the optimal player with a tie score of 20 where the player will roll:
        // reachability.printReachableStates(20, true);
        
        reachability.printReachableStates(20, false);

    }
}
