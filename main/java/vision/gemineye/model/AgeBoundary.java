package vision.gemineye.model;

public class AgeBoundary {

    public static final AgeBoundary UNKNOWN = null;

    private final int lower;
    private final int upper;

    public AgeBoundary(int lower, int upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public int getLower() {
        return lower;
    }

    public int getUpper() {
        return upper;
    }

    @Override
    public String toString() {
        return lower + "-" + upper;
    }
}
