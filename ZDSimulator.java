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

*/

import java.util.ArrayList;
import java.util.Collections;

public class ZDSimulator {
    static boolean verbose = true; // If true, print verbose output for debugging.
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
    static final int [][] DIE_SIDES = {
            {BRAIN, BRAIN, BRAIN, SHOTGUN, FOOTPRINT, FOOTPRINT}, // Green die sides
            {BRAIN, BRAIN, SHOTGUN, SHOTGUN, FOOTPRINT, FOOTPRINT}, // Yellow die sides
            {BRAIN, SHOTGUN, SHOTGUN, SHOTGUN, FOOTPRINT, FOOTPRINT}  // Red die sides
    }; // Sides of each die color represented as an array of indices for the sides.
	static final int SHOTGUN_GREEN = 0, SHOTGUN_YELLOW = 1, SHOTGUN_RED = 2, FOOTPRINT_GREEN = 3, FOOTPRINT_YELLOW = 4, FOOTPRINT_RED = 5,
			SUPPLY_GREEN = 6, SUPPLY_YELLOW = 7, SUPPLY_RED = 8; // Indices for the dice state variables representing the number of shotguns, footprints, and supplies of each color.
	static final int NUM_DICE_STATE_VARS = 9; // Number of variables in the dice state representation.
	static final int GOAL_SCORE = ZombieDiceSolver.GOAL_SCORE, MAX_SCORE = ZombieDiceSolver.MAX_SCORE; // Max score is an assumed upper bound for scores in the game, which can be adjusted as needed.

    ZDPolicy[] players; // Array of players in the game, each implementing a ZDPolicy.
    int currentPlayer; // Index of the current player.
    int[] playerScores; // Array to hold the scores of each player.
    int turnTotal; // Total score for the current turn.
    int[] diceState; // Array representing the state of the dice in the game.

    /**
     * Constructor to initialize the ZDSimulator with the given players.
     * @param player1 The first player implementing ZDPolicy.
     * @param player2 The second player implementing ZDPolicy.
     */
    public ZDSimulator(ZDPolicy player1, ZDPolicy player2) {
            players = new ZDPolicy[NUM_PLAYERS];
            players[0] = player1;
            players[1] = player2;
    }

    public int simulateGame() {
        // Initialize game state
        currentPlayer = 0;
        playerScores = new int[NUM_PLAYERS];
        initializeTurn(); // Reset the turn state for the current player

        // Main game loop
        while (true) { // while taking turns
            boolean startOfTurn = true; // Flag to indicate the start of a new turn

            while (true) { // while rolling on a turn
                if (verbose) printState(); 
                if (playerScores[0] >= MAX_SCORE || playerScores[1] >= MAX_SCORE) {
                    if (verbose) System.out.println("WARNING: Player score reached MAX_SCORE");
                }

                if (startOfTurn || isRolling()) { // roll
                    if (verbose) {
                        System.out.println("ROLL");
                    }
                    startOfTurn = false; // Reset the flag after initialization
                
                    drawAndRollDice();

                    // Check if the player has rolled three shotguns
                    if (diceState[SHOTGUN_GREEN] + diceState[SHOTGUN_YELLOW] + diceState[SHOTGUN_RED] >= 3) {
                        // Player rolled three shotguns, end turn with zero points
                        if (verbose) System.out.println("SHOTGUNNED!");
                        // Switch to the next player
                        currentPlayer = (currentPlayer + 1) % NUM_PLAYERS;
                        initializeTurn(); // Reset the turn state for the next player
                        if (currentPlayer == 0 && playerScores[0] >= GOAL_SCORE)  { // player 0 wins
                            if (verbose) {
                                printState();
                                System.out.println("Player 1 wins!");
                            }
                            return 0;
                        }
                        break;
                    }

                    // Limit the turn total to MAX_SCORE to prevent out of bounds issues
                    if (turnTotal > MAX_SCORE - playerScores[currentPlayer]) {
                        turnTotal = MAX_SCORE - playerScores[currentPlayer];
                    }
                } // end roll
                else { // hold
                    if (verbose) {
                        System.out.println("HOLD");
                    }
                    // Player decides to hold, score the turn total
                    playerScores[currentPlayer] += turnTotal;
                    // Switch to the next player
                    currentPlayer = (currentPlayer + 1) % NUM_PLAYERS;
                    initializeTurn(); // Reset the turn state for the next player
                    if (currentPlayer == 0) {
                        if (playerScores[0] >= GOAL_SCORE && playerScores[0] > playerScores[1]) {  // player 0 wins
                            if (verbose) {
                                printState();
                                System.out.println("Player 1 wins!");
                            }
                            return 0;
                        }
                        else if (playerScores[1] >= GOAL_SCORE 
                                && (playerScores[1] > playerScores[0] || playerScores[1] == MAX_SCORE)) { // player 1 wins
                            if (verbose) {
                                printState();
                                System.out.println("Player 2 wins!");
                            }
                            return 1;
                        }
                    }
                    break; // End the turn
                } // end hold
            } // end while rolling on a turn
        } // end while taking turns
    }

