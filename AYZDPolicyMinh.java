
// by Minh Q. Vu Dinh
public class AYZDPolicyMinh implements AYZDPolicy, ZDPolicy {

	final static int GOAL = 13, MAX_SCORE = 2 * GOAL;
	static int param;
	
	public boolean willRoll(int p, int i, int j, int b, int s) {
			int iHold = i + b;
			if (p == 1 && j >= GOAL && iHold < j)
				return true;
//			if (p == 1 && j >= GOAL)
//				return iHold < j;
			else if (s == 0) 
				return true; 
			else if (s == 1) { 
				if (j >= 8) 
					return iHold < Math.max(13, j + 3); //iHold < (13 > j + 3 ? 13 : j + 3);  
				else 
					return b < 4;
			}
			else 
				return b < 1;
	}

	@Override
	public boolean willRoll(int p, int i, int j, int b, int sg, int sy, int sr, int fg, int fy, int fr, int cg, int cy,
			int cr) {

		return willRoll(p, i, j, b, sg + sy + sr);
	}
	
//	public static void main(String[] args) {
//		evaluate();
////		tuneParam(0,5);
//	}
//	
//	public static void evaluate() {
//		AYZDPolicy optimal = new AYZDPolicyOptimal();
//		AYZDPolicy approx = new AYZDPolicyMinh();
//		AYZDPolicyEvaluator evaluator = new AYZDPolicyEvaluator(optimal, approx);
//		double pol1p1 = evaluator.pWin1[0][0][0][0][0];
//		double pol2p1 = evaluator.pWin2[0][0][0][0][0];
//		double pol1p2 = 1 - pol2p1;
//		double pol2p2 = 1 - pol1p1;
//		double avg1 = (pol1p1 + pol1p2) / 2.0;
//		double avg2 = (pol2p1 + pol2p2) / 2.0;
//		double diff = avg2 - avg1;
//		System.out.println("Policy 1 first player win rate: " + pol1p1);
//		System.out.println("Policy 2 first player win rate: " + pol2p1);
//		System.out.println("Policy 1 second player win rate: " + pol1p2);
//		System.out.println("Policy 2 second player win rate: " + pol2p2);
//		System.out.println("Policy 1 average win rate: " + avg1);
//		System.out.println("Policy 2 average win rate: " + avg2);
//		System.out.println("Average win rate difference: " + diff);
//	}
//	
//	public static void tuneParam(int minValue, int maxValue) {
//		AYZDPolicy optimal = new AYZDPolicyOptimal();
//		for (param = minValue; param <= maxValue; param++) {
//			AYZDPolicy approx = new AYZDPolicyMinh();
//			AYZDPolicyEvaluator evaluator = new AYZDPolicyEvaluator(optimal, approx, 1e-9);
//			double pol1p1 = evaluator.pWin1[0][0][0][0][0];
//			double pol2p1 = evaluator.pWin2[0][0][0][0][0];
//			double pol1p2 = 1 - pol2p1;
//			double pol2p2 = 1 - pol1p1;
//			double avg1 = (pol1p1 + pol1p2) / 2.0;
//			double avg2 = (pol2p1 + pol2p2) / 2.0;
//			double diff = avg2 - avg1;
//			//		System.out.println("Policy 1 first player win rate: " + pol1p1);
//			//		System.out.println("Policy 2 first player win rate: " + pol2p1);
//			//		System.out.println("Policy 1 second player win rate: " + pol1p2);
//			//		System.out.println("Policy 2 second player win rate: " + pol2p2);
//			//		System.out.println("Policy 1 average win rate: " + avg1);
//			//		System.out.println("Policy 2 average win rate: " + avg2);
//			System.out.println("param " + param + ", difference: " + diff);
//		}
//	}

}
