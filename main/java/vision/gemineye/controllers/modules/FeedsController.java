package vision.gemineye.controllers.modules;

import com.jfoenix.concurrency.JFXUtilities;
import com.jfoenix.controls.JFXMasonryPane;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacpp.opencv_core.Mat;
import vision.gemineye.Common;
import vision.gemineye.Core;
import vision.gemineye.controllers.MainController;
import vision.gemineye.framework.OpenCVUtils;
import vision.gemineye.framework.tasker.Tasker;
import vision.gemineye.model.*;
import vision.gemineye.model.entity.Profile;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.TabController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.awt.Color.DARK_GRAY;
import static java.util.Arrays.asList;
import static javafx.scene.input.MouseEvent.*;
import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import static org.bytedeco.javacpp.opencv_features2d.drawKeypoints;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_FEATURE_MAX;
import static org.bytedeco.javacpp.opencv_xfeatures2d.*;

public class FeedsController implements TabController, Runnable, EventHandler<MouseEvent> {

    public static boolean DEBUG = false;

    private static OpenCVFrameConverter.ToMat toMatConverter;
    @FXML
    private ListView<String> feedsList;
    @FXML
    private ImageView feedsView;
    @FXML
    private AnchorPane feedsContainer;
    @FXML
    private JFXMasonryPane feedProfilePane;

    private static Logger logger = Logger.getLogger(FeedsController.class.getName());
    private static Java2DFrameConverter frameConverter;
    private static CvHaarClassifierCascade classifier;
    private static opencv_core.IplImage grabbedImage;
    private static opencv_core.IplImage currentFace;
    private static opencv_core.CvMemStorage storage;
    public static OpenCVFrameConverter.ToIplImage converter;
    private static OpenCVFrameConverter.ToIplImage converterAlt;
    private static FrameRecorder recorder;
    private static BFMatcher matcher = new BFMatcher(NORM_L2, true);

    public static ExecutorService service = Executors.newScheduledThreadPool(4);
    public static AtomicBoolean faces = new AtomicBoolean(false);
    private ImageView imageView;

    private HashMap<String, Node> profilesFeed = new HashMap<>();
    private Screen<MainController> mainController;

    public void attachMainController(Screen<MainController> mainController) {
        this.mainController = mainController;
    }


