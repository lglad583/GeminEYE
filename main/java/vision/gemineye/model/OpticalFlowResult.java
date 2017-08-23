package vision.gemineye.model;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import java.util.List;

public class OpticalFlowResult {

    private boolean match;
    private KeyPointVector featuresKeyPointVector;
    private IplImage faceAsIPL;
    private Mat face;
    private Mat faceGrey;
    private Mat descriptors;
    private Rect boundingRectangle;
    private List<FeaturePoint> featurePoints;
    public opencv_core.DMatchVector matches;

    public Mat getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(Mat descriptors) {
        this.descriptors = descriptors;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public KeyPointVector getFeaturesKeyPointVector() {
        return featuresKeyPointVector;
    }

    public void setFeaturesKeyPointVector(KeyPointVector featuresKeyPointVector) {
        this.featuresKeyPointVector = featuresKeyPointVector;
    }

    public IplImage getFaceAsIPL() {
        return faceAsIPL;
    }

    public void setFaceAsIPL(IplImage faceAsIPL) {
        this.faceAsIPL = faceAsIPL;
    }

    public Mat getFace() {
        return face;
    }

    public void setFace(Mat face) {
        this.face = face;
    }

    public Rect getBoundingRectangle() {
        return boundingRectangle;
    }

    public void setBoundingRectangle(Rect boundingRectangle) {
        this.boundingRectangle = boundingRectangle;
    }

    public int x() {
        return boundingRectangle.x();
    }

    public int y() {
        return boundingRectangle.y();
    }

    public int w() {
        return boundingRectangle.width();
    }

    public int h() {
        return boundingRectangle.height();
    }

    public Mat getFaceGrey() {
        return faceGrey;
    }

    public void setFaceGrey(Mat faceGrey) {
        this.faceGrey = faceGrey;
    }

    public List<FeaturePoint> getFeaturePoints() {
        return featurePoints;
    }

    public void setFeaturePoints(List<FeaturePoint> featurePoints) {
        this.featurePoints = featurePoints;
    }
}
