package vision.gemineye.model;

import javafx.scene.image.Image;
import org.bytedeco.javacpp.opencv_core;

public class ModelEntry {

    private opencv_core.IplImage image;
    private Image loadedImage;

    public ModelEntry(opencv_core.IplImage image, Image loadedImage) {
        this.image = image;
        this.loadedImage = loadedImage;
    }

    public opencv_core.IplImage getImage() {
        return image;
    }

    public void setImage(opencv_core.IplImage image) {
        this.image = image;
    }

    public Image getLoadedImage() {
        return loadedImage;
    }

    public void setLoadedImage(Image loadedImage) {
        this.loadedImage = loadedImage;
    }
}
