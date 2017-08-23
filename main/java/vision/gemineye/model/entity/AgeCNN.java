package vision.gemineye.model.entity;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_dnn.Importer;
import org.bytedeco.javacpp.opencv_dnn.Net;
import vision.gemineye.framework.Task;
import vision.gemineye.model.AgeBoundary;
import vision.gemineye.model.WeightedResult;
import vision.gemineye.model.ui.Screen;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.Blob;
import static org.bytedeco.javacpp.opencv_dnn.createCaffeImporter;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class AgeCNN implements Task {

    private static Net ageNN;
    private static final AgeBoundary[] AGE_BOUNDRIES = new AgeBoundary[]{
            new AgeBoundary(0, 2),
            new AgeBoundary(4, 6),
            new AgeBoundary(8, 13),
            new AgeBoundary(15, 20),
            new AgeBoundary(25, 32),
            new AgeBoundary(38, 43),
            new AgeBoundary(48, 53),
            new AgeBoundary(60, 99),
    };

    private static final String[] AGES = new String[]{"0-2", "4-6", "8-13", "15-20", "25-32", "38-43", "48-53", "60-"};

    public static WeightedResult<AgeBoundary> predict(Mat face, int depth) {
        try {
            Mat croppedMat = new Mat();
            resize(face, croppedMat, new Size(256, 256));
            normalize(croppedMat, croppedMat, 0, Math.pow(2, depth), NORM_MINMAX, -1, null);

            Blob data = new Blob(croppedMat);
            ageNN.setBlob(".data", data);
            ageNN.forward();
            Blob prob = ageNN.getBlob("prob");

            DoublePointer pointer = new DoublePointer(new double[1]);
            Point max = new Point();
            minMaxLoc(prob.matRefConst(), null, pointer, null, max, null);

            return new WeightedResult<AgeBoundary>(
                    100,
                    AGE_BOUNDRIES[max.x()]
            );


        } catch (Exception e) {
            e.printStackTrace();
        }

//        throw new RuntimeException("FUCK");

        return new WeightedResult<AgeBoundary>(
                100,
                AgeBoundary.UNKNOWN
        );
    }

    @Override
    public String bootstrap() throws Exception {
        return "Loading age neural net";
    }

    @Override
    public String resolve(int index) throws Exception {
        try {
            ageNN = new Net();

            File protocol = Screen.getFile("classifier/deploy_agenet.prototxt", "classifier", ".prototext");
            File model = Screen.getFile("classifier/age_net.caffemodel", "classifier", ".caffemodel");

            Importer importer = createCaffeImporter(protocol.getAbsolutePath(), model.getAbsolutePath());

            importer.populateNet(ageNN);
            importer.close();

        } catch (Exception e) {
        }
        return "Loaded Age CNN";
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public String label() {
        return "AGE-CNN";
    }
}

