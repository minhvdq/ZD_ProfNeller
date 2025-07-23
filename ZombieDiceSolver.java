
/*

Rules of Zombie Dice:
Zombie Dice (ZD) is a dice game for two or more players using 13 nonstandard six-sided dice described in the table below:

Color	Number	Brain	Shotgun	Footprint
        of Dice	Sides	Sides	Sides
Green	6		3		1		2 
Yellow	4		2		2		2
Red		3		1		3		2 

A _turn_ consists of a sequence of player dice rolls where rolled brains and shotguns are set aside.  The turn ends when, after rolling, the player either decides to _hold_ (i.e. stop rolling) and score the total number of brains rolled, or has rolled three or more shotguns, ending the turn and scoring 0 points.  

At the beginning of the turn, three of the supply of 13 dice are drawn at random and rolled.  Any rolled brains and shotguns are then set aside.  If 3 or more shotguns are set aside, the turn ends scoring 0 points. Otherwise, the player chooses either to hold, scoring the number of brains and ending their turn, or to roll again.  To roll again, dice are drawn at random and added to any rolled footprints until there are three dice to roll.  (If there are no dice to draw, keep track of the number of brains set aside, and add all rolled brain dice back into the dice supply, and continue drawing dice at random.)  Then those three dice are rolled, any brains and shotguns are again set aside, and we repeat the process described above.

A _round_ consists of each player taking one turn in sequence.  When a round ends with any player having 13 or more points, a player having the most points wins.  If two or more players are tied with the most points (13 or more), another round is played between those players only. 
 In this paper, we will focus on the two-player ZD game, so we can say that a 2-player game ends when a round concludes with a single winner having the most points with 13 or more points.

The game thus consists of roll/hold risk assessments in a race to achieve a unique top score of 13 or more points, playing additional tie-breaker rounds with tied leaders as necessary.  Given the player scores, the numbers of colored brain and shotgun dice set aside, and the current locations of non-shotgun dice of different colors, should the current player roll or hold so as to maximize the probability of winning?

State Representation:

The _game state_ consists of the _dice state_, the turn total, the current player score, the opponent score, and the current player.  
The _dice state_ specified where all dice are located.  Given the three colors and four locations (set aside as shotguns, awaiting possible reroll as footprints, in the supply, and set aside as brains), there are 3 * 4 = 12 variables.
However, we can leave the rolled brains set aside as implicit from the other dice state variables, using only 9 variables to represent the dice state.

Let such variable index constants be denoted as follows with the potential values for each variable in parentheses:
SHOTGUN_GREEN (0-2), SHOTGUN_YELLOW (0-2), SHOTGUN_RED (0-2), FOOTPRINT_GREEN (0-3), FOOTPRINT_YELLOW (0-3), FOOTPRINT_RED (0-3), SUPPLY_GREEN (0-6), SUPPLY_YELLOW (0-4), SUPPLY_RED (0-3).

Bit-packing:

The dice state can be bit-packed into a single 20-bit integer to facilitate fast data structure lookups.  The bit-packing is done as follows:
SHOTGUN_GREEN (0-2)  -> bits 0-1
SHOTGUN_YELLOW (0-2) -> bits 2-3
SHOTGUN_RED (0-2)    -> bits 4-5
FOOTPRINT_GREEN (0-3) -> bits 6-7
FOOTPRINT_YELLOW (0-3) -> bits 8-9
FOOTPRINT_RED (0-3) -> bits 10-11
SUPPLY_GREEN (0-6) -> bits 12-14
SUPPLY_YELLOW (0-4) -> bits 15-17
SUPPLY_RED (0-3) -> bits 18-19

Dice roll transitions:

When a player holds, the result is deterministic: the turn total is added to the player's score, the dice state is reset to the initial state, the turn total is reset to 0, and the player changes. 

However, when a player rolls, there are a potentially large number of next dice states, changes to the turn total, and a potential for the turn to end as if holding but with no score gain.

It seems that it is worth precomputing a dice roll transition data structure that maps the current dice state integer to a list of tuples, each containing:
(1) probability of the transition, (2) next dice state integer (or -1 if the turn ends with no score), and (3) change to the turn total (possibly 0).

Solution Approach:

After precomputing the dice roll transition data structure, we can sort all dice state integers and use their sorted indices as a compact reference for the dice state.
We can then build a multi-dimensional arrays of size [2][MAX_SCORE][MAX_SCORE][MAX_SCORE][NUM_DICE_STATES] for holding
(1) the probabilities of winning for each player given the current game state, and
(2) the policy for each player given the current game state, where the policy is either ROLL or HOLD.

Given this representation, we can apply a value iteration algorithm to compute the probabilities of winning and the optimal policies for each player.

*/

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class ZombieDiceSolver implements ZDPolicy {

	static final int ROLL = 0, HOLD = 1; // Action constants for the player's decision to roll or hold.
	static final int NUM_PLAYERS = 2; // Number of players in the game.
	static final int NUM_DICE_GREEN = 6, NUM_DICE_YELLOW = 4, NUM_DICE_RED = 3; // Number of dice of each color.
	static final int NUM_DICE = NUM_DICE_GREEN + NUM_DICE_YELLOW + NUM_DICE_RED; // Total number of dice in the game.
	static final int GREEN_DIE = 0, YELLOW_DICE = 1, RED_DICE = 2; // Indices for the different colored dice.
	static final int BRAIN = 0, SHOTGUN = 1, FOOTPRINT = 2; // Indices for the different sides of the dice.
	static final int[][] DIE_SIDE_FREQUENCIES = {
			{3, 1, 2}, // Green die: 3 brains, 1 shotgun, 2 footprints
			{2, 2, 2}, // Yellow die: 2 brains, 2 shotguns, 2 footprints
			{1, 3, 2}  // Red die: 1 brain, 3 shotguns, 2 footprints
	}; // Sides of each die color represented as an array of [brains, shotguns, footprints].
	static final int SHOTGUN_GREEN = 0, SHOTGUN_YELLOW = 1, SHOTGUN_RED = 2, FOOTPRINT_GREEN = 3, FOOTPRINT_YELLOW = 4, FOOTPRINT_RED = 5,
			SUPPLY_GREEN = 6, SUPPLY_YELLOW = 7, SUPPLY_RED = 8; // Indices for the dice state variables representing the number of shotguns, footprints, and supplies of each color.
	static final int NUM_DICE_STATE_VARS = 9; // Number of variables in the dice state representation.
	static final int GOAL_SCORE = 13, MAX_SCORE = 2 * GOAL_SCORE; // Max score is an assumed upper bound for scores in the game, which can be adjusted as needed.
	// Note: With MAX_SCORE = 3 * GOAL_SCORE, run with "java -Xmx32g ZombieDiceSolver" to avoid OutOfMemoryError.
	static final int SHOTGUN_BUSTED = -1; // Special dice state value indicating that the player has rolled three or more shotguns and busted.
	static final int MAX_CHOOSE = 6;
	static int[][] choose = new int[MAX_CHOOSE + 1][MAX_CHOOSE + 1];

	static int[] diceStates;  // sorted array of all possible dice state integers (bit-packed representation)
	static int numDiceStates; // number of unique dice states
	static Map<Integer, Integer> diceStateIndexMap = new TreeMap<>(); // maps dice state integer to its index in the sorted array

	static int[] transitionCounts; // Number of transitions for each dice state index
	static int[][] transitionFrequencies; // Frequencies of transitions, indexed by dice state index and transition index
	static int[][] nextDiceStateIndices; // Next dice state indices for each transition, indexed by dice state index and transition index
	static int[][] changesToTurnTotal; // Changes to the turn total for each transition, indexed by dice state index and transition index
	static final int INITIAL_DICE_STATE_INTEGER = getDiceStateInteger(0, 0, 0, 0, 0, 0, 6, 4, 3);
	static final int INITIAL_DICE_STATE_INDEX; // Index of the initial dice state in the sorted array

	static {
		// Precompute binomial coefficients for efficiency
		for (int n = 0; n <= MAX_CHOOSE; n++) {
			for (int k = 0; k <= n; k++) {
				if (k == 0)
					choose[n][k] = 1;
				else if (n == 0)
					choose[n][k] = 0;
				else
					choose[n][k] = choose[n - 1][k - 1] + choose[n - 1][k];
			}
		}
		computeDiceStates();
		computeDiceStateTransitions();
		INITIAL_DICE_STATE_INDEX = diceStateIndexMap.get(INITIAL_DICE_STATE_INTEGER);
	}

	public static int getDiceStateInteger(int shotgunGreen, int shotgunYellow, int shotgunRed,
			int footprintGreen, int footprintYellow, int footprintRed,
			int supplyGreen, int supplyYellow, int supplyRed) {
		// Bit-pack the dice state into a single integer
		int diceStateInteger = shotgunGreen;
		diceStateInteger = (diceStateInteger << 2) | shotgunYellow; // bits 0-1 for SHOTGUN_GREEN, bits 2-3 for SHOTGUN_YELLOW
		diceStateInteger = (diceStateInteger << 2) | shotgunRed; // bits 4-5 for SHOTGUN_RED
		diceStateInteger = (diceStateInteger << 2) | footprintGreen; // bits 6-7 for FOOTPRINT_GREEN
		diceStateInteger = (diceStateInteger << 2) | footprintYellow; // bits 8-9 for FOOTPRINT_YELLOW
		diceStateInteger = (diceStateInteger << 2) | footprintRed; // bits 10-11 for FOOTPRINT_RED
		diceStateInteger = (diceStateInteger << 3) | supplyGreen; // bits 12-14 for SUPPLY_GREEN
		diceStateInteger = (diceStateInteger << 3) | supplyYellow; // bits 15-17 for SUPPLY_YELLOW
		diceStateInteger = (diceStateInteger << 2) | supplyRed; // bits 18-19 for SUPPLY_RED
		return diceStateInteger;
	}

	public static int[] getDiceStateArray(int diceStateInteger) {
		// Decompose the dice state integer into its components
		int[] diceStateArr = new int[NUM_DICE_STATE_VARS];
		diceStateArr[SUPPLY_RED] = diceStateInteger & 0b11; // bits 18-19
		diceStateInteger >>= 2;
		diceStateArr[SUPPLY_YELLOW] = diceStateInteger & 0b111; // bits 15-17
		diceStateInteger >>= 3;
		diceStateArr[SUPPLY_GREEN] = diceStateInteger & 0b111; // bits 12-14
		diceStateInteger >>= 3;
		diceStateArr[FOOTPRINT_RED] = diceStateInteger & 0b11; // bits 10-11
		diceStateInteger >>= 2;
		diceStateArr[FOOTPRINT_YELLOW] = diceStateInteger & 0b11; // bits 8-9
		diceStateInteger >>= 2;
		diceStateArr[FOOTPRINT_GREEN] = diceStateInteger & 0b11; // bits 6-7
		diceStateInteger >>= 2;
		diceStateArr[SHOTGUN_RED] = diceStateInteger & 0b11; // bits 4-5
		diceStateInteger >>= 2;
		diceStateArr[SHOTGUN_YELLOW] = diceStateInteger & 0b11; // bits 2-3
		diceStateInteger >>= 2;
		diceStateArr[SHOTGUN_GREEN] = diceStateInteger & 0b11; // bits 0-1
		return diceStateArr;
	}

	public static void computeDiceStates() {
		// Collect all unique dice states based on the game rules into an ArrayList.
		ArrayList<Integer> diceStateList = new ArrayList<>();
		for (int shotgunGreen = 0; shotgunGreen < 3; shotgunGreen++)
			for (int shotgunYellow = 0; shotgunYellow < 3 - shotgunGreen; shotgunYellow++)
				for (int shotgunRed = 0; shotgunRed < 3 - shotgunGreen - shotgunYellow; shotgunRed++) {
					int greenLeft = NUM_DICE_GREEN - shotgunGreen;
					int yellowLeft = NUM_DICE_YELLOW - shotgunYellow;
					int redLeft = NUM_DICE_RED - shotgunRed;
					int maxFootprintGreen = Math.min(3, greenLeft);
					for (int footprintGreen = 0; footprintGreen <= maxFootprintGreen; footprintGreen++) {
						int maxFootprintYellow = Math.min(3 - footprintGreen, yellowLeft);
						for (int footprintYellow = 0; footprintYellow <= maxFootprintYellow; footprintYellow++) {
							int maxFootprintRed = Math.min(3 - footprintGreen - footprintYellow, redLeft);
							for (int footprintRed = 0; footprintRed <= maxFootprintRed; footprintRed++) {
								int supplyGreenLeft = greenLeft - footprintGreen;
								int supplyYellowLeft = yellowLeft - footprintYellow;
								int supplyRedLeft = redLeft - footprintRed;
								for (int supplyGreen = 0; supplyGreen <= supplyGreenLeft; supplyGreen++)
									for (int supplyYellow = 0; supplyYellow <= supplyYellowLeft; supplyYellow++)
										for (int supplyRed = 0; supplyRed <= supplyRedLeft; supplyRed++) {
											int diceState = getDiceStateInteger(shotgunGreen, shotgunYellow, shotgunRed,
													footprintGreen, footprintYellow, footprintRed,
													supplyGreen, supplyYellow, supplyRed);
											diceStateList.add(diceState);
										}
							}
						}
					}
				}
		
		System.out.println("Number of unique dice states (without SHOTGUN_BUSTED): " + diceStateList.size());
		numDiceStates = diceStateList.size();
		
		// Sort the list
		Collections.sort(diceStateList);

		// Convert the list to an array and store it in diceStates
		diceStates = new int[numDiceStates];
		for (int i = 0; i < numDiceStates; i++) {
			int diceState = diceStateList.get(i);
			diceStateIndexMap.put(diceState, i); // Map the dice state to its index
			diceStates[i] = diceState; // Store the dice state in the array
		}

		transitionCounts = new int[numDiceStates]; 
		transitionFrequencies = new int[numDiceStates][];
		nextDiceStateIndices = new int[numDiceStates][];
		changesToTurnTotal = new int[numDiceStates][]; 
	}

	public static void computeDiceStateTransitions() {
		// Iterate through all possible dice states and compute the transitions.
		for (int shotgunGreen = 0; shotgunGreen < 3; shotgunGreen++)
			for (int shotgunYellow = 0; shotgunYellow < 3 - shotgunGreen; shotgunYellow++)
				for (int shotgunRed = 0; shotgunRed < 3 - shotgunGreen - shotgunYellow; shotgunRed++) {
					int greenLeft = NUM_DICE_GREEN - shotgunGreen;
					int yellowLeft = NUM_DICE_YELLOW - shotgunYellow;
					int redLeft = NUM_DICE_RED - shotgunRed;
					int maxFootprintGreen = Math.min(3, greenLeft);
					for (int footprintGreen = 0; footprintGreen <= maxFootprintGreen; footprintGreen++) {
						int maxFootprintYellow = Math.min(3 - footprintGreen, yellowLeft);
						for (int footprintYellow = 0; footprintYellow <= maxFootprintYellow; footprintYellow++) {
							int maxFootprintRed = Math.min(3 - footprintGreen - footprintYellow, redLeft);
							for (int footprintRed = 0; footprintRed <= maxFootprintRed; footprintRed++) {
								int supplyGreenLeft = greenLeft - footprintGreen;
								int supplyYellowLeft = yellowLeft - footprintYellow;
								int supplyRedLeft = redLeft - footprintRed;
								for (int supplyGreen = 0; supplyGreen <= supplyGreenLeft; supplyGreen++)
									for (int supplyYellow = 0; supplyYellow <= supplyYellowLeft; supplyYellow++)
										for (int supplyRed = 0; supplyRed <= supplyRedLeft; supplyRed++) {
											// Use helper method to compute all transitions from this dice state.
											rollTransitions(shotgunGreen, shotgunYellow, shotgunRed,
													footprintGreen, footprintYellow, footprintRed,
													supplyGreen, supplyYellow, supplyRed);


											
										}
							}
						}
					}
				}
	}


	public static void rollTransitions(int shotgunGreen, int shotgunYellow, int shotgunRed,
			int footprintGreen, int footprintYellow, int footprintRed,
			int supplyGreen, int supplyYellow, int supplyRed) {
		
		// System.out.printf("Computing transitions for dice state: shotgunGreen=%d, shotgunYellow=%d, shotgunRed=%d, footprintGreen=%d, footprintYellow=%d, footprintRed=%d, supplyGreen=%d, supplyYellow=%d, supplyRed=%d\n",
		// 		shotgunGreen, shotgunYellow, shotgunRed,
		// 		footprintGreen, footprintYellow, footprintRed,
		// 		supplyGreen, supplyYellow, supplyRed);
		class Transition {
			int frequency; // Frequency of this transition
			int nextDiceState; // Next dice state index after this transition
			int changeToTurnTotal; // Change to the turn total after this transition

			public Transition(int frequency, int nextDiceState, int changeToTurnTotal) {
				this.frequency = frequency;
				this.nextDiceState = nextDiceState;
				this.changeToTurnTotal = changeToTurnTotal;
			}
		}


		Map<Integer, Transition> transitionsMap = new TreeMap<>(); // Map to hold transitions for this dice state

		int diceState = getDiceStateInteger(shotgunGreen, shotgunYellow, shotgunRed,
													footprintGreen, footprintYellow, footprintRed,
													supplyGreen, supplyYellow, supplyRed);
		int diceStateIndex = diceStateIndexMap.get(diceState); // Get the index of this dice state
		int count = 0; // Count of transitions from this dice state

		// Dice Selection Phase

		// Move footprints to rolling dice selection
		int rollingGreen = footprintGreen;
		int rollingYellow = footprintYellow;
		int rollingRed = footprintRed;
		footprintGreen = 0;
		footprintYellow = 0;
		footprintRed = 0;

		// Draw dice from supply
		int numDrawing = 3 - rollingGreen - rollingYellow - rollingRed; // Number of dice to draw
		int numInSupply = supplyGreen + supplyYellow + supplyRed; // Total dice in supply
		// If not enough dice in supply, draw the dice that are in the supply, and put all rolled brains back into supply
		if (numDrawing > numInSupply) {
			// draw the remaining dice from supply
			rollingGreen += supplyGreen;
			numDrawing -= supplyGreen;
			supplyGreen = 0; 
			rollingYellow += supplyYellow;
			numDrawing -= supplyYellow;
			supplyYellow = 0;
			rollingRed += supplyRed;
			numDrawing -= supplyRed;
			supplyRed = 0;
			// add all rolled brains back into supply
			supplyGreen += NUM_DICE_GREEN - supplyGreen - rollingGreen - shotgunGreen;
			supplyYellow += NUM_DICE_YELLOW - supplyYellow - rollingYellow - shotgunYellow;
			supplyRed += NUM_DICE_RED - supplyRed - rollingRed - shotgunRed;
		}

		Stack<Integer> drawnDice = new Stack<>(); // Stack to hold drawn dice
		for (int i = 0; i < rollingGreen; i++) {
			drawnDice.push(GREEN_DIE); // Add rolling green dice to stack
		}	
		for (int i = 0; i < rollingYellow; i++) {
			drawnDice.push(YELLOW_DICE); // Add rolling yellow dice to stack
		}
		for (int i = 0; i < rollingRed; i++) {
			drawnDice.push(RED_DICE); // Add rolling red dice to stack
		}
		int maxGreenDrawn = Math.min(numDrawing, supplyGreen);
		for (int numGreenDrawn = 0; numGreenDrawn <= maxGreenDrawn; numGreenDrawn++) {
			for (int i = 0; i < numGreenDrawn; i++) {
				drawnDice.push(GREEN_DIE); // Add drawn green dice to stack
				supplyGreen--; // Decrease supply of green dice
				rollingGreen++; // Increment rolling green dice count
			}
			int maxYellowDrawn = Math.min(numDrawing - numGreenDrawn, supplyYellow);
			for (int numYellowDrawn = 0; numYellowDrawn <= maxYellowDrawn; numYellowDrawn++) {
				for (int i = 0; i < numYellowDrawn; i++) {
					drawnDice.push(YELLOW_DICE); // Add drawn yellow dice to stack
					supplyYellow--; // Decrease supply of green dice
					rollingYellow++; // Increment rolling yellow dice count
				}
				int numRedDrawn = numDrawing - numGreenDrawn - numYellowDrawn;
				if (numRedDrawn <= supplyRed) {
					for (int i = 0; i < numRedDrawn; i++) {
						drawnDice.push(RED_DICE); // Add drawn red dice to stack
						supplyRed--; // Decrease supply of red dice
						rollingRed++; // Increment rolling red dice count
					}
					// Compute the frequency of this draw transition
					int drawFrequency = choose[supplyGreen + numGreenDrawn][numGreenDrawn] * 
							choose[supplyYellow + numYellowDrawn][numYellowDrawn] *
							choose[supplyRed + numRedDrawn][numRedDrawn];

					// Roll Phase
					for (int i = 0; i < 27; i++) { // 3^3 = 27 possible rolls of 3 dice
						int[][] nextDiceStateArr = new int[][] {{shotgunGreen, shotgunYellow, shotgunRed}, // indexed by location (SHOTGUN, FOOTPRINT, SUPPLY) and die color (GREEN, YELLOW, RED)
															{footprintGreen, footprintYellow, footprintRed},
															{supplyGreen, supplyYellow, supplyRed}}; 
						int numBrainsRolled = 0; // Count of brains rolled
						int[] roll = new int[3]; // Array to hold the rolled die symbols
						int rollFrequency = 1; // Frequency of this roll
						int iCopy = i; // Copy of i for manipulation
						for (int j = 0; j < 3; j++) {
							int dieColor = drawnDice.get(j);
							int dieSymbol = iCopy % 3; // Determine which side is rolled
							roll[j] = dieSymbol; // Increment the die symbol count
							if (dieSymbol == BRAIN) {
								numBrainsRolled++; // Increment brains rolled
							} else if (dieSymbol == SHOTGUN) {
								nextDiceStateArr[0][dieColor]++; // Increment the footprint count for this die color
							} else if (dieSymbol == FOOTPRINT) {
								nextDiceStateArr[1][dieColor]++; // Increment the supply count for this die color
							}
							rollFrequency *= DIE_SIDE_FREQUENCIES[dieColor][dieSymbol]; // Update roll frequency based on remaining dice
							iCopy /= 3; // Move to the next die
						}

						int frequency = drawFrequency * rollFrequency; // Total frequency of this transition
						count += frequency; // Update the count of transitions
						int changeToTurnTotal = numBrainsRolled; // Change to the turn total is the number of brains rolled
						int nextDiceState = SHOTGUN_BUSTED; // Initialize next dice state index

						// Check if the player busted
						if ((nextDiceStateArr[0][0] + nextDiceStateArr[0][1] + nextDiceStateArr[0][2]) >= 3) {
							// Transition ends with no score, reset turn total and change player
							changeToTurnTotal = 0; // No score gain
						}
						else {
							nextDiceState = getDiceStateInteger(nextDiceStateArr[0][0], nextDiceStateArr[0][1], nextDiceStateArr[0][2],
									nextDiceStateArr[1][0], nextDiceStateArr[1][1], nextDiceStateArr[1][2],
									nextDiceStateArr[2][0], nextDiceStateArr[2][1], nextDiceStateArr[2][2]);
						}
						if (transitionsMap.containsKey(nextDiceState)) {
							// If this next dice state already exists, update its transition
							transitionsMap.get(nextDiceState).frequency += frequency; // Increment the frequency
						}
						else {
							// Otherwise, create a new transition
							transitionsMap.put(nextDiceState, new Transition(frequency, nextDiceState, changeToTurnTotal));
						}

					} // End of rolling phase

					for (int i = 0; i < numRedDrawn; i++) {
						drawnDice.pop(); // Pop drawn red dice from stack
						supplyRed++; // Increase supply of red dice
						rollingRed--; // Decrease rolling red dice count
					}
				}
				for (int i = 0; i < numYellowDrawn; i++) {
					drawnDice.pop(); // Pop drawn yellow dice from stack
					supplyYellow++; // Increase supply of yellow dice
					rollingYellow--; // Decrease rolling yellow dice count
				}
			}
			for (int i = 0; i < numGreenDrawn; i++) {
				drawnDice.pop(); // Pop drawn green dice from stack
				supplyGreen++; // Increase supply of green dice
				rollingGreen--; // Decrease rolling green dice count
			}
		}

		// Store the transitions for this dice state index
		transitionCounts[diceStateIndex] = count; // Store the total count of transitions for this dice state
		transitionFrequencies[diceStateIndex] = new int[transitionsMap.size()]; // Initialize the frequencies array
		nextDiceStateIndices[diceStateIndex] = new int[transitionsMap.size()]; // Initialize the next dice state indices array
		changesToTurnTotal[diceStateIndex] = new int[transitionsMap.size()]; // Initialize the changes to turn total array
		int idx = 0;
		for (int transitionDiceState : transitionsMap.keySet()) {
			Transition transition = transitionsMap.get(transitionDiceState); // Get the transition
			int transitionDiceStateIndex = transitionDiceState == SHOTGUN_BUSTED ? SHOTGUN_BUSTED : diceStateIndexMap.get(transition.nextDiceState); // Get the index of the next dice state
			transitionFrequencies[diceStateIndex][idx] = transition.frequency; // Store the frequency
			nextDiceStateIndices[diceStateIndex][idx] = transitionDiceStateIndex; // Store the next dice state index
			changesToTurnTotal[diceStateIndex][idx] = transition.changeToTurnTotal; // Store the change to turn total
			idx++;
		}
	}

	public static void printTransition(int diceStateIndex) {
		// Print the transitions for a given dice state index
		System.out.println("Transitions for dice state index " + diceStateIndex + ":");
		System.out.println("Total transitions: " + transitionCounts[diceStateIndex]);
		for (int i = 0; i < transitionFrequencies[diceStateIndex].length; i++) {
			int nextDiceStateIndex = nextDiceStateIndices[diceStateIndex][i];
			int changeToTurnTotal = changesToTurnTotal[diceStateIndex][i];
			System.out.printf("Transition %d: Frequency=%d, Change to Turn Total=%d\n",
					i, transitionFrequencies[diceStateIndex][i], changeToTurnTotal);
			printDiceState(nextDiceStateIndex);
		}
	}	

	public static void printDiceState(int diceStateIndex) {
		if (diceStateIndex == SHOTGUN_BUSTED) {
			System.out.println("SHOTGUN_BUSTED");
			return;
		}	
		int diceState = diceStates[diceStateIndex];
		int[] diceStateArr = getDiceStateArray(diceState);
		System.out.printf("Dice State Index: %d, Dice State: %d\n", diceStateIndex, diceState);
		System.out.print("\tG\tY\tR\tTOTAL\n");
		System.out.printf("S\t%d\t%d\t%d\t%d\n", 
				diceStateArr[SHOTGUN_GREEN], diceStateArr[SHOTGUN_YELLOW], diceStateArr[SHOTGUN_RED],
				diceStateArr[SHOTGUN_GREEN] + diceStateArr[SHOTGUN_YELLOW] + diceStateArr[SHOTGUN_RED]);
		System.out.printf("F\t%d\t%d\t%d\t%d\n",
				diceStateArr[FOOTPRINT_GREEN], diceStateArr[FOOTPRINT_YELLOW], diceStateArr[FOOTPRINT_RED],
				diceStateArr[FOOTPRINT_GREEN] + diceStateArr[FOOTPRINT_YELLOW] + diceStateArr[FOOTPRINT_RED]);
		System.out.printf("C\t%d\t%d\t%d\t%d\n",
				diceStateArr[SUPPLY_GREEN], diceStateArr[SUPPLY_YELLOW], diceStateArr[SUPPLY_RED],
				diceStateArr[SUPPLY_GREEN] + diceStateArr[SUPPLY_YELLOW] + diceStateArr[SUPPLY_RED]);
		int brainsGreen = NUM_DICE_GREEN - diceStateArr[SHOTGUN_GREEN] - diceStateArr[FOOTPRINT_GREEN] - diceStateArr[SUPPLY_GREEN];
		int brainsYellow = NUM_DICE_YELLOW - diceStateArr[SHOTGUN_YELLOW] - diceStateArr[FOOTPRINT_YELLOW] - diceStateArr[SUPPLY_YELLOW];
		int brainsRed = NUM_DICE_RED - diceStateArr[SHOTGUN_RED] - diceStateArr[FOOTPRINT_RED] - diceStateArr[SUPPLY_RED];
		System.out.printf("B\t%d\t%d\t%d\t%d\n",
				brainsGreen, brainsYellow, brainsRed,
				brainsGreen + brainsYellow + brainsRed);
	}

	double epsilon; // Default epsilon for convergence in value iteration, can override via constructor
	boolean[][][][][] shouldRoll; // optimal policy shouldRoll[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex]
	double[][][][][] pWin; // probability of winning pWin[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex]

	public ZombieDiceSolver() {
		this(1e-14); // Default constructor with a small epsilon for convergence
	}	
	
	public ZombieDiceSolver(double epsilon) {
		this.epsilon = epsilon; // Set the convergence threshold for value iteration
		shouldRoll = new boolean[NUM_PLAYERS][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][numDiceStates];
		pWin = new double[NUM_PLAYERS][MAX_SCORE + 1][MAX_SCORE + 1][MAX_SCORE + 1][numDiceStates];
		loadOrSolve();
	}

	public void loadOrSolve() {
		String filename = String.format("zd_solution_goal%d_max%d_eps%.0e.dat", GOAL_SCORE, MAX_SCORE, epsilon);
		File file = new File(filename);
		// If the file exists, load shouldRoll and pWin from the file.
		try {
			if (file.exists()) {
				System.out.println("Loading solution from file: " + filename);
				// Create an ObjectInputStream to read the solution from the file
				java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(file));
				// Read the shouldRoll and pWin arrays from the file
				shouldRoll = (boolean[][][][][]) ois.readObject();
				pWin = (double[][][][][]) ois.readObject();
				ois.close(); // Close the ObjectInputStream
			} else {
				System.out.println("File " + filename + " not found, solving Zombie Dice...");
				solve(); // Solve the game if the file does not exist
				// Save the solution to the file
				java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file));
				oos.writeObject(shouldRoll); // Write the shouldRoll array to the file	
				oos.writeObject(pWin); // Write the pWin array to the file
				oos.close(); // Close the ObjectOutputStream
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void solve() {
		double maxChange; // Maximum change in winning probabilities for convergence
		do {
			maxChange = 0; // Reset the maximum change for this iteration
			int[] maxChangeState = new int[5]; // To track the state with the maximum change
			double newPWin = 0;
			for (int distToMaxScore = 0; distToMaxScore <= 2 * MAX_SCORE; distToMaxScore++) {
				System.out.println("Distance to max score: " + distToMaxScore);
				int minCurrentPlayerScore = Math.max(0, GOAL_SCORE - distToMaxScore);
				for (int currentPlayerScore = MAX_SCORE; currentPlayerScore >= minCurrentPlayerScore; currentPlayerScore--) {
					int minOpponentScore = Math.max(0, GOAL_SCORE - currentPlayerScore - distToMaxScore);
					for (int opponentScore = MAX_SCORE; opponentScore >= minOpponentScore; opponentScore--) {
						for (int turnTotal = MAX_SCORE - currentPlayerScore; turnTotal >= 0; turnTotal--) {
							for (int player = 0; player < NUM_PLAYERS; player++) {
								for (int diceStateIndex = 0; diceStateIndex < numDiceStates; diceStateIndex++) {
									// Initialize the winning probabilities for this state
									double pWinPrevious = pWin[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex];

									// Compute the winning probabilities for rolling
									double pWinRoll = 0;
									int transitionCount = transitionCounts[diceStateIndex];
									int numTransitions = transitionFrequencies[diceStateIndex].length;
									for (int i = 0; i < numTransitions; i++) {
										int nextDiceStateIndex = nextDiceStateIndices[diceStateIndex][i];
										int changeToTurnTotal = changesToTurnTotal[diceStateIndex][i];
										double transitionProbability = (double) transitionFrequencies[diceStateIndex][i] / transitionCount;
										if (nextDiceStateIndex == SHOTGUN_BUSTED) {
											// If busted, no score gain, reset turn total and change player
											pWinRoll += transitionProbability * (1 - pWin(1 - player, opponentScore, currentPlayerScore, 0, INITIAL_DICE_STATE_INDEX));
										} else {
											// Otherwise, update the winning probability based on the new state
											pWinRoll += transitionProbability * pWin(player, currentPlayerScore, opponentScore, turnTotal + changeToTurnTotal, nextDiceStateIndex);
										}
									}
									if (pWinRoll > 1.0) {
										pWinRoll = 1.0; // Correct for floating point errors adding to more than 1.0
									} 

									// Compute the winning probabilities for holding
									double pWinHold = 1 - pWin(1 - player, opponentScore, currentPlayerScore + turnTotal, 0, INITIAL_DICE_STATE_INDEX);	

									// Determine the best action (roll or hold)
									boolean rolling = pWinRoll > pWinHold;
									double pWinMax = rolling ? pWinRoll : pWinHold;
									shouldRoll[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] = rolling;
									pWin[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex] = pWinMax;
									double change = Math.abs(pWinMax - pWinPrevious);
									if (change > maxChange) {
										maxChange = change; // Update the maximum change
										maxChangeState[0] = player;
										maxChangeState[1] = currentPlayerScore;
										maxChangeState[2] = opponentScore;
										maxChangeState[3] = turnTotal;
										maxChangeState[4] = diceStateIndex; // Store the state with the maximum change
										newPWin = pWinMax; // Store the new winning probability for this state
									}
								} // End of dice state loop
							} // End of player loop
						} // End of turn total loop
					} // End of score2 loop
				} // End of score1 loop
			} // End of distance to max score loop
			System.out.println("Max change: " + maxChange + " for state: " + Arrays.toString(maxChangeState) + " with new pWin: " + newPWin);
		} while (maxChange > epsilon); // Continue until convergence
		System.out.println("Win probability for player 0 with score 0, opponent score 0, turn total 0, and initial dice state: " +
				pWin(0, 0, 0, 0, INITIAL_DICE_STATE_INDEX));
	}

	public double pWin(int player, int currentPlayerScore, int opponentScore, int turnTotal, int diceStateIndex) {
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
		return pWin[player][currentPlayerScore][opponentScore][turnTotal][diceStateIndex];
	}

	public boolean willRoll(int p, int i, int j, int b, int sg, int sy, int sr,
	                 int fg, int fy, int fr, int cg, int cy, int cr) {
		// Ensure the scores and turn total are within bounds
		if (i > MAX_SCORE)
			i = MAX_SCORE; 
		if (j > MAX_SCORE)
			j = MAX_SCORE; 
		if (b > MAX_SCORE - i)
			b = MAX_SCORE - i; 
		int diceState = getDiceStateInteger(sg, sy, sr, fg, fy, fr, cg, cy, cr);
		// System.out.printf("sg%d sy%d sr%d fg%d fy%d fr%d cg%d cy%d cr%d\n",
		// 		sg, sy, sr, fg, fy, fr, cg, cy, cr);
		int diceStateIndex = diceStateIndexMap.get(diceState);
		// Return whether the player should roll based on the computed policy
		return shouldRoll[p][i][j][b][diceStateIndex];
	}
	
	public static String diceStateIndexToString(int diceStateIndex) {
		int diceState = diceStates[diceStateIndex];
		int[] diceStateArr = getDiceStateArray(diceState);
		return String.format("sg%d sy%d sr%d fg%d fy%d fr%d cg%d cy%d cr%d",
				diceStateArr[SHOTGUN_GREEN], diceStateArr[SHOTGUN_YELLOW], diceStateArr[SHOTGUN_RED],
				diceStateArr[FOOTPRINT_GREEN], diceStateArr[FOOTPRINT_YELLOW], diceStateArr[FOOTPRINT_RED],
				diceStateArr[SUPPLY_GREEN], diceStateArr[SUPPLY_YELLOW], diceStateArr[SUPPLY_RED]);
	}

	public static void printDiceStates() {
		// Print all unique dice states
	//	System.out.println("Unique Dice States:");
		for (int i = 0; i < numDiceStates; i++) {
			System.out.println(diceStateIndexToString(i));
		}
	}

	public static void main(String[] args) {
		// printTransition(0); // Print transitions for the first dice state as an example
		//printDiceStates(); // Print all dice states
		new ZombieDiceSolver(1e-14);
	}

}
