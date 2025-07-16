
/**
 * GROPolicy - a common interface for evaluating and experimenting with various
 * Great Rolled Ones play policies.
 * @author Todd W. Neller
 */
public interface ZDPolicy {
	/**
	 * @param p - player (0/1)
	 * @param i - current player score
	 * @param j - current opponent score
	 * @param b - turn total (brains rolled)
	 * @param sg - green shotguns rolled
	 * @param sy - yellow shotguns rolled
	 * @param sr - red shotguns rolled
	 * @param fg - green footprints rolled
	 * @param fy - yellow footprints rolled
	 * @param fr - red footprints rolled
	 * @param cg - green dice in supply (cup)
	 * @param cy - yellow dice in supply
	 * @param cr - red dice in supply
	 * (all other dice are assumed to be set aside as brains)
	 * @return whether or not player will roll in this state
	 */
	boolean willRoll(int p, int i, int j, int b, int sg, int sy, int sr,
	                 int fg, int fy, int fr, int cg, int cy, int cr);
}