    public void drawAndRollDice() {
        // Draw three dice from the supply

        // "Draw" footprints first
        int[] drawnDice = new int[] {diceState[FOOTPRINT_GREEN], diceState[FOOTPRINT_YELLOW], diceState[FOOTPRINT_RED]}; // indexed by color (0=green, 1=yellow, 2=red)
        diceState[FOOTPRINT_GREEN] = 0; // Reset footprints for green
        diceState[FOOTPRINT_YELLOW] = 0; // Reset footprints for yellow
        diceState[FOOTPRINT_RED] = 0; // Reset footprints for red
        int drawnCount = drawnDice[0] + drawnDice[1] + drawnDice[2]; // Count how many dice have been already "drawn" from footprints

        // If the supply cannot provide the required number of dice to make up three, we need to put the brain dice back into the supply
        int numInSupply = diceState[SUPPLY_GREEN] + diceState[SUPPLY_YELLOW] + diceState[SUPPLY_RED];
        if (3 - drawnCount > numInSupply) {
            // Draw what is in the supply
            drawnDice[GREEN_DIE] += diceState[SUPPLY_GREEN];
            drawnCount += diceState[SUPPLY_GREEN];
            diceState[SUPPLY_GREEN] = 0; // Reset green supply
            drawnDice[YELLOW_DICE] += diceState[SUPPLY_YELLOW];
            drawnCount += diceState[SUPPLY_YELLOW];
            diceState[SUPPLY_YELLOW] = 0; // Reset yellow supply
            drawnDice[RED_DICE] += diceState[SUPPLY_RED];
            drawnCount += diceState[SUPPLY_RED];
            diceState[SUPPLY_RED] = 0; // Reset red supply

            // Add all rolled brains back into the supply
            diceState[SUPPLY_GREEN] += NUM_DICE_GREEN - diceState[SHOTGUN_GREEN] - drawnDice[GREEN_DIE];
            diceState[SUPPLY_YELLOW] += NUM_DICE_YELLOW - diceState[SHOTGUN_YELLOW] - drawnDice[YELLOW_DICE];
            diceState[SUPPLY_RED] += NUM_DICE_RED - diceState[SHOTGUN_RED] - drawnDice[RED_DICE];
        }

        // Draw remaining dice from the supply
        ArrayList<Integer> supplyDice = new ArrayList<>();
        for (int color = 0; color < 3; color++) {
            for (int i = 0; i < diceState[SUPPLY_GREEN + color]; i++) {
                supplyDice.add(color);
            }   
        }
        Collections.shuffle(supplyDice); // Shuffle the supply dice to randomize the draw
        for (int i = 0; i < 3 - drawnCount; i++) {
            int color = supplyDice.get(i); // Draw a die from the supply
            diceState[SUPPLY_GREEN + color]--; // Decrement the supply for the drawn color
            int rollSymbol = DIE_SIDES[color][(int) (Math.random() * 6)]; // Simulate rolling the die 
            if (rollSymbol == BRAIN) {
                turnTotal++; // Increment the turn total for a brain
            } else if (rollSymbol == SHOTGUN) {
                // Increment the shotgun count for the corresponding color
                diceState[SHOTGUN_GREEN + color]++;
            } else if (rollSymbol == FOOTPRINT) {
                // Increment the footprint count for the corresponding color
                diceState[FOOTPRINT_GREEN + color]++;
            }
        }
    }

