import java.util.Arrays;

/**
 * ZDPolicyEvaluator - Given two policy and an assumption that policies will hold to win, 
 * performs a value iteration on the fixed policies to compute
 * the probabilities of each policy winning against the other in each possible non-terminal state.
 * As output, the code shows computed win rates of each policy as first and second player, as well as
 * the average win rate (i.e. the expected win rate if a policy plays as first or second player each half of the time.

 * @author Todd W. Neller
 */
public class ZDPolicyEvaluator {

	static boolean verbose = false;
	static double epsilon = 1e-14; // value iteration convergence threshold default 1e-14

	final int ROLL = 1, HOLD = 0, GOAL_SCORE = ZombieDiceSolver.GOAL_SCORE, NUM_DICE = 3, MAX_SCORE = ZombieDiceSolver.MAX_SCORE; // roll action, hold action, goal score, number of dice rolled, maximum score
	double[][][][][] pWin1, pWin2; // indexed by player, score, opponent score, turn total (brains), dice state index
    boolean[][][][][] roll1, roll2; // indexed by player, score, opponent score, turn total (brains), dice state index
    ZDPolicy policy1, policy2; // policies to evaluate

	public ZDPolicyEvaluator(ZDPolicy policy1, ZDPolicy policy2) {
		this(policy1, policy2, epsilon);
	}

	public ZDPolicyEvaluator(ZDPolicy policy1, ZDPolicy policy2, double epsilon) {
        this.policy1 = policy1;
        this.policy2 = policy2;
        ZDPolicyEvaluator.epsilon = epsilon;
        init();
        evaluate();
    }


