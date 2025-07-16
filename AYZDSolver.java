import java.awt.Color;
import java.util.Formatter;

/**
 * AYZDSolver - computes optimal play for the All Yellow Zombie Dice game adapted from Zombie Dice 
 * (2010; https://boardgamegeek.com/boardgame/62871/zombie-dice). 
 * 
 * 2-Player All Yellow Zombie Dice Rules:
 * Materials: 3 x d6 Dice with faces showing 2 brains, 2 footprints, and 2 shotguns
 * 
 * At the beginning of a player's turn the turn total and number of shotguns rolled are 0.
 * The player rolls 3 yellow zombie dice.
 * - Each brain rolled adds 1 to the turn total, initially 0.
 * - Each shotgun rolled adds 1 to the number of shotguns rolled.
 * 
 * If the number of shotguns rolled this turn is 3 or more, the turn is over with no score change.
 * Otherwise, the player decides whether to roll again or hold.
 * If the player holds, the turn ends and the turn total is added to the player's score.
 * If the player rolls again, the player repeats the rolling of 3 dice above.
 * 
 * A round consists of each player taking a turn.
 * At the end of a round where a player's score is 13 or more, and is not tied, that player wins.
 * Otherwise, another round is played.
 * 
 * First version written and computed May 22-23, 2024.
 * 
 * @author Todd W. Neller
 */
public class AYZDSolver implements AYZDPolicy {

	final int ROLL = 1, HOLD = 0, GOAL = 13, NUM_DICE = 3, MAX_SCORE = 3 * GOAL; 
	double epsilon; // value iteration convergence threshold
	double[][][][][] pWin; // indexed by p player, i score, j opponent score, b turn total (brains rolls), s shotguns rolled
	boolean[][][][][] roll; // indexed by p player, i score, j opponent score, b turn total (brains rolls), s shotguns rolled
	boolean[][][][][] isReachable; // indexed by p player, i score, j opponent score, b turn total (brains rolls), s shotguns rolled

	double[][] pRollOutcome; // indexed by b brains rolled, s shotguns rolled

	public AYZDSolver() {
		this(1e-14);
	}

	public AYZDSolver(double epsilon) {
		this.epsilon = epsilon;
		System.out.printf("Solving epsilon = %.1e ...\n", epsilon);
		init();
		fastSolve();
		System.out.println("Solved.");
	}

	private void init() {
		pWin = new double[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][3]; // indexed by player, score, opponent score, turn total, shotguns
		roll = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][3];
		
