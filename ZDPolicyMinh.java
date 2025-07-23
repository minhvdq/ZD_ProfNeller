public class ZDPolicyMinh implements  ZDPolicy {

    final int GOAL_SCORE = 13;
    final int MAX_SCORE = 26;

    public boolean willRoll(int p, int i, int j, int b, int sg, int sr, int sy) {
        // int stateInteger = ZombieDiceSolver.diceStates[sIndex];
        // int[] stateArray = ZombieDiceSolver.getDiceStateArray(stateInteger);
        // int sgRed = stateArray[SHOTGUN_RED];
        // int sgGreen = stateArray[SHOTGUN_GREEN];
        // int sgYellow = stateArray[SHOTGUN_YELLOW];
        int s = sg + sr + sy;

        int iHold = i + b;
		if (p == 1 && j >= GOAL_SCORE && iHold < j)
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
    public boolean willRoll(int p, int i, int j, int b, int sg, int sy, int sr,
	                 int fg, int fy, int fr, int cg, int cy, int cr){
        int s = sg + sr + sy;
        int rir = fr + cr;

        int riskNumber = 1;

        int iHold = i + b;
		if (p == 1 && j >= GOAL_SCORE && iHold < j)
			return true;
//			if (p == 1 && j >= GOAL)
//				return iHold < j;
		else if (s == 0) 
			return true; 
		else if (s == 1) { 
            // red dice player might have to roll
            // int rir = fr + cr;
            // if (rir <= riskNumber)
            //     if(j >= 7)
            //         return iHold < Math.max(13,j + 3);
            //     else
            //         return b < 4;
            // else
            //     if (j >= 8) 
            //         return iHold < Math.max(13, j + 3); //iHold < (13 > j + 3 ? 13 : j + 3);  
            //     else 
            //         return b < 4;
            if (j >= 8) 
                return iHold < Math.max(13, j + 3); //iHold < (13 > j + 3 ? 13 : j + 3);  
            else 
                return b < 4;
		}
		else 
            if (rir <= riskNumber)
                return b < 3;
			return b < 1;
    }
}