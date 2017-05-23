package chord.analyses.damianoAnalysis;

public class Utilities {
	
	private static boolean verbose;
	
	public static void out(String str) { System.out.println(str); }

	public static void out0(String str) { System.out.print(str); }

	public static void debug(String str) {
		if (isVerbose()) {
			System.out.println(str);
		}
	}

	public static void debug0(String str) {
		if (isVerbose()) {
			System.out.print(str);
		}
	}

	public static void begin(String str) { debug("[BEGIN] " + str);	}
	
	public static void end(String str) { debug("[END] " + str);	}
	
	public static void err(String str) { out("[ERROR] " + str); }
	
	public static void warn(String str) { out("[WARNING] " + str); }
	
	public static boolean isVerbose() { return verbose; }

	public static void setVerbose(boolean verbose) { Utilities.verbose = verbose; }

}
