
/**
 * AYZDPolicy - a common interface for evaluating and experimenting with various
 * All Yellow Zombie Dice play policies.
 * @author Todd W. Neller
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
 */
public interface AYZDPolicy {
	/**
	 * @param p - player (0/1)
	 * @param i - current player score
	 * @param j - current opponent score
	 * @param b - turn total (brains rolled this turn)
	 * @param s - shotguns rolled this turn
	 * @return whether or not player will roll in this state
	 */
	boolean willRoll(int p, int i, int j, int b, int s);
}
