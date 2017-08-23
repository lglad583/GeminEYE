package vision.gemineye.model;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2f;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacv.Frame;
import scala.collection.JavaConversions;
import vision.gemineye.Common;
import vision.gemineye.Core;
import vision.gemineye.framework.OpenCVUtils;

import java.util.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.KeyPointVector;
import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_core.abs;
import static org.bytedeco.javacpp.opencv_imgproc.goodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_video.calcOpticalFlowFarneback;
import static org.bytedeco.javacpp.opencv_video.calcOpticalFlowPyrLK;
import static vision.gemineye.framework.OpenCVUtils.*;

public class StateTracker {

    private static final double TRACK_QUALITY = .6;

    private static final int TRACK_FEATURE_LIMIT = 300;
    private static final int TRACK_MAX_CORNERS = 4000;
    private static final int TRACK_MIN_DISTANCE = 2;
    private static final int TRACK_BLOCK_DEPTH = 2;

    public Rect location;
    public List<FeaturePoint> initialPositions = new ArrayList<>();
    public List<FeaturePoint> trackedPoints = new ArrayList<>();
    public Mat trackedDescriptors;
    private boolean checked;
    public boolean tracking;
    private Mat trackedFeatures;
    private Frame attach;

    public Frame getAttach() {
        return attach;
    }


    public void setAttach(Mat attach) {
        final Mat attached = attach.clone();

        Platform.runLater(() -> {
            this.attach = Common.convert(attached);
            Core.getFeedsControllerScreen().getController().setProfilesFeed(new ArrayList<>());
        });
    }

    public boolean isTracking() {
        return tracking;
    }

    public void activityTracked(float amount) {

    }

    public void setTracking(boolean tracking) {
        if (tracking && !this.tracking) {
            lastTrack = System.currentTimeMillis();
        }

        if (!tracking && this.tracking) {
            lastTrack = 0;
        }

        this.tracking = tracking;
    }

    public StateTracker() {
    }


//    opencv_features2d.FlannBasedMatcher matcher = new opencv_features2d.FlannBasedMatcher();

    opencv_features2d.BFMatcher matcher = new opencv_features2d.BFMatcher(NORM_L2, true);


    public void onFaceDetectedWithClassifier(Rect location, KeyPointVector faceKeyPointList, Mat faceDescriptorList) {
        List<Point2f> features = toPointList(faceKeyPointList);
        List<FeaturePoint> point = new ArrayList<FeaturePoint>();

        for (int i = 0; i < features.size(); i++) {
            FeaturePoint feature = new FeaturePoint(
                    features.get(i),
                    faceKeyPointList.get(i)
            );

            point.add(feature);
        }

        onFaceDetectedWithClassifier(location, point, faceDescriptorList);

        this.location = location;
    }

    public void onFaceDetectedWithClassifier(Rect location, List<FeaturePoint> features, Mat faceDescriptorList) {
        this.location = location;


        if (trackedPoints.size() < TRACK_FEATURE_LIMIT) {


            if (this.trackedDescriptors == null) {
                this.trackedDescriptors = new Mat(faceDescriptorList);
            } else {
                DMatchVector matches = new DMatchVector();
                matcher.match(faceDescriptorList, trackedDescriptors, matches);

                Set<Integer> exclude = new HashSet<>();
                Mat mat = new Mat((int) (faceDescriptorList.rows() - matches.size()), 128, CV_64F);

                //remove matches
                for (long i = 0; i < matches.size(); i++) {
                    DMatch match = matches.get(i);
                    int index = match.queryIdx();

                    features.set(match.queryIdx(), null);

                    exclude.add(match.queryIdx());
                }

                features = features.stream().filter(Objects::nonNull).collect(Collectors.toList());

                int idx = 0;
                for (int i = 0; i < faceDescriptorList.rows(); i++) {
                    if (exclude.contains(Integer.valueOf(i))) {
                        continue;
                    }

                    faceDescriptorList.row(i).copyTo(mat.row(idx));

                    idx++;
                }

                initialPositions.addAll(features);
                trackedPoints.addAll(features);

                int start = trackedDescriptors.rows();

                trackedDescriptors.resize(mat.rows() + trackedDescriptors.rows());

                for (int i = 0; i < mat.rows(); i++) {
                    mat.row(i).copyTo(trackedDescriptors.row(start + i));
                }

            }
        }

        //TODO: smart expansion
    }

    private static List<Point2f> toPointList(KeyPointVector vector) {
        Point2fVector points = new Point2fVector();
        KeyPoint.convert(vector, points);
        Point2f[] pointArray = OpenCVUtils.toPoint2fArray(OpenCVUtils.toMat(points));
        return Arrays.asList(pointArray);
    }

    public void initTracking(Mat tracked, int x, int y) {
        trackedFeatures = new Mat();
        tracking = true;

        opencv_imgproc.goodFeaturesToTrack(tracked, // the image
                trackedFeatures, // the output detected state
                TRACK_MAX_CORNERS, // the maximum number of state
                TRACK_QUALITY, // quality level
                TRACK_MIN_DISTANCE // min distance between two state
        );
    }

