package vision.gemineye;


import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import vision.gemineye.framework.Task;
import vision.gemineye.model.ui.Screen;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.bytedeco.javacpp.opencv_imgproc.INTERSECT_NONE;
import static org.bytedeco.javacpp.opencv_imgproc.minAreaRect;
import static org.bytedeco.javacpp.opencv_imgproc.rotatedRectangleIntersection;

public class Common implements Task {


    public static Image PROFILE;
    public static FileChooser chooser;

    static {
        chooser = new FileChooser();
        chooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
    }

    public static java.awt.Image makeColorTransparent(BufferedImage im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {

            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    public static void selectImageFiles(String title, Consumer<List<File>> imageConsumer) {
        chooser.setTitle(title);

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.*"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        List<File> files = chooser.showOpenMultipleDialog(Core.getMainScreen().getStage());
        if (files == null) {
            imageConsumer.accept(new ArrayList<>());
        } else {
            imageConsumer.accept(files);
        }

    }

    public static String removeFileExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    public static Background getPNGBackgroundImage(String asset) {

        BackgroundImage image = new BackgroundImage(
                new Image(Screen.getPNGAssetStream(asset)),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(24, 24, true, true, false, true)
        );

        return new Background(image);
    }

    public static String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    public static boolean isFlagged(String[] flagCollection, String flagName) {
        if (flagCollection == null || flagCollection.length == 0 || flagName == null) {
            return false;
        }

        final String flag = flagName.toUpperCase();

        return Arrays
                .stream(flagCollection)
                .filter(arg -> arg != null && !arg.isEmpty())
                .map(String::toUpperCase)
                .anyMatch(arg -> arg.contentEquals(flag));
    }

    @Override
    public String bootstrap() throws Exception {
        return "Loading common assets";
    }

    @Override
    public String resolve(int index) throws Exception {
        PROFILE = new Image(Screen.getPNGAssetStream("profile"));

        return "Loaded common assets";
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public String label() {
        return "COMMON";
    }

    public static opencv_core.RotatedRect toRotatedRect(opencv_core.Rect location) {
        return new opencv_core.RotatedRect(
                new opencv_core.Point2f(location.x() + location.width() / 2, location.y() + location.height() / 2),
                new opencv_core.Size2f(location.width(), location.height()),
                0F
        );
    }

    public static double getUnionRatio(opencv_core.Rect locationA, opencv_core.Rect locationB) {
        opencv_core.Mat collision = new opencv_core.Mat();
        int type = rotatedRectangleIntersection(
                Common.toRotatedRect(locationA),
                Common.toRotatedRect(locationB),
                collision
        );

        if (type == INTERSECT_NONE) {
            return 1000;
        }


        int mh = Math.max(locationA.height(), locationB.height());
        int sh = Math.min(locationA.height(), locationB.height());
        int mw = Math.max(locationA.width(), locationB.width());
        int sw = Math.min(locationA.width(), locationB.width());

        if(((mh*.15) > sh) || ((mw*.15) > sw)) {
            return 1000;
        }

        opencv_core.Rect intersection = minAreaRect(collision).boundingRect();

        double ratio = Math.abs((double) (Math.abs(locationA.area() - intersection.area()) / (double) intersection.area()));

        return ratio;
    }

    public static opencv_core.Rect conformToRectangle(opencv_core.Rect boundsA, opencv_core.Rect boundsB, float step) {

        float ox = 0;
        float oy = 0;
        float ow = 0;
        float oh = 0;

        if (boundsA.x() < boundsB.x()) {
            if (boundsA.x() + ox > boundsB.x()) {
                ox = boundsB.x() - boundsA.x();
            } else {
                ox += step;
            }
        } else {
            if (boundsA.x() - ox < boundsB.x()) {
                ox = -(boundsB.x() - boundsA.x());
            } else {
                ox -= step;
            }
        }

        if (boundsA.y() < boundsB.y()) {
            if (boundsA.y() + oy > boundsB.y()) {
                oy = boundsB.y() - boundsA.y();
            } else {
                oy += step;
            }
        } else {
            if (boundsA.y() - oy < boundsB.y()) {
                oy = -(boundsB.y() - boundsA.y());
            } else {
                oy -= step;
            }
        }

        if (boundsA.width() < boundsB.width()) {
            if (boundsA.width() + step > boundsB.width()) {
                ow = boundsB.width() - boundsA.height();
            } else {
                ow += step;
            }
        } else {
            if (boundsA.width() - step < boundsB.width()) {
                ow = -(boundsB.width() - boundsA.height());
            } else {
                ow -= step;
            }
        }

        if (boundsA.height() < boundsB.height()) {
            if (boundsA.height() + step > boundsB.height()) {
                oh = boundsB.height() - boundsA.height();
            } else {
                oh += step;
            }
        } else {
            if (boundsA.height() - step < boundsB.height()) {
                oh = -(boundsB.height() - boundsA.height());
            } else {
                oh -= step;
            }
        }

        int tx = (int) Math.floor(boundsA.x() + ox);
        int ty = (int) Math.floor(boundsA.y() + oy);
        int tw = (int) Math.ceil(boundsA.width() + ow);
        int th = (int) Math.ceil(boundsA.height() + oh);

        opencv_core.Rect translated = new opencv_core.Rect(tx, ty, tw, th);

        return translated;

    }


    private static OpenCVFrameConverter frameConverter = new OpenCVFrameConverter.ToIplImage();
    public static Frame convert(opencv_core.Mat attached) {
        return frameConverter.convert(new opencv_core.IplImage(attached.clone()));
    }
}