		pRollOutcome = new double[NUM_DICE + 1][];
		for (int brains = 0; brains <= NUM_DICE; brains++) {
			pRollOutcome[brains] = new double[NUM_DICE - brains + 1];
			for (int shotguns = 0; shotguns <= NUM_DICE - brains; shotguns++) {
				int footsteps = NUM_DICE - brains - shotguns;
				// int numRollOutcomes = choose(NUM_DICE, brains) * choose(NUM_DICE - brains, footsteps);
				// double prob = (double) numRollOutcomes / Math.pow(3, NUM_DICE);
				pRollOutcome[brains][shotguns] = RollOutcomeProbabilities.probBFS[brains][footsteps][shotguns];
			}
		}
//		System.out.println("Probability of new brains (row) by number of shotguns (column), 0-based:");
//		System.out.println(Arrays.deepToString(pRollOutcome));
	}


	private void fastSolve() {
		double maxChange;
		do {
			maxChange = 0.0;
			for (int p = 0; p < 2; p++) // for each player 0/1
				for (int s = 2; s >= 0; s--) // for each non-turn-ending total shotguns 0/1/2
					for (int totalScore = 2 * MAX_SCORE; totalScore >= 0; totalScore--) // for each possible total score
						for (int i = MAX_SCORE; i >= 0; i--) {
							int j = totalScore - i;
							if (j < 0 || j > MAX_SCORE) continue;
							for (int b = MAX_SCORE - i; b >= 0; b--) {
								// If the state shouldn't be reachable, skip.
								if ((i >= GOAL && j < i) || (p == 1 && i + b >= GOAL && i + b > j)) { // already won or hold-to-win
									pWin[p][i][j][b][s] = 1.0;
									roll[p][i][j][b][s] = false;
									continue;
								}
								if (p == 0 && j >= GOAL && i < j) { // already lost
									pWin[p][i][j][b][s] = 0.0;
									roll[p][i][j][b][s] = false;
									continue;
								}
//								System.out.printf("%d %d %d\n", i, j, b);
								double oldPWin = pWin[p][i][j][b][s];

								double pWinRoll = 0;
								for (int newBrains = 0; newBrains <= NUM_DICE; newBrains++) {
									for (int newShotguns = 0; newShotguns <= NUM_DICE - newBrains; newShotguns++) {
										double pRoll = pRollOutcome[newBrains][newShotguns];
										if (s + newShotguns > 2) { 
											// Total shotguns have exceeded 2, so we use the 1 - the probability of the opponent winning
											// starting in the next turn.
											pWinRoll += pRoll * (1 - pWin(1 - p, j, i, 0, 0));
										}
										else {
											// Total shotguns have not exceeded 2, so we use the probability of the player winning.
											pWinRoll += pRoll * pWin(p, i, j, b + newBrains, s + newShotguns);
										}
									}
								}

								double pWinHold = 1 - pWin(1 - p, j, i + b, 0, 0);
//								if (i == 13 && j == 0 && s == 0 && p == 1 && b == 0) {
//									System.out.println(pWinRoll + "\t" + pWinHold);
//								}
								roll[p][i][j][b][s] = pWinRoll > pWinHold;
								pWin[p][i][j][b][s] = pWinRoll > pWinHold ? pWinRoll : pWinHold;

								double change = Math.abs(oldPWin - pWin[p][i][j][b][s]);
								maxChange = maxChange > change ? maxChange : change;
							}
						}
			// System.out.println(maxChange);
		} while (maxChange > epsilon);
	}

	public double pWin(int p, int i, int j, int b, int s) {
		// truncate state variables that would exceed the max score
		if (i > MAX_SCORE) i = MAX_SCORE;
		if (j > MAX_SCORE) j = MAX_SCORE;
		if (i + b > MAX_SCORE) b = MAX_SCORE - i;
		// It is expected that p will be 0 or 1, and s will be 0, 1, or 2.

		/* First we check for game-end conditions:
		 * - If player 0 has met or exceeded the goal score and has a higher score than player 1 
		 *   at the beginning of their turn (b == 0), player 0 wins.
		 * - If player 0 has not met the goal, but player 1 previously did, then player 0 loses.
		 * - If player 1 has a score and turn total sum that is greater than or equal to the goal score 
		 *   and is greater than player 0's score, player 1 wins.
		 * - If player 0 and player 1 have the maximum possible score, player 1 wins. 
		 * (The max score should be set to a value well beyond the goal score such that this would be a suboptimal state to reach.)
		 */
		if (p == 0 && i < GOAL && j >= GOAL)
			return 0.0;
		if (p == 0 && i >= GOAL && i > j && b == 0) // game end; note not i + b > goal; must have already held
			return 1.0;
		if (p == 1 && i + b >= GOAL && i + b > j)
			return 1.0;
		if (i == MAX_SCORE && j == MAX_SCORE)
			return 0.5; //p == 1 ? 1.0 : 0.0;

		// If the game is not over, return the scored probability of winning.
		return pWin[p][i][j][b][s];
	}

	@Override
	public boolean willRoll(int p, int i, int j, int b, int s) {
		// truncate state variables that would exceed the max score
		if (i > MAX_SCORE) i = MAX_SCORE;
		if (j > MAX_SCORE) j = MAX_SCORE;
		if (i + b > MAX_SCORE) b = MAX_SCORE - i;
		// It is expected that p will be 0 or 1, and s will be 0, 1, or 2.

		// If the game is not over, return the scored boolean value of whether or not to roll.
		return roll[p][i][j][b][s];
	}

	public void printMinHoldValues() {
		System.out.println("\nMinimum hold values:");
		for (int p = 0; p < 2; p++) 
			for (int s = 0; s < 3; s++) {
//				System.out.printf("\nPlayer %d, %d shotguns:\n i\\j", p, s);
				System.out.printf("\np%ds%d:\ni\\j,", p, s);
				for (int j = 0; j < MAX_SCORE; j++) 
					System.out.printf("%4d,", j);
				System.out.println();
				for (int i = 0; i < MAX_SCORE; i++) {
					System.out.printf("%4d,", i);
					for (int j = 0; j < MAX_SCORE; j++) {
						int b = 0;
						while (b + 1 < MAX_SCORE && roll[p][i][j][b][s])
							b++;
						System.out.printf("%4d,", b);
					}
					System.out.println();
				}
				System.out.println();
			}
	}

	public void printTurnStartWinProbs() {
		System.out.println("\nTurn start win probabilities:");
		for (int p = 0; p < 2; p++)  {
			System.out.printf("\nPlayer %d\n i\\j", p);
			for (int j = 0; j < MAX_SCORE; j++) 
				System.out.printf("%5d", j);
			System.out.println();
			for (int i = 0; i < MAX_SCORE; i++) {
				System.out.printf("%4d", i);
				for (int j = 0; j < MAX_SCORE; j++) {
					System.out.printf(" %4.2f", pWin[p][i][j][0][0]);
				}
				System.out.println();
			}
		}
	}

	public boolean checkForInclusion() { // Are the states one will roll in a subset of those one will roll in with one fewer shotgun?
		for (int p = 0; p < 2; p++) 
			for (int i = 0; i <= MAX_SCORE; i++)
				for (int j = 0; j <= MAX_SCORE; j++)
					for (int b = 0; b <= MAX_SCORE - i; b++) 
						if ((roll[p][i][j][b][2] && !roll[p][i][j][b][1]) || (roll[p][i][j][b][1] && !roll[p][i][j][b][0])) {
							System.out.printf("%d %d %d %d 0 %b 1 %b 2 %b\n", p, i, j, b, roll[p][i][j][b][0], roll[p][i][j][b][1], roll[p][i][j][b][2]);
							return false;
						}
		return true;
	}

	public void generateVRML() {
		Color[] colors = {Color.GRAY, Color.YELLOW, Color.RED};
		float[][] rgbColors = new float[colors.length][];
		for (int i = 0; i < colors.length; i++)
			rgbColors[i] = colors[i].getRGBColorComponents(null);
		double[] transparencies = {.9, .9, 0};
		for (int p = 0; p < 2; p++) {
			try {
				int count = 0;
				Formatter out = new Formatter(String.format("roll_p%d.wrl", p + 1));
				out.format("#VRML V2.0 utf8\n");
				out.format("Group { children [\n");
				for (int i = 0; i <= MAX_SCORE; i++)
					for (int j = 0; j <= MAX_SCORE; j++)
						for (int b = 0; b <= MAX_SCORE - i; b++) {
							int s = -1;
							while (s < 2 && roll[p][i][j][b][s + 1])
								s++;
							if (s >= 0) {
								out.format("DEF box%d Transform {\n", count++);
								out.format("  translation %d %d %d\n", i, j, b);
								out.format("  children [ Shape { appearance Appearance { material Material { diffuseColor %f %f %f transparency %f } } geometry Box { size 1 1 1 } } ] }\n", 
										rgbColors[s][0], rgbColors[s][1], rgbColors[s][2], transparencies[s]);
							}
						}
				out.format("] }\n");
				out.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	int maxReachableScore = 0;

	/* computeReachability - given a starting state (p, i, j, b, s), which states are reachable if both players play optimally? */
	public void computeReachability(int p, int i, int j, int b, int s) { 
		isReachable = new boolean[2][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][3];
		maxReachableScore = 0;
		reach(p, i, j, b, s);
	}
	
	private void reach(int p, int i, int j, int b, int s) { 
		// truncate state variables that would exceed the max score
		if (i > MAX_SCORE) i = MAX_SCORE;
		if (j > MAX_SCORE) j = MAX_SCORE;
		if (i + b > MAX_SCORE) b = MAX_SCORE - i;
		// It is expected that p will be 0 or 1, and s will be 0, 1, or 2.

//		try { ## commented try-catch lines here are for tracing how a state may be reached
		if (isReachable[p][i][j][b][s])
			return;
//		if (p == 0 && i == 6 && j == 0 && b == 0 && s == 0)  # for try-catch testing of state sequences
//			throw new RuntimeException("Reach query");
		isReachable[p][i][j][b][s] = true;
		if (i + b > maxReachableScore)
			maxReachableScore = i + b;
		
		if (roll[p][i][j][b][s]) { // roll
			reach(1 - p, j, i, 0, 0); // 3 or more shotguns
			for (int brains = 0; brains <= NUM_DICE; brains++) {
				for (int newShotguns = 0; newShotguns <= NUM_DICE - brains; newShotguns++) {
					if (s + newShotguns > 2) // 3 or more shotguns
						break; // already handled
					reach(p, i, j, b + brains, s + newShotguns);
				}
			}
		}	
		else { // hold
			reach(1 - p, j, i + b, 0, 0);
		}

//		}
//		catch (RuntimeException e) {
//			System.out.printf("%d %d %d %d %d\n", p, i, j, b, s);
//			throw e;
//		}
	}
	
	public static void exportCSV(AYZDSolver solver) {
		try {
			Formatter out = new Formatter(String.format("AYZD-optimal-maxscore%d.csv", solver.MAX_SCORE));
			out.format("p,i,j,b,s,roll,pWin,reachable\n");
			for (int p = 0; p < 2; p++) 
				for (int i = 0; i <= solver.MAX_SCORE; i++)
					for (int j = 0; j <= solver.MAX_SCORE; j++)
						for (int b = 0; b <= solver.MAX_SCORE - i; b++) 
							for (int s = 0; s < 3; s++) 
								out.format("%d,%d,%d,%d,%d,%d,%f,%d\n", p, i, j, b, s, 
									solver.roll[p][i][j][b][s] ? 1 : 0, 
									solver.pWin(p, i, j, b, s),
									solver.isReachable[p][i][j][b][s] ? 1 : 0);
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		long startMS = System.currentTimeMillis();
		AYZDSolver solver = new AYZDSolver(); 
		System.out.println("Solved in " + (System.currentTimeMillis() - startMS) + " ms.");
		System.out.println("pWin(0, 0, 0, 0, 0) = " + solver.pWin(0, 0, 0, 0, 0)); // 0.47013462795223776 5/23/24

		 solver.printMinHoldValues();
		// solver.printTurnStartWinProbs();
		// System.out.println(solver.checkForInclusion()); // true

		// Fairest komi i=1: pWin=0.528875 (pWin - .5 = 0.028875)
		// int fairestI = 0;
		// double fairestPWin = 0.0;
		// double minDistFromFair = Double.MAX_VALUE;
		// for (int i = 0; i < 3; i++) {
		// 	double pWin = solver.pWin(0, i, 0, 0, 0);
		// 	double distFromFair = Math.abs(pWin - 0.5);
		// 	if (distFromFair < minDistFromFair) {
		// 		minDistFromFair = distFromFair;
		// 		fairestI = i;
		// 		fairestPWin = pWin;
		// 	}
		// 	System.out.printf("i=%d: pWin=%f (pWin - .5 = %f)\n", i, pWin, pWin - .5);
		// }
		// System.out.printf("Fairest komi i=%d: pWin=%f (pWin - .5 = %f)\n", fairestI, fairestPWin, fairestPWin - .5);
		
		/*
i=0: pWin=0.470135 (pWin - .5 = -0.029865)
i=1: pWin=0.528875 (pWin - .5 = 0.028875)
i=2: pWin=0.589345 (pWin - .5 = 0.089345)
Fairest komi i=1: pWin=0.528875 (pWin - .5 = 0.028875)
		 */
		// Note: The fairest komi is similarly unfair in the opposite direction for player 1.
		// Is is better to simply recommend flipping a coin to determine who goes first.
		
		// solver.generateVRML();
		
		// Uncomment for reachability computataion (isReachable[][][][][])
		// ... with default initial state:
		solver.computeReachability(0, 0, 0, 0, 0);
		// ... with first player komi i = 1:
		// solver.computeReachability(1, 0, 0, 0, 0);

		// System.out.println("Max reachable score: " + solver.maxReachableScore); // MAX_SCORE
		/* What I conjecture we will always see here is MAX_SCORE because:
		   Players could conceivably tie after every round where a player was at or above the GOAL.
		   However much player 1 would push for in the next round, player 2 could match it with enough 
		     shotguns to recommend holding for a chance to exceed in the next round.
		   This could continue indefinitely, although it would be highly unlikely.

		   This could be a mathematical side excursion: Could we identify a case scenario where,
		   (1) both players finish a round rationally with a tie at 13 or more, and
		   (2) proceed to a greater tie?
		   If so, we prove that players could rationally tie indefinitely with rational play to 
		   an unbounded score. 
		*/

		exportCSV(solver);
//		solver.printMinHoldValues();
//		for (int i = 0; i < solver.MAX_SCORE; i++) {
//			System.out.print(i + ": ");
//			for (int b = 0; b < 8; b++) {
//				System.out.printf("\t%6.4f(%s) ", solver.pWin[1][i][0][b][0], solver.roll[1][i][0][b][0] ? "R":"h");
//			}
//			System.out.println();
//		}
	}
	

}
