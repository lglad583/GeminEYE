package vision.gemineye.model;

import org.bytedeco.javacpp.opencv_core;

public class Boundary {

    public int x;
    public int y;
    public int w;
    public int h;

    public Boundary(opencv_core.Rect rect) {
        set(rect);
    }

    public void set(opencv_core.Rect rect) {
        this.x = rect.x();
        this.y = rect.y();
        this.w = rect.width();
        this.h = rect.height();
    }

}