    public void initializeTurn() {
        turnTotal = 0;
        diceState = new int[] {
            0, 0, 0, // Shotguns for green, yellow, red
            0, 0, 0, // Footprints for green, yellow, red
            NUM_DICE_GREEN, NUM_DICE_YELLOW, NUM_DICE_RED // Supply for green, yellow, red
        };
    }

    public boolean isRolling() {
        // Use the current player's policy to decide whether to roll or hold
        return players[currentPlayer].willRoll(currentPlayer, playerScores[currentPlayer], playerScores[(currentPlayer + 1) % NUM_PLAYERS], turnTotal, 
                diceState[SHOTGUN_GREEN], diceState[SHOTGUN_YELLOW], diceState[SHOTGUN_RED],
                diceState[FOOTPRINT_GREEN], diceState[FOOTPRINT_YELLOW], diceState[FOOTPRINT_RED],
                diceState[SUPPLY_GREEN], diceState[SUPPLY_YELLOW], diceState[SUPPLY_RED]);
    }

    public void printState() {
        System.out.printf("p=%d, i=%d, j=%d, b=%d, sg=%d, sy=%d, sr=%d, fg=%d, fy=%d, fr=%d, cg=%d, cy=%d, cr=%d\n",
                currentPlayer,
                playerScores[currentPlayer],
                playerScores[(currentPlayer + 1) % NUM_PLAYERS],
                turnTotal,
                diceState[SHOTGUN_GREEN], diceState[SHOTGUN_YELLOW], diceState[SHOTGUN_RED],
                diceState[FOOTPRINT_GREEN], diceState[FOOTPRINT_YELLOW], diceState[FOOTPRINT_RED],
                diceState[SUPPLY_GREEN], diceState[SUPPLY_YELLOW], diceState[SUPPLY_RED]);
    }

    public int[] simulateGames(int numGames) {
        verbose = false; // Disable verbose output for multiple simulations
        int[] results = new int[NUM_PLAYERS];
        for (int i = 0; i < numGames; i++) 
            results[simulateGame()]++;
        return results;
    }

    public static void testPolicies(ZDPolicy player1, ZDPolicy player2, int numGames) {
        ZDSimulator sim = new ZDSimulator(player1, player2);
        ZDSimulator sim2 = new ZDSimulator(player2, player1); // Reverse the players for the second simulator
        int[][] results = {sim.simulateGames(numGames / 2), sim2.simulateGames(numGames / 2)};
        int[] totalWins = new int[] {results[0][0] + results[1][1], results[0][1] + results[1][0]};
        System.out.println("Results for " + numGames + " games:");
        System.out.println("Player 1 wins: " + totalWins[0] + " (" + (totalWins[0] * 100.0 / numGames) + "%)");
        System.out.println("Player 2 wins: " + totalWins[1] + " (" + (totalWins[1] * 100.0 / numGames) + "%)");
        System.out.println("Player 2 gap: " + (totalWins[1] - totalWins[0]) + " (" + ((totalWins[1] - totalWins[0]) * 100.0 / numGames) + "%)");
    }

    public static void main(String[] args) {
        ZDPolicy optimalPolicy = new ZombieDiceSolver();
        ZDPolicy minhPolicy = new AYZDPolicyMinh();
        //ZDPolicy ayzdOptimalPolicy = new AYZDPolicyOptimal();
        
        // ZDSimulator sim = new ZDSimulator(optimalPolicy, minhPolicy);
        // sim.simulateGame();
        // NOTE: MAX_SCORE at 26 is reached many times in 100,000 games, but was not observed in 10,000,000 games for MAX_SCORE at 39.
        //System.out.println(Arrays.toString(sim.simulateGames(10000000))); 
        /* Optimal vs. optimal for 10,000,000 games (MAX_SCORE=39):
$ time java -Xmx32g ZDSimulator
Number of unique dice states (without SHOTGUN_BUSTED): 10820
Loading solution from file: zd_solution_goal13_max39_eps1e-14.dat
[4666630, 5333370]

real    3m26.127s
user    0m0.000s
sys     0m0.046s
         */

        // testPolicies(minhPolicy, optimalPolicy, 10000000);
         /* minh vs. optimal (hit max score of 39 some times):
Results for 10000000 games:
Player 1 wins: 4809673 (48.09673%)
Player 2 wins: 5190327 (51.90327%)

          */

        testPolicies(optimalPolicy, minhPolicy, 10000000);
    }
}
