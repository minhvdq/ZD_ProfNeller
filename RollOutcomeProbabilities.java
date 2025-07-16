import java.util.Arrays;

public class RollOutcomeProbabilities {

	static final boolean VERBOSE = false;
	static final int MAX_CHOOSE = 3, NUM_DICE_ROLLED = 3, NUM_DIE_OUTCOMES = 3, NUM_ALL_OUTCOMES_ORDERED = (int) Math.pow(NUM_DIE_OUTCOMES, NUM_DICE_ROLLED);
	static int[][] choose = new int[MAX_CHOOSE + 1][MAX_CHOOSE + 1];
	static double[][][] probBFS = new double[NUM_DIE_OUTCOMES + 1][NUM_DIE_OUTCOMES + 1][NUM_DIE_OUTCOMES + 1]; 

	static {
		for (int n = 0; n < choose.length; n++)
			for (int k = 0; k <= n; k++) {
				if (k == 0)
					choose[n][k] = 1;
				else if (n == 0)
					choose[n][k] = 0;
				else
					choose[n][k] = choose[n - 1][k - 1] + choose[n - 1][k];
			}
		if (VERBOSE)
			System.out.println("Choose: " + Arrays.deepToString(choose));

		// Let b be the number of brains, f be the number of footsteps, and s be the number of shotguns
		double totalProb = 0;
		for (int b = 0; b <= NUM_DICE_ROLLED; b++) 
			for (int f = 0; f <= NUM_DICE_ROLLED - b; f++) {
				int s = NUM_DICE_ROLLED - b - f;
				int numRollOutcomes = choose[NUM_DICE_ROLLED][b] * choose[NUM_DICE_ROLLED - b][f];
				double prob = (double) numRollOutcomes / NUM_ALL_OUTCOMES_ORDERED;
				if (VERBOSE)
					System.out.printf("b=%d, f=%d, s=%s: %d of %d outcomes (probability %f)\n", 
							b, f, s, numRollOutcomes, NUM_ALL_OUTCOMES_ORDERED, prob);
				totalProb += prob;
				probBFS[b][f][s] = prob;
			}
		if (VERBOSE)
			System.out.println("Total probability: " + totalProb);
		
	}
	
	public static void main(String[] args) {
	}

}
