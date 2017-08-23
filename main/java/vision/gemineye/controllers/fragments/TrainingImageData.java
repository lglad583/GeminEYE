package vision.gemineye.controllers.fragments;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import vision.gemineye.Common;
import vision.gemineye.Core;
import vision.gemineye.controllers.modules.FeedsController;
import vision.gemineye.framework.AppLogger;
import vision.gemineye.model.Profiles;
import vision.gemineye.model.entity.Profile;
import vision.gemineye.model.ui.UserTrainAttachment;
import vision.gemineye.model.entity.Model;
import vision.gemineye.model.ui.FragmentController;
import vision.gemineye.model.ui.Screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bytedeco.javacpp.opencv_core.IplImage;

import static java.util.UUID.randomUUID;
import static org.bytedeco.javacpp.opencv_core.cvFlip;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;

public class TrainingImageData extends FragmentController<UserTrainAttachment, Model> {
    private Screen<TrainingImageData> screen;
    private static AppLogger logger = AppLogger.get(TrainingImageData.class);

    @FXML
    private AnchorPane anchor;
    @FXML
    private JFXButton startButton;
    @FXML
    private JFXButton closeButton;

    @FXML
    private Label statusLabel;
    @FXML
    private ImageView camera;
    private UserTrainAttachment attachment;
    @FXML
    private VBox parentContainer;

    @Override
    public void init(Screen screen) throws Exception {
        camera.fitWidthProperty().bind(parentContainer.widthProperty());
    }

    @Override
    public void start(UserTrainAttachment attachment) {
        Platform.runLater(() -> statusLabel.setText("When you're ready, hit start."));

        this.attachment = attachment;

        this.attachment.getFeedsController().attachImageView(camera);
    }

    public void closeFrame() {
        finish(null);
    }

    private final static int IMAGE_COUNT = 11;

    public void captureUser() {

        FeedsController controller = attachment.getFeedsController();
        Profile profile = attachment.getProfile();

        FeedsController.service.submit(() -> {
            try {

                AtomicInteger integer = new AtomicInteger(IMAGE_COUNT);

                Consumer<Profile> finishConsumer = profileConsumtion -> {
                    if (integer.addAndGet(-1) != 0) {
                        return; //Waiting for other images to save
                    }

                    profile.reloadTrainingImages();

                    FeedsController.service.submit(() -> {
                        Profiles.train();

                        Platform.runLater(() -> finish(null));
                    });

                };

                try {
                    for (int i = 0; i < IMAGE_COUNT; i++) {
                        final int index = i;

                        while (!FeedsController.faces.get()) {
                            Platform.runLater(() -> statusLabel.setText("No face found. \n\nPlease make sure you are in front of your camera and your entire face is visible."));

                            Thread.sleep(200L);
                        }

                        if (profile.hasProfileImage()) {
                            Platform.runLater(() -> statusLabel.setText("Grabbing Image #" + (index + 1) + " image"));
                        } else {
                            Platform.runLater(() -> statusLabel.setText("Creating Profile Image"));
                        }

                        Profiles.saveProfileImage(profile, controller.getCurrentFaceMatrix(), finishConsumer, true, null);

                        if (!profile.hasProfileImage()) {
                            String profileImage = Profiles.saveProfileImage(
                                    profile,
                                    controller.getCurrentImage(),
                                    consumedProfile -> {
                                        profile.reloadProfileImage();

                                        Profiles.save();
                                    },
                                    false, Profile.PROFILE_IMAGE_PREFIX
                            );

                            profile.setProfileImageId(profileImage);

                            Profiles.save();

                        }

                        Thread.sleep(500);

                        Platform.runLater(() -> statusLabel.setText("Get ready for the next image"));

                        Thread.sleep(500);
                    }


                    Platform.runLater(() -> {
                        statusLabel.setText("Retraining System\n\nThis may take up to a minute.");
                    });

                } catch (Exception e) {

                    logger.error(
                        "STEP:", integer.get(),
                        e
                    );

                    finish(null);



                    Screen.error(Core.getMainScreen(),  Common.getStackTrace(e), false);
                }

            } catch (Exception e) {
                e.printStackTrace();

                finish(null);

                Screen.error(Core.getMainScreen(), Common.getStackTrace(e), false);
            }
        });


    }
}

