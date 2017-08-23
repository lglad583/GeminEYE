package vision.gemineye.model.entity;

import javafx.scene.image.Image;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face;
import vision.gemineye.framework.AppLogger;
import vision.gemineye.model.ModelEntry;
import vision.gemineye.model.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;

public class Model {

    private static AppLogger logger = AppLogger.get(Model.class);

    private List<String> data;

    public Model(List<String> data) {
        this.data = data;
    }

    public List<String> getData() {
        return this.data;
    }

    private transient List<Image> resolvedImages;

    public void resolveImages(List<Image> images) {
        if (resolvedImages != null) {
            resolvedImages.clear();
        } else {
            resolvedImages = new ArrayList<>();
        }

        resolvedImages.addAll(images);
    }

    public List<Image> getResolvedImages() {
        if (resolvedImages == null) {
            resolvedImages = new ArrayList<>();
        }
        return resolvedImages;
    }

    public void deleteData(String rootFolder, String filePrefix, String format) {
        Path path = Paths.get(rootFolder);

        logger.blocks("DELETING MODEL DATA",
                "size", data.size(),
                "filePrefix", filePrefix,
                "format", format
        );

        if (filePrefix == null) {
            filePrefix = "";
        } else {
            filePrefix = filePrefix + "_";
        }

        if (!Files.exists(path)) {
            logger.info("path", path, "does not exists. skipping delete");
            return;
        }

        for (String model : data) {
            Path modelPath = path.resolve(filePrefix + model + format);

            try {
                Files.deleteIfExists(modelPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public void deleteData(String rootFolder, String format) {
        deleteData(rootFolder, null, format);
    }

    public void addWithImage(String id, Image flexFrameImage) {
        data.add(id);

        if (resolvedImages == null) {
            resolvedImages = new ArrayList<>();
        }

        resolvedImages.add(flexFrameImage);
    }

    public void removeAtIndex(int index) {
        resolvedImages.remove(index);
        data.remove(index);
    }

    public void removeAll(Set<Integer> toRemove) {
        List<String> toRemovePaths = new ArrayList<>();
        List<Image> toRemoveImages = new ArrayList<>();

        for (int remove : toRemove) {
            toRemovePaths.add(data.get(remove));
            toRemoveImages.add(resolvedImages.get(remove));
        }

        Path path = Paths.get(Profiles.getModelsFolder());

        if (Files.exists(path)) {
            for (String remove : toRemovePaths) {
                Path modelPath = path.resolve(remove + Profile.IMAGE_FORMAT);

                try {
                    Files.deleteIfExists(modelPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        data.removeAll(toRemovePaths);
        resolvedImages.removeAll(toRemoveImages);
    }

    private transient Map<String, opencv_core.Mat> resolvedMatList;

    public void setResolvedMat(String id, opencv_core.Mat mat) {
        if (resolvedMatList == null) {
            resolvedMatList = new HashMap<>();
        }
        resolvedMatList.put(id, mat);
    }

    public List<ModelEntry> getResolvedImageMats(Iterable<Integer> indexList) {
        List<ModelEntry> matList = new ArrayList<>();
        if (resolvedMatList == null) {
            return matList;
        }
        for (int index : indexList) {
            opencv_core.IplImage iplImage = new opencv_core.IplImage(resolvedMatList.get(data.get(index)));
            Image image = resolvedImages.get(index);

            matList.add(new ModelEntry(iplImage, image));
        }
        return matList;
    }
}