    public void setProfilesFeed(List<String> id) {
        List<String> toRemove = new ArrayList<>();
        for (String key : profilesFeed.keySet()) {
            if (!id.contains(key)) {
                toRemove.add(key);
            }
        }

        List<String> toAdd = new ArrayList<>();
        for (String key : id) {
            if (!profilesFeed.containsKey(key)) {
                toAdd.add(key);
            }
        }

        for (String key : toRemove) {
            feedProfilePane.getChildren().remove(profilesFeed.get(key));

            profilesFeed.remove(key);
        }

        feedProfilePane.limitColumnProperty().set(1);

        for (String key : toAdd) {
            Profile profile = Profiles.findProfileForId(key);
            if (profile == null) {
                continue;
            }

            String title = profile.getIdentity().getName().getFirst();
            Image pimage = profile.getProfileImage();

            ImageView imageView = new ImageView(pimage);
            imageView.setFitWidth(130);
            imageView.setFitHeight(130);
            // set a clip to apply rounded border to the original image.
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                    130, 130
            );

            clip.setArcWidth(150);
            clip.setArcHeight(150);
//            imageView.setClip(clip);
            imageView.setSmooth(true);

            // snapshot the rounded image.
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(javafx.scene.paint.Color.TRANSPARENT);
            WritableImage image = imageView.snapshot(parameters, null);

            // remove the rounding clip so that our effect can show through.
            imageView.setClip(null);

            // apply a shadow effect.
            imageView.setEffect(new DropShadow(100, javafx.scene.paint.Color.DARKGRAY));

            // store the rounded image in the imageView.
            imageView.setImage(image);

            VBox box = new VBox();
            Label label = new Label(title);
            label.setStyle("-fx-font-size: 16px;");
            label.setTextFill(javafx.scene.paint.Color.DIMGREY);
            box.getChildren().add(label);
            box.getChildren().add(imageView);
            label.setEffect(new DropShadow(1, javafx.scene.paint.Color.WHITE));

//            box.getChildren().add(imageView);

            box.setPrefWidth(130);
            box.setPrefHeight(130);

            feedProfilePane.getChildren().add(box);
            profilesFeed.put(key, box);
        }
    }

    @Override
    public void init(Screen screen) throws Exception {
        feedProfilePane.setCellWidth(150);
        feedProfilePane.setCellHeight(110);
        feedProfilePane.setVSpacing(10);

        feedsView.setOnMouseMoved(this);
        feedsView.setOnMousePressed(this);
        feedsView.setOnMouseReleased(this);
        feedsView.setOnMouseDragged(this);

        feedsList.getItems().add("Local WebCam");
        feedsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        feedsList.getSelectionModel().select(0);

        feedsView.fitWidthProperty().bind(feedsContainer.widthProperty());
        feedsView.fitHeightProperty().bind(feedsContainer.heightProperty());


        service.submit(this);
    }

    public void attachImageView(ImageView image) {
        this.imageView = image;
    }

    @Override
    public void onShowEvent() {
    }

    @Override
    public void onHideEvent() {
    }


    public IplImage getCurrentImage() {
        return grabbedImage;
    }

    public IplImage previousImage;

    public static void loadClassifer() {

        frameConverter = new Java2DFrameConverter();
        storage = CvMemStorage.create();

        toMatConverter = new OpenCVFrameConverter.ToMat();

        URL url = ClassLoader.getSystemClassLoader().getResource("classifier/haarcascade_frontalface_alt2.xml");
        File file = null;
        try {
            file = Loader.extractResource(url, null, "classifier", ".xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Loader.load(opencv_objdetect.class);

        classifier = new CvHaarClassifierCascade(
                cvLoad(file.getAbsolutePath())
        );

        try {
            Path folder = Paths.get(System.getenv("APPDATA"), "FlexVision");
            if (!Files.exists(folder)) {
                Files.createFile(folder);
            }

            Path outputVideo = folder.resolve("feed.avi");

            converter = new OpenCVFrameConverter.ToIplImage();

            FrameGrabber grabber = Core.getFrameGrabber();

            if (Core.isFeedless() || grabber == null) {
                logger.info("** Not recording because feedless");
                return;
            }

            IplImage iplImage = converter.convert(grabber.grab());
            int width = iplImage.width();
            int height = iplImage.height();

            storage = CvMemStorage.create();

            if (Core.isRecordless()) {
                logger.info("** Not recording because recordless");
                return;
            }

            recorder = FrameRecorder.createDefault(outputVideo.toAbsolutePath().toString(), width, height);
            recorder.start();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    SIFT detector = SIFT.create(100, 3, 0.04, 10, 1.6);


    public void powerwash() {
        List<Profile> profiles = asList(Profiles.getProfiles());
        profiles.forEach(profile -> {
            if (profile.state != null) {
                profile.state.powerwash(350, 300);
            }
        });

        Tasker.background(index -> {
            cvClearMemStorage(new CvMemStorage());
            System.gc();
        });


    }

    @Override
    public void run() {
        FrameGrabber grabber = Core.getFrameGrabber();

        if (Core.isFeedless() || grabber == null) {
            logger.info("** Not processing feeds because feedless");
            return;
        }


        try {
            Frame frame;
            while ((frame = Core.getFrameGrabber().grab()) != null) {
                long currentTime = System.currentTimeMillis();
                long updateLapse = currentTime - lastReset;

                if (lastReset <= 0) {
                    lastReset = System.currentTimeMillis();
                } else if (updateLapse >= 3000) {
                    powerwash();
                }

                List<Profile> profiles = asList(Profiles.getProfiles());

                IplImage iplImage = converter.convert(frame);

                grabbedImage = iplImage.clone();

                Mat outputAsMAT = new Mat(iplImage);

                Mat _output = new Mat();
                outputAsMAT.copyTo(_output);

                IplImage grayImage = opencv_core.IplImage.create(iplImage.width(), iplImage.height(), IPL_DEPTH_8U, 1);
                if (previousImage == null) {
                    previousImage = grayImage;
                }
                cvCvtColor(iplImage, grayImage, CV_BGR2GRAY);
//                cvEqualizeHist(grayImage, grayImage);

                Mat _image = new Mat(grayImage.clone());

                CvSeq faces = cvHaarDetectObjects(grayImage, classifier, storage,
                        1.1, 5, CV_HAAR_DO_CANNY_PRUNING | CV_HAAR_FEATURE_MAX);


                int total = faces.total();

                Set<String> annotedProfiles = new HashSet<>();

                LocalTime time = LocalTime.now();
                String feed = "Local Webcam";

                IplImage outputAsIPL = new IplImage(outputAsMAT);

//                System.out.println("FRAME");
                for (Profile profile : Profiles.getProfiles()) {
                    if (annotedProfiles.contains(profile.getId())
                            || profile.state == null) {
                        continue;
                    }


                    OpticalFlowResult result;

                    try {
                        Mat faceImage = _image.apply(profile.state.location);

                        Mat featureDescriptorList = new Mat();
                        KeyPointVector featureKeyPointList = new KeyPointVector();

                        detector.detect(faceImage, featureKeyPointList);
                        detector.compute(faceImage, featureKeyPointList, featureDescriptorList);

                        result = profile.state.calcOpticalFlowAndFindFeatures(
                                previousImage,
                                grayImage, featureKeyPointList, featureDescriptorList);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, Common.getStackTrace(e));

                        System.exit(-1);

                        profile.state.tracking = false;
                        return;
                    }

                    if (result == null || !result.isMatch()) {
                        profile.state.tracking = false;
                        continue;
                    }

                    annotedProfiles.add(profile.getId());

                    profile.state.onOpticalFlowCatch(result);

                    Rect bounds = result.getBoundingRectangle();
                    Mat face = _output.apply(bounds);
                    Mat grayFace = new Mat();
                    cvtColor(face, grayFace, COLOR_BGR2GRAY);

                    currentFace = new IplImage(grayFace.clone());

                    Mat faceFeatureDescriptorList = new Mat();

                    KeyPointVector faceFeatureKeyPointList = new KeyPointVector();
                    detector.detect(grayFace, faceFeatureKeyPointList);
                    detector.compute(grayFace, faceFeatureKeyPointList, faceFeatureDescriptorList);

                    addProfileToFeed(
                            profile,
                            face,
                            grayFace,
                            faceFeatureDescriptorList,
                            bounds,
                            outputAsMAT,
                            outputAsIPL,
                            result.x(),
                            result.y(),
                            result.w(),
                            result.h(),
                            faceFeatureKeyPointList
                    );

                    annotedProfiles.add(profile.getId());
                }


                previousImage = grayImage;

                Set<Profile> alreadyProvidedFor = new HashSet<>();
                for (int i = 0; i < total; i++) {
                    CvRect rect = new CvRect(cvGetSeqElem(faces, i));

                    int x = rect.x();
                    int y = rect.y();
                    int w = rect.width();
                    int h = rect.height();

                    Rect faceBoundingRectangle = new Rect(x, y, w, h);

                    java.util.stream.Stream<WeightedResult<Profile>> neighbors = profiles
                            .parallelStream()
                            .filter(profile -> !alreadyProvidedFor.contains(profile)
                                    && profile.state != null
                                    && profile.state.tracking
                            )
                            .map(profile -> new WeightedResult<Profile>(Common.getUnionRatio(profile.state.location, faceBoundingRectangle), profile))
                            .sorted((o1, o2) -> (int) (o2.getConfidence() - o1.getConfidence()));

                    WeightedResult<Profile> intercecting = neighbors.filter(result -> result.getConfidence() <= .25)
                            .findFirst()
                            .orElse(null);

                    if (intercecting != null && intercecting.getConfidence() <= .1) {
                        intercecting.getResult().state.location = faceBoundingRectangle;

                        alreadyProvidedFor.add(intercecting.getResult());
                    } else {
                        Mat face = _output.apply(faceBoundingRectangle);
                        Mat grayFace = new Mat();
                        cvtColor(face, grayFace, COLOR_BGR2GRAY);

                        Profile profile = Profiles.findProfileForMatrix(grayFace);

                        if (profile == null) {
                            if (intercecting != null) {
                                intercecting.getResult().state.location = faceBoundingRectangle;
                                alreadyProvidedFor.add(intercecting.getResult());
                            } else {

                                WeightedResult<Profile> nextBest = profiles
                                        .parallelStream()
                                        .filter(profilex -> !alreadyProvidedFor.contains(profilex)
                                                && profilex.state != null
                                                && profilex.state.tracking
                                        )
                                        .map(profilex -> new WeightedResult<Profile>(Common.getUnionRatio(profilex.state.location, faceBoundingRectangle), profilex))
                                        .sorted((o1, o2) -> (int) (o2.getConfidence() - o1.getConfidence()))
                                        .filter(result -> result.getConfidence() <= .8)
                                        .findFirst()
                                        .orElse(null);

                                if (nextBest != null) {
                                    nextBest.getResult().state.location = faceBoundingRectangle;
                                    alreadyProvidedFor.add(nextBest.getResult());
                                    continue;
                                }

                                CvScalar color = CvScalar.BLUE;
                                color.scale(.1);
//                                cvRectangle(outputAsIPL, cvPoint(x, y), cvPoint(x + w, y + h), color, 1, CV_AA, 0);
                            }

                            currentFace = new IplImage(grayFace.clone());
                            continue;
                        }


                        Mat faceFeatureDescriptorList = new Mat();

                        KeyPointVector faceFeatureKeyPointList = new KeyPointVector();
                        detector.detect(grayFace, faceFeatureKeyPointList);
                        detector.compute(grayFace, faceFeatureKeyPointList, faceFeatureDescriptorList);

                        if (profile.state == null) {
                            profile.state = new StateTracker();
                        }

                        profile.state.tracking = true;

                        if (!annotedProfiles.contains(profile.getId())) {

                            addProfileToFeed(
                                    profile,
                                    face,
                                    grayFace,
                                    profile.state.trackedDescriptors,
                                    profile.state.location,
                                    outputAsMAT,
                                    outputAsIPL,
                                    x, y, w, h,
                                    faceFeatureKeyPointList
                            );

                            annotedProfiles.add(profile.getId());
                        }

                        profile.state.onFaceDetectedWithClassifier(
                                faceBoundingRectangle,
                                faceFeatureKeyPointList,
                                faceFeatureDescriptorList
                        );

                        if (profile.state.isBaseLine(500)) {
                            Mat grayFaceALT = new Mat();
                            cvtColor(face, grayFaceALT, COLOR_BGR2GRAY);

                            int divisor = profile.state.baseLineAttempts;

                            int dx = 1;
                            int dy = 1;

                            if (divisor > 1) {
                                dx = (int) divisor;
                                dy = (int) divisor;
                            }

                            dx = Math.min(dx, 25);
                            dy = Math.min(dy, 25);

                            grayFaceALT = grayFaceALT.apply(new Rect(
                                            dx,
                                            dy,
                                            w - (dx * 2),
                                            h - (dy * 2)
                                    )
                            );

                            KeyPointVector altFaceFeatureKeyPointList = new KeyPointVector();
                            Mat altFaceFeatureDescriptorList = new Mat();
                            detector.detect(grayFaceALT, altFaceFeatureKeyPointList);
                            detector.compute(grayFaceALT, altFaceFeatureKeyPointList, altFaceFeatureDescriptorList);
                            profile.state.setBaseLine(altFaceFeatureKeyPointList, altFaceFeatureDescriptorList);
                        }
                    }

                }

                profiles = profiles
                        .stream()
                        .filter(profile -> profile.state != null
                                && !annotedProfiles.contains(profile.getId())
                                && !alreadyProvidedFor.contains(profile)
                        )
                        .collect(Collectors.toList());

                profiles.forEach(profile -> profile.state.onMissing());

                FeedsController.faces.set(total > 0);

                Snapshot snapshot = new Snapshot(new ArrayList<>(annotedProfiles), feed, time);
                Platform.runLater(() -> mainController.getController().broadcast(snapshot));

                Frame rotatedFrame = converter.convert(new IplImage(outputAsMAT));
                showImage(rotatedFrame);

                if (Core.isRecordless()) {
                    continue;
                }

                recorder.record(rotatedFrame);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();

            Screen.error(Core.getMainScreen(), Common.getStackTrace(e), true);
        } catch (Exception e) {
            e.printStackTrace();

            Screen.error(Core.getMainScreen(), Common.getStackTrace(e), true);
        }

    }

    private void showImage(Frame frame) {
        BufferedImage image = frameConverter.convert(frame);
        Image jfxImage = SwingFXUtils.toFXImage(image, null);
        this.feedsView.setImage(jfxImage);
    }

    @FXML
    private void openFeeds() {

    }

    @FXML
    private void openTargets() {

    }

    private long lastReset = -1;

    private void addProfileToFeed(Profile profile,
                                  Mat faceMAT,
                                  Mat faceMATGrey,
                                  Mat faceDescriptorList,
                                  Rect pointList,
                                  Mat outputFeedImage,
                                  IplImage outputFeedImageAsIPL, int x, int y, int w, int h, KeyPointVector... pointVectors) {


        FeedIdentifier identifier = profile.getFeedIdentifier();

        annotateProfile(outputFeedImage, profile, x, y, w, h, true, 0);
//        if(profile.state != null && profile.state.isChecked()) {
//            for (KeyPointVector vector : pointVectors) {
//                drawKeypoints(
//                        faceMATGrey,
//                        vector,
//                        faceMAT,
//                        Scalar.BLUE.mul(Scalar.all(.1)),
//                        opencv_features2d.DrawMatchesFlags.DRAW_OVER_OUTIMG
//                );
//            }
//        }

        if (profile.state.isChecked()) {
            for (int i = 0; i < profile.state.baseLineVectors.size(); i++) {
                KeyPointVector vector = profile.state.baseLineVectors.get(i);

                drawKeypoints(
                        faceMAT,
                        vector,
                        outputFeedImage.apply(new Rect(x, y, w, h)),
                        new Scalar(136D, 78D, 160D, 255D).mul(new Scalar(i * .1D, i * .2D, i * .3D, i * .4D)),
                        opencv_features2d.DrawMatchesFlags.DRAW_RICH_KEYPOINTS
                );
            }
        }


//        y += 20;
//
//        if (profile.state != null && profile.state.baseLineStrength <= 15) {
//            x -= 30;
//            w += 60;
//        } else if (profile.state != null && profile.state.baseLineStrength <= 30) {
//            x -= 20;
//            w += 40;
//        }

//        double ratio = Math.max(0, Math.min(100, ((double) (x + w / 2) / (double) outputFeedImageAsIPL.width()) * 100));
//        double ratioH = Math.max(0, Math.min(100, ((double) (y + h / 2) / (double) outputFeedImageAsIPL.height()) * 100));
//        double hue = Math.floor(100 - ratio) * 120 / 100;
//        double saturation = Math.floor(ratio - 50) / 50;
//
//        Color COLOR = Color.getHSBColor((float) hue, (float) saturation, .5F);
//
//        target = CV_RGB(COLOR.getRed(), COLOR.getBlue(), COLOR.getGreen());
//
//        int radius = (int) (Math.max(w/2D, h/2D) * 1.5);
//        int cw = radius * 2;
//        int ch = radius * 2;
//        int cx = (x + w / 2) - radius;
//        int cy = (y + h / 2) - ch / 4 - radius;
//
//        cvCircle(
//                outputFeedImageAsIPL,
//                cvPoint(x + w / 2, y + h / 2 - h / 4),
//                radius,
//                target,
//                5,
//                (int) (4 + (5 * ratioH)), 0
//        );
//
//        int lw = 100;
//        int lh = 15;
//        int lx = cx-lw/2+lw;
//        int ly = cy+ch-lh/2;
//
//        cvCircle(outputFeedImageAsIPL, new CvPoint(lx, ly - lh/2 - 10), 30, CV_RGB(255, 255, 255), 2, 0, 0);
//        putText(outputFeedImage,
//                profile.getInitals(),
//                new opencv_core.Point(lx - 20, ly-lh+5),
//                FONT_HERSHEY_SIMPLEX,
//                .8,
//                RGB(67, 126, 175)
//        );

//        cvLine(outputFeedImageAsIPL, cvPoint(x, y + h), cvPoint(x + w, y + h), target);
        CvScalar target = identifier.boundsStrokeColor;

//        y += 20;
//
//        if (profile.state != null && profile.state.baseLineStrength <= 15) {
//            x -= 30;
//            w += 60;
//        } else if (profile.state != null && profile.state.baseLineStrength <= 30) {
//            x -= 20;
//            w += 40;
//        }

        cvLine(outputFeedImageAsIPL, cvPoint(x, y + h), cvPoint(x + w, y + h), target);
    }


//    private CrossMatch crossMatch(Mat descriptorsLeft, Mat descriptorsRight) {
//        DMatchVector matches = new DMatchVector();
//
//        matcher.match(descriptorsLeft, descriptorsRight, matches);
//        matcher.crossMatch(descriptorsLeft, descriptorsRight, matches, 2);
//
//        long matchSize = matches.size();
//        int strength = 0;
//
//        for (long index = 0; index < matchSize; index++) {
//            DMatchVector matchVector = matches.get(index);
//
//            DMatch first = matchVector.get(0);
//            DMatch second = matchVector.get(1);
//
//            if (first.distance() < (.7 * second.distance())) {
//                strength++;
//            }
//
//        }
//
//        return new CrossMatch((int) matches.size(), matches);
//    }

    private void annotateProfile(Mat output, Profile profile, int x, int y, int w, int h, boolean classifier, int strength) {


        Scalar color = new Scalar(0 + (profile.state == null ? 0 : (profile.state.strikes > 2 ? 100 : 140)), 100, profile.state == null ? 100 : 100 + profile.state.strikes * 20, 1);

        String description = "Gender: " + profile.getIdentity().getGender().getTitle();
        if (profile.getIdentity().getDob() != null) {
            description += " | Age:" + profile.getIdentity().getAge();
        }
        putText(output, description, new opencv_core.Point(x, y - 35), FONT_HERSHEY_DUPLEX, .4, Scalar.CYAN);
        putText(output, profile.getIdentity().getName().getFirst().toUpperCase(), new opencv_core.Point(x, y - 55), FONT_HERSHEY_DUPLEX, .8, Scalar.GREEN);

    }

    public IplImage getCurrentFaceMatrix() {
        return currentFace;
    }

    @Override
    public void handle(MouseEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        List<Profile> profileList = asList(Profiles.getProfiles()).stream().filter(profile -> profile.state != null).collect(Collectors.toList());
        List<Profile> profilesInBounds = profileList.stream().filter(profile1 -> profile1.inLocation(x, y)).collect(Collectors.toList());
        List<Profile> profilesNotInBounds = profileList.stream().filter(profile1 -> !profile1.inLocation(x, y)).collect(Collectors.toList());

        if (event.getEventType().equals(MOUSE_MOVED)) {
            profilesInBounds.forEach(profiles -> {
                profiles.state.onMouseEnter(event);
                profiles.set(MOUSE_ENTERED_TARGET);
            });
        } else if (event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
            profilesInBounds.forEach(profiles -> {
                profiles.state.onMousePressed(event);
                profiles.set(MOUSE_PRESSED);
            });
        }

        if (event.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
            if (event.isShiftDown() && event.isControlDown()) {
                DEBUG = !DEBUG;
            }

            profileList.forEach(profiles -> {
                profiles.state.onMouseReleased(event);
                profiles.unset(MOUSE_PRESSED);
            });
        }

        for (Profile profile1 : profilesNotInBounds) {
            if (profile1.is(MOUSE_ENTERED_TARGET)) {
                profile1.unset(MOUSE_ENTERED_TARGET);
                profile1.state.onMouseLeave(event);
            }
        }

    }
}
