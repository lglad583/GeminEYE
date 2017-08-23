package vision.gemineye.model.entity;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.opencv_dnn.Importer;
import org.bytedeco.javacpp.opencv_dnn.Net;
import vision.gemineye.framework.Task;
import vision.gemineye.model.Attributes;
import vision.gemineye.model.WeightedResult;
import vision.gemineye.model.ui.Screen;

import java.io.File;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_dnn.Blob;
import static org.bytedeco.javacpp.opencv_dnn.createCaffeImporter;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class GenderCNN implements Task {

    private static Net genderNN;

    public static WeightedResult<Attribute> predict(Mat face, int depth) {
        try {
            Mat croppedMat = new Mat();
            resize(face, croppedMat, new Size(256, 256));
            normalize(croppedMat, croppedMat, 0, Math.pow(2, depth), NORM_MINMAX, -1, null);

            Blob data = new Blob(croppedMat);
            genderNN.setBlob(".data", data);
            //opencv_dnn.Blob.fromImages(face);
           genderNN.forward();
            Blob prob = genderNN.getBlob("prob");


            Indexer indexer = prob.matRefConst().createIndexer();

            if (indexer.getDouble(0, 0) > indexer.getDouble(0, 1)) {
                return new WeightedResult<>(
                        indexer.getDouble(0, 0),
                        Attributes.get("gender", "male")
                );
            } else
                return new WeightedResult<>(
                        indexer.getDouble(0, 0),
                        Attributes.get("gender", "female")
                );

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new WeightedResult<>(
                0,
                Attributes.get("gender", "-1")
        );
    }

    @Override
    public String bootstrap() throws Exception {
        return "Building neural net";
    }

    @Override
    public String resolve(int index) throws Exception {
        try {
            genderNN = new Net();

            File protocol = Screen.getFile("classifier/deploy_gendernet.prototxt", "classifier", ".prototxt");
            File model = Screen.getFile("classifier/gender_net.caffemodel", "classifier", ".caffemodel");

            Importer importer = createCaffeImporter(protocol.getAbsolutePath(), model.getAbsolutePath());

            importer.populateNet(genderNN);
            importer.close();

        }catch(Exception e){
            e.printStackTrace();
        }
        return "Loaded Gender CNN";
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public String label() {
        return "GENDER-CNN";
    }
}

