package vision.gemineye.model;

import org.bytedeco.javacpp.opencv_core;

public class FeaturePoint {
    private int class_id;
    private float size;
    private float angle;
    private float response;
    private int octave;

    public opencv_core.Point2f getLocation() {
        return location;
    }

    public void setLocation(opencv_core.Point2f location) {
        this.location = location;
    }

    public opencv_core.KeyPoint getKey() {

        return new opencv_core.KeyPoint(
                location,
                this.size,
                this.angle,
                this.response,
                this.octave,
                this.class_id
        );
    }

    public void setKey(opencv_core.KeyPoint key) {
        this.location = location;
        this.size = key.size();
        this.angle = key.angle();
        this.response = key.response();
        this.octave = key.octave();
        this.class_id = key.class_id();
    }

    public FeaturePoint(opencv_core.Point2f location, opencv_core.KeyPoint key) {
        setKey(key);

        this.location = location;
    }

    public opencv_core.Point2f location;

}