    public void lock(Mat currentFrame, Mat previousFrame) {
        if (!tracking) {
            return;
        }

        Mat alpha = new Mat(
                trackedFeatures.rows(),
                trackedFeatures.cols(),
                CV_32F
        );
        trackedFeatures.copyTo(alpha);

        Mat beta = new Mat();
        Mat alphaError = new Mat();
        Mat alphaStatus = new Mat();

        Mat betaAnswers = new Mat();
        Mat betaError = new Mat();
        Mat betaStatus = new Mat();

        calcOpticalFlowPyrLK(
                previousFrame, currentFrame,
                alpha,
                beta,
                alphaStatus,
                alphaError
        );

        calcOpticalFlowPyrLK(
                currentFrame, previousFrame,
                beta,
                betaAnswers,
                betaStatus,
                betaError
        );

        Mat match = null;

        max(abs(subtract(alpha, betaAnswers)).asMat().reshape(-1, 2), -1);

    }

    public int strikes = 0;
    private int hits = 0;

    private transient long lastTrack;

    public float justTracked(long lapse) {
        if (lastTrack <= 0) {
            return 0;
        }

        long span = System.currentTimeMillis() - lastTrack;
        if (span >= lapse) {
            return (float) ((float) 1 - ((double) span / (double) lapse));
        }

        return 0;
    }

    public void powerwash(int level, int baseLineLevel) {

        if (trackedDescriptors != null && level < trackedDescriptors.rows()) {
            trackedDescriptors.pop_back(level);
            trackedDescriptors.resize(level);
        }
        if (trackedFeatures != null && level < trackedDescriptors.rows())
            trackedFeatures.resize(level);
        if (trackedPoints != null) {
            if (initialPositions.size() > level) {
                for (int i = 0; i < level; i++) {
                    initialPositions.remove(0);
                }
            }
            if (initialPositions.size() > level) {
                for (int i = 0; i < level; i++) {
                    initialPositions.remove(0);
                }
            }
        }

        if (trackedPoints != null && trackedPoints.size() > level) {
            trackedPoints = trackedPoints.subList(0, level);

            initialPositions = new ArrayList<>();
            initialPositions.addAll(trackedPoints);
        } else if (initialPositions != null && initialPositions.size() > level) {
            initialPositions = initialPositions.subList(0, level);
        }

        strikes = 0;
        lastTrack = 0;
        hits = 0;

        if (baseLineDescriptors != null) {
            baseLineLevel = Math.min(baseLineLevel, baseLineDescriptors.rows());
            baseLineCount = 0;
            baseLineAttempts = 0;
            if (baseLineDescriptors.rows() > baseLineLevel)
                baseLineDescriptors.resize(baseLineLevel);

            if (baseLineDescriptors != null && baseLineLevel < baseLineDescriptors.rows()) {
                Mat _baseLineDescriptors = new Mat(level, 128, CV_32F);
                for (int y = 0; y < baseLineLevel; y++) {
                    _baseLineDescriptors.row(y).setTo(baseLineDescriptors.row(y + baseLineLevel));
                }
                baseLineDescriptors = _baseLineDescriptors;
            }

            baseLineStrength = 0;
            if (baseLineVectors != null && baseLineVectors.size() > level) {
                baseLineVectors = baseLineVectors.subList(0, baseLineLevel);
            }
        }


    }