	private void init() {
		pWin1 = new double[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates]; // indexed by player, score, opponent score, turn total (brains), shotguns
		pWin2 = new double[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates]; 
        roll1 = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates]; // indexed by player, score, opponent score, turn total (brains), shotguns
        roll2 = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][ZombieDiceSolver.numDiceStates];

        // initialize the roll1 and roll2 arrays based on the policies
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i <= MAX_SCORE; i++) {
                for (int j = 0; j <= MAX_SCORE; j++) {
                    for (int b = 0; b <= MAX_SCORE - i; b++) {
                        for (int diceStateIdx = 0; diceStateIdx < ZombieDiceSolver.numDiceStates; diceStateIdx++) {
                            int diceState = ZombieDiceSolver.diceStates[diceStateIdx];
                            int[] state = ZombieDiceSolver.getDiceStateArray(diceState);
                            roll1[p][i][j][b][diceStateIdx] = policy1.willRoll(p, i, j, b, state[ZombieDiceSolver.SHOTGUN_GREEN], state[ZombieDiceSolver.SHOTGUN_YELLOW], 
                                state[ZombieDiceSolver.SHOTGUN_RED], state[ZombieDiceSolver.FOOTPRINT_GREEN], state[ZombieDiceSolver.FOOTPRINT_YELLOW],
                                state[ZombieDiceSolver.FOOTPRINT_RED], state[ZombieDiceSolver.SUPPLY_GREEN], state[ZombieDiceSolver.SUPPLY_YELLOW],
                                state[ZombieDiceSolver.SUPPLY_RED]);
                            roll2[p][i][j][b][diceStateIdx] = policy2.willRoll(p, i, j, b, state[ZombieDiceSolver.SHOTGUN_GREEN], state[ZombieDiceSolver.SHOTGUN_YELLOW],
                                state[ZombieDiceSolver.SHOTGUN_RED], state[ZombieDiceSolver.FOOTPRINT_GREEN], state[ZombieDiceSolver.FOOTPRINT_YELLOW],
                                state[ZombieDiceSolver.FOOTPRINT_RED], state[ZombieDiceSolver.SUPPLY_GREEN], state[ZombieDiceSolver.SUPPLY_YELLOW],
                                state[ZombieDiceSolver.SUPPLY_RED]);
                        } // diceStateIdx
                    } // b
                } // j
            } // i
        } // p
	}


	private void evaluate() {
	double maxChange; // Maximum change in winning probabilities for convergence
		do {
			maxChange = 0; // Reset the maximum change for this iteration
			int[] maxChangeState = new int[5]; // To track the state with the maximum change
			double newPWin = 0;
			for (int distToMaxScore = 0; distToMaxScore <= 2 * MAX_SCORE; distToMaxScore++) {
				System.out.println("Distance to max score: " + distToMaxScore);
				int minCurrentPlayerScore = Math.max(0, ZombieDiceSolver.GOAL_SCORE - distToMaxScore);
				for (int currentPlayerScore = MAX_SCORE; currentPlayerScore >= minCurrentPlayerScore; currentPlayerScore--) {
					int minOpponentScore = Math.max(0, ZombieDiceSolver.GOAL_SCORE - currentPlayerScore - distToMaxScore);
					for (int opponentScore = MAX_SCORE; opponentScore >= minOpponentScore; opponentScore--) {
						for (int turnTotal = MAX_SCORE - currentPlayerScore; turnTotal >= 0; turnTotal--) {
							for (int player = 0; player < ZombieDiceSolver.NUM_PLAYERS; player++) {
								for (int diceStateIndex = 0; diceStateIndex < ZombieDiceSolver.numDiceStates; diceStateIndex++) {

                                    // Update for policy 1

									// Initialize the winning probabilities for this state
									double pWinPrevious = pWin1[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex];

									double pWin = 0;
									if (roll1[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex]) {
									    
									    // Compute the winning probabilities for rolling
									    double pWinRoll = 0;
									    int transitionCount = ZombieDiceSolver.transitionCounts[diceStateIndex];
									    int numTransitions = ZombieDiceSolver.transitionFrequencies[diceStateIndex].length;
									    for (int i = 0; i < numTransitions; i++) {
											int nextDiceStateIndex = ZombieDiceSolver.nextDiceStateIndices[diceStateIndex][i];
											int changeToTurnTotal = ZombieDiceSolver.changesToTurnTotal[diceStateIndex][i];
											double transitionProbability = (double) ZombieDiceSolver.transitionFrequencies[diceStateIndex][i] / transitionCount;
											if (nextDiceStateIndex == ZombieDiceSolver.SHOTGUN_BUSTED) {
												// If busted, no score gain, reset turn total and change player
												pWinRoll += transitionProbability * (1 - pWin(2, 1 - player, opponentScore, currentPlayerScore, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX));
											} else {
												// Otherwise, update the winning probability based on the new state
												pWinRoll += transitionProbability * pWin(1, player, currentPlayerScore, opponentScore, turnTotal + changeToTurnTotal, nextDiceStateIndex);
											}
									    }
									    if (pWinRoll > 1.0) {
											pWinRoll = 1.0; // Correct for floating point errors adding to more than 1.0
									    }
									    pWin = pWinRoll;
									}
									else {
									    // Compute the winning probabilities for holding
									    pWin = 1 - pWin(2, 1 - player, opponentScore, currentPlayerScore + turnTotal, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
									}
									
									pWin1[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] = pWin;
									double change = Math.abs(pWin - pWinPrevious);
									if (change > maxChange) {
										maxChange = change; // Update the maximum change
										maxChangeState[0] = player;
										maxChangeState[1] = currentPlayerScore;
										maxChangeState[2] = opponentScore;
										maxChangeState[3] = turnTotal;
										maxChangeState[4] = diceStateIndex; // Store the state with the maximum change
									}

									// Update for policy 2
									
									pWinPrevious = pWin2[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex];
									pWin = 0;
									if (roll2[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex]) {
									    
									    // Compute the winning probabilities for rolling
									    double pWinRoll = 0;
									    int transitionCount = ZombieDiceSolver.transitionCounts[diceStateIndex];
									    int numTransitions = ZombieDiceSolver.transitionFrequencies[diceStateIndex].length;
									    for (int i = 0; i < numTransitions; i++) {
											int nextDiceStateIndex = ZombieDiceSolver.nextDiceStateIndices[diceStateIndex][i];
											int changeToTurnTotal = ZombieDiceSolver.changesToTurnTotal[diceStateIndex][i];
											double transitionProbability = (double) ZombieDiceSolver.transitionFrequencies[diceStateIndex][i] / transitionCount;
											if (nextDiceStateIndex == ZombieDiceSolver.SHOTGUN_BUSTED) {
												// If busted, no score gain, reset turn total and change player
												pWinRoll += transitionProbability * (1 - pWin(1, 1 - player, opponentScore, currentPlayerScore, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX));
											} else {
												// Otherwise, update the winning probability based on the new state
												pWinRoll += transitionProbability * pWin(2, player, currentPlayerScore, opponentScore, turnTotal + changeToTurnTotal, nextDiceStateIndex);
											}
									    }
									    if (pWinRoll > 1.0) {
											pWinRoll = 1.0; // Correct for floating point errors adding to more than 1.0
									    }
									    pWin = pWinRoll;
									}
									else {
									    // Compute the winning probabilities for holding
									    pWin = 1 - pWin(1, 1 - player, opponentScore, currentPlayerScore + turnTotal, 0, ZombieDiceSolver.INITIAL_DICE_STATE_INDEX);
									}
									
									// Determine the best action (roll or hold)
									pWin2[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] = pWin;
									change = Math.abs(pWin - pWinPrevious);
									if (change > maxChange) {
										maxChange = change; // Update the maximum change
										maxChangeState[0] = player;
										maxChangeState[1] = currentPlayerScore;
										maxChangeState[2] = opponentScore;
										maxChangeState[3] = turnTotal;
										maxChangeState[4] = diceStateIndex; // Store the state with the maximum change
									}
								} // End of dice state loop
							} // End of player loop
						} // End of turn total loop
					} // End of score2 loop
				} // End of score1 loop
			} // End of distance to max score loop
			System.out.println("Max change: " + maxChange + " for state: " + Arrays.toString(maxChangeState) + " with new pWin: " + newPWin);
		} while (maxChange > epsilon); // Continue until convergence
	}

    public double pWin(int policy, int player, int currentPlayerScore, int opponentScore, int turnTotal, int diceStateIndex) {
		// Ensure the scores and turn total are within bounds
		if (currentPlayerScore > MAX_SCORE)
			currentPlayerScore = MAX_SCORE; 
		if (opponentScore > MAX_SCORE)
			opponentScore = MAX_SCORE; 
		if (turnTotal > MAX_SCORE - currentPlayerScore)
			turnTotal = MAX_SCORE - currentPlayerScore; 
		// Return the winning probability for the given game state
		
		/* First we check for game-end conditions:
		 * - If player 0 has met or exceeded the goal score and has a higher score than player 1 
		 *   at the beginning of their turn (turnTotal == 0), player 0 wins.
		 * - If player 0 has not met the goal, but player 1 previously did, then player 0 loses.
 		 * - If, at the beginning of player 0's turn, player 1 has a greater score that achieves the goal score, player 0 loses.
		 * - If player 1 has a score and turn total sum that is greater than or equal to the goal score 
		 *   and is greater than player 0's score, player 1 wins.
		 * - If player 0 and player 1 have the maximum possible score, we call it a tie for analysis (although there are no ties in Zombie Dice). 
		 * (The max score should be set to a value well beyond the goal score such that this would be a suboptimal state to reach.)
		 */
		if (player == 0 && currentPlayerScore < GOAL_SCORE && opponentScore >= GOAL_SCORE)
			return 0.0;
		if (player == 0 && currentPlayerScore >= GOAL_SCORE && currentPlayerScore > opponentScore && turnTotal == 0) // game end; note not currentPlayerScore + turnTotal > goalScore; must have already held
			return 1.0;
		if (player == 0 && opponentScore >= GOAL_SCORE && currentPlayerScore < opponentScore && turnTotal == 0) // game end; note not currentPlayerScore + turnTotal > goalScore; must have already held
			return 0.0;
		if (player == 1 && currentPlayerScore + turnTotal >= GOAL_SCORE && currentPlayerScore + turnTotal > opponentScore)
			return 1.0;
		if (currentPlayerScore == MAX_SCORE && opponentScore == MAX_SCORE)
			return 0.5;
		return policy == 1 ? pWin1[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] : pWin2[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex];
	}

	public static void main(String[] args) {
		ZDPolicy optimal = new ZombieDiceSolver();

		// TODO - Experiment with creating new ZDPolicy classes to discover:
		// (1) The best-performing policy that can be computed with mental math, and
		// (2) The best-performing policy that takes the least code/memory, i.e. the simplest, best-performing
		//     approximation of the optimal ZDPolicy

		// Validation of optimal play through student implementation

		ZDPolicy approx = new AYZDPolicyMinh();

		// TODO - try out your own policies here


		ZDPolicyEvaluator evaluator = new ZDPolicyEvaluator(optimal, approx);
		double pol1p1 = evaluator.pWin1[0][0][0][0][ZombieDiceSolver.INITIAL_DICE_STATE_INDEX];
		double pol2p1 = evaluator.pWin2[0][0][0][0][ZombieDiceSolver.INITIAL_DICE_STATE_INDEX];
		double pol1p2 = 1 - pol2p1;
		double pol2p2 = 1 - pol1p1;
		double avg1 = (pol1p1 + pol1p2) / 2.0;
		double avg2 = (pol2p1 + pol2p2) / 2.0;
		double diff = avg2 - avg1;
		System.out.println("Policy 1 first player win rate: " + pol1p1);
		System.out.println("Policy 2 first player win rate: " + pol2p1);
		System.out.println("Policy 1 second player win rate: " + pol1p2);
		System.out.println("Policy 2 second player win rate: " + pol2p2);
		System.out.println("Policy 1 average win rate: " + avg1);
		System.out.println("Policy 2 average win rate: " + avg2);
		System.out.println("Average win rate difference: " + diff);
	}


}

