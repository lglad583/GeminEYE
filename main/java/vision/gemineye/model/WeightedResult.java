package vision.gemineye.model;

public class WeightedResult<T> {

    private double confidence;
    private T result;

    public WeightedResult(double confidence, T result) {
        this.confidence = confidence;
        this.result = result;
    }

    public T getResult() {
        return result;
    }

    public double getConfidence() {
        return confidence;
    }
}