    public OpticalFlowResult calcOpticalFlowAndFindFeatures(IplImage previousImage, IplImage grayImage, KeyPointVector featureKeyPointList, Mat featureDescriptors) throws Exception {
        OpticalFlowResult result = new OpticalFlowResult();
        result.setBoundingRectangle(location);

        if (trackedPoints.isEmpty()) {
            return result;
        }

        Mat before = new Mat(previousImage);
        Mat after = new Mat(grayImage);
        Mat trackingStatus = new Mat();
        Mat trackedPointsNewUnfilteredMat = new Mat();
        Mat err = new Mat();
        Mat matFeatures = toMatPoint2f(JavaConversions.asScalaBuffer(trackedPoints.stream().map(point -> point.location).collect(Collectors.toList())));

        if (!confirmBaseLine(featureDescriptors)) {
            strikes++;

            if (hits < 15 || strikes >= 10) {
                strikes = 0;
                hits = 0;
                tracking = false;
                return result;
            }

        }

        DMatchVector matches = new DMatchVector();
        matcher.match(featureDescriptors, trackedDescriptors, matches);

        List<Point2f> movement = new ArrayList<>();
        for (long i = 0; i < matches.size(); i++) {
            DMatch match = matches.get(i);
            movement.add(featureKeyPointList.get(match.queryIdx()).pt());
        }

//
//        calcOpticalFlowFarneback(
//                before,
//                after, // 2 consecutive images
//                trackedPointsNewUnfilteredMat, // output point position in the second image
//                0.7, 3, 11, 5, 5, 1.1, 0
//        );

        calcOpticalFlowPyrLK(
                before,
                after, // 2 consecutive images
                matFeatures, // input point position in first image
                trackedPointsNewUnfilteredMat, // output point position in the second image
                trackingStatus, // tracking success
                err // tracking error
        );

        if (trackedPoints.size() < 2) {
            tracking = false;
            strikes = 0;
            hits = 0;
            return result;
        }

        Point2f[] trackedPointsNewUnfiltered = OpenCVUtils.toPoint2fArray(trackedPointsNewUnfilteredMat);
        ArrayList<FeaturePoint> initialPositionsNew = new ArrayList<>();
        ArrayList<FeaturePoint> trackedPointsNew = new ArrayList<>();
        ArrayList<Mat> colList = new ArrayList<>();
        UByteRawIndexer trackingStatusIndexer = trackingStatus.createIndexer();

        int matchCount = 0;

        for (int i = 0; i < trackedPointsNewUnfiltered.length; i++) {
            if (acceptTrackedPoint(
                    trackingStatusIndexer.get(i),
                    trackedPoints.get(i).location,
                    trackedPointsNewUnfiltered[i])) {
                initialPositionsNew.add(initialPositions.get(i));
                trackedPointsNew.add(new FeaturePoint(
                        trackedPointsNewUnfiltered[i],
                        trackedPoints.get(i).getKey()
                ));
                colList.add(trackedDescriptors.row(i));
                matchCount++;
            }
        }


        if (matchCount <= 5) {
            strikes++;

            if (hits < 15 || strikes >= 40) {
                strikes = 0;
                hits = 0;
                tracking = false;
                return result;
            }
        }


//        System.out.println(matchCount);

        if (matchCount < 50) {
            lastTrack = System.currentTimeMillis();
        }

        tracking = true;
        hits++;

        result.setMatch(true);
        result.setBoundingRectangle(location);

        Mat trackedDescriptors = new Mat(matchCount, 128, CV_32F);
        for (int i = 0; i < matchCount; i++) {
            colList.get(i).copyTo(trackedDescriptors.row(i));
        }

        onFaceDetectedWithClassifier(location, trackedPointsNew, trackedDescriptors);

        return result;
    }

    private boolean confirmBaseLine(Mat featureDescriptors) {
        DMatchVector matches = new DMatchVector();
        if (baseLineDescriptors == null || baseLineCount <= 100) {
            return true;
        }

        matcher.match(featureDescriptors, baseLineDescriptors, matches);

        baseLineStrength = (int) matches.size();

        return matches.size() >= 9;
    }

    private Mat toPointMat(KeyPointVector featureKeyPointList) {
        Point2fVector points = new Point2fVector();
        KeyPoint.convert(featureKeyPointList, points);
        return toMat(points);
    }

    public boolean acceptTrackedPoint(int status, Point2f point0, Point2f point1) {
        return status != 0 &&
                (abs(point0.x() - point1.x()) + abs(point0.y() - point1.y()) > 1);
    }

    public void onOpticalFlowCatch(OpticalFlowResult result) {
        this.location = result.getBoundingRectangle();
    }

    public void onMissing() {
//        this.location = null;
        this.tracking = false;
//        this.location = null;
    }

    public void onMouseEnter(MouseEvent event) {
    }

    public void onMousePressed(MouseEvent event) {
    }

    public void onMouseReleased(MouseEvent event) {
        setChecked(!isChecked());

        System.out.println("checked");
    }

    public void onMouseLeave(MouseEvent event) {
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public KeyPointVector toKeypointVector() {
        KeyPointVector vector = new KeyPointVector();
        trackedPoints.forEach(point -> vector.put(point.getKey()));

        return vector;
    }

    public boolean isBaseLine(int value) {
        return this.baseLineCount <= value;
    }

    public int baseLineCount;
    private Mat baseLineDescriptors;
    public int baseLineAttempts;
    public int baseLineStrength;
    public List<KeyPointVector> baseLineVectors = new ArrayList<>();

    public void setBaseLine(KeyPointVector baseLineVector, Mat faceFeatureDescriptorList) {

        faceFeatureDescriptorList = faceFeatureDescriptorList.clone();

        if (faceFeatureDescriptorList.rows() > 50) {
            faceFeatureDescriptorList.resize(50);
        }

        baseLineVectors.add(baseLineVector);

        int start = 0;
        if (this.baseLineDescriptors == null) {
            this.baseLineDescriptors = new Mat(faceFeatureDescriptorList.rows(), 128, CV_32F);
        } else {
            start = this.baseLineDescriptors.rows();
        }
        this.baseLineDescriptors.resize(faceFeatureDescriptorList.rows() + start);

        for (int i = 0; i < faceFeatureDescriptorList.rows(); i++) {
            faceFeatureDescriptorList.row(i).copyTo(this.baseLineDescriptors.row(i + start));
        }
        baseLineAttempts++;
        baseLineCount += faceFeatureDescriptorList.rows();
    }
}
