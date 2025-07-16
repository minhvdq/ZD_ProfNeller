
public class AYZDPolicyOptimal implements AYZDPolicy, ZDPolicy {

	public static AYZDSolver solver = new AYZDSolver();
	
	@Override
	public boolean willRoll(int p, int i, int j, int b, int s) {
		return solver.willRoll(p, i, j, b, s);
	}
	
	@Override
	public boolean willRoll(int p, int i, int j, int b, int sg, int sy, int sr, int fg, int fy, int fr, int cg, int cy,
			int cr) {

		return willRoll(p, i, j, b, sg + sy + sr);
	}
}
