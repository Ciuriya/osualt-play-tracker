package help;

public class CycleBlurb extends HelpBlurb {

	public CycleBlurb() {
		super(new String[] { "cycle", "cycles" }, "Cycles are used to split the tracked osu players up into more easily processable lists.\n" +
												  "Each cycle represents a certain period of inactivity, for example, a cycle could be for users between 2 and 6 hours of inactivity.\n\n" +
												  "Cycles are ordered from lowest to highest in terms of inactivity, with cycle 0 being an exception.\n" +
												  "Cycle 0 is the **must be processed now** cycle, once it finishes running, those who haven't set a play falls into cycle 1 automatically.\n" +
												  "\nYou may use `-botstats` to see cycle information (shows progress and time left)");
	}
}
