package vision.gemineye.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.transform.MatrixType;
import jdk.nashorn.internal.runtime.options.Option;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_face.BasicFaceRecognizer;
import vision.gemineye.Common;
import vision.gemineye.Core;
import vision.gemineye.controllers.modules.FeedsController;
import vision.gemineye.framework.AppLogger;
import vision.gemineye.framework.TaskStorage;
import vision.gemineye.model.entity.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.createFisherFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class Profiles extends TaskStorage {

    private static final String TAG = "PROFILE";
    private static Profile[] profiles;

    private static AppLogger logger = AppLogger.get(Profiles.class);

    public static boolean trainingDataExists;
    private static opencv_face.FaceRecognizer recognizer;
    private static Path path;

    public static Profile ADMIN_PROFILE = new Profile(
            "ADMIN",
            new Identity(
                    new Name("admin", "", ""),
                    new Date(),
                    new Attribute("-1", "Unknown"),
                    new Attribute("-1", "Unknown")
            ),
            new Model(new ArrayList<>()),
            new Model(new ArrayList<>()),
            new Credentials(
                    "admin",
                    "admin",
                    null
            ),
            new Role("Admin", EnumSet.allOf(Permission.class)),
            null
    );

    public String process(Path originalPath) throws Exception {
        path = originalPath.resolve("profiles.json");

        if (!Files.exists(path)) {
            Files.createFile(path);

            Files.write(path, "[]".getBytes());

            profiles = new Profile[0];

            return "Created Empty Profiles";
        }

        String JSON = new String(Files.readAllBytes(path));

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        gsonBuilder.setPrettyPrinting();

        Gson gson = gsonBuilder.create();
        profiles = gson.fromJson(JSON, Profile[].class);

        Arrays.asList(profiles).forEach(person -> {
            if (person.getId() == null || person.getId().isEmpty()) {
                person.setId(randomUUID().toString());
            }
        });


        for (Profile profile : profiles) {
            profile.reloadProfileImage();
            profile.reloadGalleryImages();
            profile.reloadTrainingImages();
        }

        int imageCount = train();

        return "Loaded " + profiles.length + " profiles and " + imageCount + " model images";
    }

    public static Profile[] getProfiles() {
        return profiles;
    }

    @Override
    public String bootstrap() throws Exception {
        return "Loading profiles";
    }

    @Override
    public String label() {
        return TAG;
    }

    public static Profile findByUsername(String username) {
        if (username == null) {
            return null;
        }

        username = username.toLowerCase().trim();

        if (username.contentEquals(ADMIN_PROFILE.getCredentials().getUsername())) {
            return ADMIN_PROFILE;
        }

        for (Profile profile : profiles) {
            if (profile.getCredentials() != null && profile.getCredentials().getUsername().equalsIgnoreCase(username)) {
                return profile;
            }
        }

        return null;
    }

    public static Image loadProfileImage(Profile profile) {
        if (profile == null || profile.getProfileImageId() == null) {
            return null;
        }

        try {

            Path profileImage = Profiles.getProfileImagePath(profile);
            if (profileImage == null) {
                return null;
            }

            return new Image(profileImage.toUri().toString());
        } catch (Exception e) {
            return null;
        }
    }


    public static void add(Profile profile) {
        if (path == null) {
            throw new RuntimeException("Profiles not loaded yet");
        }

        ArrayList<Profile> newProfileList = new ArrayList<>();
        newProfileList.addAll(Arrays.asList(profiles));

        newProfileList.add(profile);

        profiles = newProfileList.toArray(new Profile[0]);

        save();

        Core.getMainScreen().getController().reloadProfileList();
    }

    public static void save() {
        GsonBuilder builder = new GsonBuilder();
        builder.setLenient();
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        String JSON = gson.toJson(profiles);

        try {
            Files.write(path, JSON.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final int X_THRESHOLD = 80;
    private static final int Y_THRESHOLD = 80;
    private static final int W_THRESHOLD = 80;
    private static final int H_THRESHOLD = 80;

    public static Profile getProfileFromLocation(Rect rect) {
        int ax = rect.x();
        int ay = rect.y();
        int aw = rect.width();
        int ah = rect.height();

        int mpX = ax + aw / 2;
        int mpY = ay + ah / 2;

        for (Profile profile : profiles) {
            if (profile.getLastBoundary() == null) {
                continue;
            }

            int pMpX = profile.getLastBoundary().x + profile.getLastBoundary().w / 2;
            int pMpY = profile.getLastBoundary().y + profile.getLastBoundary().y / 2;

            int dMpX = Math.abs(pMpX - mpX);
            int dMpY = Math.abs(pMpY - mpY);
            int dW = Math.abs(aw - profile.getLastBoundary().w);
            int dH = Math.abs(ah - profile.getLastBoundary().h);

            if (dMpX < X_THRESHOLD &&
                    dMpY < Y_THRESHOLD &&
                    dW < W_THRESHOLD &&
                    dH < H_THRESHOLD) {
                return profile;
            }
        }

        return null;
    }

    public static Profile findProfileForMatrix(Mat face) {

        if (!trainingDataExists) {
            return null;
        }

        IplImage cropped = new IplImage(face);
        IplImage resized = IplImage.create(225, 400, cropped.depth(), cropped.nChannels(), INTER_CUBIC);
        cvResize(cropped, resized);

        face = new Mat(resized);
        if(!face.isContinuous()) {
            face = face.clone();
        }

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        recognizer.predict(face, label, confidence);

        int index = label.get();
        double certainty = confidence.get();

        if (index == -1 || certainty > 50D) {
            return null;
        }

        return profiles[index];
    }


    public static int train() {
        recognizer = createLBPHFaceRecognizer();

        String trainingDir = getModelsFolder();

        File root = new File(trainingDir);
        if (!root.exists()) {
            trainingDataExists = false;
            return 0;
        }

        List<File> files = Arrays.stream(root.listFiles())
                .filter(file -> !file.isDirectory()
                        && !file.getName().startsWith(Profile.PROFILE_IMAGE_PREFIX)
                        && !file.getName().startsWith(Profile.GALLERY_IMAGE_PREFIX)
                        && file.getName().endsWith(Profile.IMAGE_FORMAT))
                .collect(Collectors.toList());

        final int size = files.size();

        if (size == 0) {
            trainingDataExists = false;
            return 0;
        }


        MatVector images = new MatVector(size);

        Mat labels = new Mat(size, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();
        HashSet<Integer> labelHistory = new HashSet<>();

        for (int index = 0; index < size; index++) {
            File image = files.get(index);

            String name = image.getName();

            final String UUID = name.substring(0, name.lastIndexOf("."));

            Mat img = imread(image.getAbsolutePath());
            IplImage imgToIPL = new IplImage(img);
            IplImage grayImg = opencv_core.IplImage.create(imgToIPL.width(), imgToIPL.height(), IPL_DEPTH_8U, 1);
            cvCvtColor(imgToIPL, grayImg, CV_BGR2GRAY);

            IplImage resized = IplImage.create(225, 400, grayImg.depth(), grayImg.nChannels(), INTER_CUBIC);
            cvResize(grayImg, resized);

            Mat face = new Mat(resized);

            Profile profile = null;
            int label = -1;

            for (int x = 0; x < profiles.length; x++) {
                if (profiles[x].hasModelUUID(UUID)) {
                    profile = profiles[x];
                    label = x;
                    labelHistory.add(x);
                    break;
                }
            }

            if (profile == null) {
                throw new RuntimeException("Model mismatch on " + UUID);
            }

            System.out.println("For user " + profile.getIdentity().getName() + ", " + image.getName());

            profile.getModel().setResolvedMat(UUID, img);

            images.put(index, new Mat(face));
            labelsBuf.put(index, label);
        }

        trainingDataExists = labelHistory.size() > 1 && size > 1;

        if (!trainingDataExists) {
            return 0;
        }

        recognizer.train(images, labels);


        return size;
    }

    public static String getModelsFolder() {
        return Paths.get(System.getenv("APPDATA"), "FlexVision", "models").toAbsolutePath().toString();
    }

    public static void delete(Profile profile) {
        delete(profile, true);
    }

    public static void delete(Profile profile, boolean save) {
        // Delete model data
        String models = getModelsFolder();

        if (profile.getModel() != null) {
            profile.getModel().deleteData(models, Profile.IMAGE_FORMAT);
        }

        if (profile.getGalleryImages() != null) {
           profile.getGalleryImages().deleteData(models, Profile.GALLERY_IMAGE_PREFIX, Profile.IMAGE_FORMAT);
        }


        Path profileImage = Profiles.getProfileImagePath(profile);
        if (profileImage != null) {
            try {
                if (Files.exists(profileImage)) {
                    Files.delete(profileImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Filter out profile
        List<Profile> profiles = Arrays
                .stream(Profiles.profiles)
                .filter(user -> !user.getId().contentEquals(profile.getId()))
                .collect(Collectors.toList());

        Profiles.profiles = profiles.toArray(new Profile[0]);

        // Save
        if(save)
        save();
    }

    private static Path getProfileImagePath(Profile profile) {
        if (profile == null || profile.getProfileImageId() == null) {
            return null;
        }

        Path path = Paths.get(
                Profiles.getModelsFolder(),
                Profile.PROFILE_IMAGE_PREFIX + "_" +  profile.getProfileImageId() + ".jpeg"
        );

        if (!Files.exists(path)) {
            profile.setProfileImageId(null);
            profile.setProfileImage(null);
            return null;
        }

        return path;
    }

    public static void save(Profile profile) {
        if (profile == null) {
            return;
        }

        logger.info("Saving profile: ", profile);


        int index = -1;

        for (int i = 0; i < Profiles.profiles.length; i++) {
            if (Profiles.profiles[i].getId().contentEquals(profile.getId())) {
                index = i;
            }
        }

        if (index == -1) {
            logger.error("Could not find profile with id: ", profile.getId(), "| skipping");
            return;
        }

        Profiles.profiles[index] = profile;
        Profiles.save();

    }

    public static String saveProfileImage(Profile profile, IplImage face, Consumer<Profile> postSave, boolean saveProfile, String specialPrefix) {

        Path path = Paths.get(Profiles.getModelsFolder());

        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String id = randomUUID().toString();
        String fileName = id + ".jpeg";

        if (specialPrefix == null) {
            profile.getModel().getData().add(id);

            IplImage iplImage = IplImage.create(225, 400, face.depth(), face.nChannels(), INTER_CUBIC);
            cvResize(face, iplImage);

            face = iplImage;
        } else {
            fileName = specialPrefix + "_" + fileName;
        }

        String filePath = path.resolve(fileName).toFile().getAbsolutePath();

        logger.blocks(
                "SAVING PROFILE IMAGE",
                "fileName", fileName,
                "filePath", filePath,
                "specialPrefix", specialPrefix,
                "profile", profile
        );

        final IplImage image = face;

        FeedsController.service.submit(() -> {
            try {
                cvSaveImage(filePath, image);

                if (saveProfile) {
                    Profiles.save();
                }
                if (postSave != null) {
                    postSave.accept(profile);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }


        });
        return id;
    }

    public static Profile createProfile(IplImage currentFace, IplImage profileImage, Image flexFrameImage, Mat croppedMat, int depth) {
        String id = UUID.randomUUID().toString();

        Name name = new Name(
                "Unknown",
                "",
                "#" + id.split("-")[0]
        );
        Date date = null;
        Attribute race = Attributes.getDefault("race");

        Attribute gender = GenderCNN.predict(croppedMat, depth).getResult();
        AgeBoundary ageBoundary = AgeCNN.predict(croppedMat, depth).getResult();

        if (ageBoundary != null) {
            int age = (ageBoundary.getUpper() + ageBoundary.getLower()) / 2;

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, age * -1);

            date = cal.getTime();
        }

        Identity identity = new Identity(
                name,
                date,
                gender,
                race
        );

        Role role = Roles.UNKNOWN;

        Model model = new Model(new ArrayList<>());
        Model galleryModel = new Model(new ArrayList<>());

        Profile profile = new Profile(
                id,
                identity,
                model,
                galleryModel,
                null,
                role,
                null
        );

        profile.setUnknown(true);

        Profiles.add(profile);

        saveProfileImage(profile, profileImage, null, true, Profile.PROFILE_IMAGE_PREFIX);

        String profileImageID = saveProfileImage(profile, currentFace, userProfile -> {
            train();

            Platform.runLater(() -> Core.getMainScreen().getController().reloadProfileList());
        }, true, null);

        profile.getModel().addWithImage(profileImageID, flexFrameImage);

        return profile;
    }

    public static Profile findProfileForId(String id) {
        for (Profile profile : profiles) {
            if (profile.getId().contentEquals(id)) {
                return profile;
            }
        }
        return null;
    }

    public static List<Image> loadModelImages(String prefix, Model model) {
        List<Image> images = new ArrayList<>();

        if(model == null) {
            return images;
        }


        if(prefix == null) {
            prefix = "";
        } else {
            prefix = prefix + "_";
        }

        Path root = Paths.get(getModelsFolder());

        List<String> toRemove = new ArrayList<>();
        for(String image : model.getData()) {
            Path imagePath = root.resolve(prefix + image + Profile.IMAGE_FORMAT);
            if(!Files.exists(imagePath)) {
                toRemove.add(image);
            } else {
                try {
                    images.add(new Image(imagePath.toUri().toString()));
                } catch (Exception e) {
                    logger.error("Could not load model image: ", image, null, e);

                    toRemove.add(image);
                }
            }
        }
        model.resolveImages(images);

        model.getData().removeAll(toRemove);


        return images;
    }

    public static void postLoad() {
        Arrays.asList(profiles).forEach(Profile::reloadGalleryImages);
        Arrays.asList(profiles).forEach(Profile::reloadProfileImage);
    }

    public static Profile findProfileForTrainingImage(String s) {
        String name = Common.removeFileExtension(s);
        String[] blocks = name.split("_");

        String blockType = "";
        String blockId = name;

        if(blocks.length > 1) {
            blockType = blocks[0];
            blockId = blocks[1];
        }

        final String id = blockId;
        final String type = blockType;

        List<Profile> profiles = Arrays.asList(Profiles.profiles);

        if (type.contentEquals(Profile.PROFILE_IMAGE_PREFIX)) {

            return profiles.stream()
                    .filter(profile -> profile.hasProfileImage() && profile.getProfileImageId().contentEquals(id))
                    .findFirst()
                    .orElse(null);
        }

        if (type.contentEquals(Profile.GALLERY_IMAGE_PREFIX)) {

            return profiles.stream()
                    .filter(profile -> profile.getGalleryImages() != null && profile.getGalleryImages().getData().contains(id))
                    .findFirst()
                    .orElse(null);
        }

        return profiles.stream()
                .filter(profile -> profile.getModel() != null && profile.getModel().getData().contains(id))
                .findFirst()
                .orElse(null);
    }
}
