package vision.gemineye.model;

import org.bytedeco.javacpp.opencv_core.DMatchVector;

public class CrossMatch {

    private int matches;
    private DMatchVector vector;

    public CrossMatch(int matches, DMatchVector vector) {
        this.matches = matches;
        this.vector = vector;
    }

    public int getMatches() {
        return matches;
    }

    public void setMatches(int matches) {
        this.matches = matches;
    }

    public DMatchVector getVector() {
        return vector;
    }

    public void setVector(DMatchVector vector) {
        this.vector = vector;
    }
}
