package vision.gemineye.controllers.modules;

import com.jfoenix.controls.*;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import vision.gemineye.Common;
import vision.gemineye.controllers.MainController;
import vision.gemineye.controllers.fragments.ManageUserController;
import vision.gemineye.controllers.fragments.labeled.LabeledListController;
import vision.gemineye.controllers.fragments.SelectUserController;
import vision.gemineye.controllers.fragments.TrainingImageData;
import vision.gemineye.framework.AppLogger;
import vision.gemineye.framework.tasker.Tasker;
import vision.gemineye.model.*;
import vision.gemineye.controllers.fragments.labeled.Labeled;
import vision.gemineye.model.entity.*;
import vision.gemineye.model.ui.TabController;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.UserTrainAttachment;
import vision.gemineye.tasks.ProfileDeleteTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExplorerController implements TabController, ChangeListener<Profile>, EventHandler<MouseEvent> {

    private static AppLogger logger = AppLogger.get(ExplorerController.class);

    @FXML
    private HBox trainingManageContainer;
    @FXML
    private Label trainLabel;
    @FXML
    private HBox profilePane;
    @FXML
    private JFXTextField filterTextField;
    @FXML
    private JFXComboBox<Role> roleComboBox;
    @FXML
    private JFXComboBox<Attribute> raceComboBox;
    @FXML
    private ListView<Profile> profileList;
    @FXML
    private JFXSlider ageStartSlider;
    @FXML
    private JFXSlider ageEndSlider;
    @FXML
    private JFXTextField nameTextField;
    @FXML
    private VBox selectedState;
    @FXML
    private VBox emptyState;
    @FXML
    private HBox createUserPanel;
    @FXML
    private HBox advancedSearchHBox;
    @FXML
    private HBox searchHBox;
    @FXML
    private VBox toolsVBox;
    @FXML
    private Label userLabel;
    @FXML
    private JFXMasonryPane imagePane;
    @FXML
    private VBox trainingDataContainer;
    @FXML
    private ScrollPane imageScrollPane;
    @FXML
    private ImageView profileImage;
    @FXML
    private Label idLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label firstNameLabel;
    @FXML
    private Label middleNameLabel;
    @FXML
    private Label lastNameLabel;
    @FXML
    private Label raceLabel;
    @FXML
    private Label genderLabel;
    @FXML
    private Label dobLabel;
    @FXML
    private JFXButton removeTrainingDataButton;
    @FXML
    private JFXButton moveTrainingDataButton;
    @FXML
    private Label statusTrainingLabel;

    private String originalTrainingLabelText;
    private Screen<ManageUserController> manageUserScreen;
    private Screen<TrainingImageData> trainingScreen;
    private Screen<FeedsController> feedsScreen;
    private Screen<SelectUserController> selectUserFragment;
    private Screen<LabeledListController> selectImages;

    private List<Profile> profiles = new ArrayList<>();

    private MainController mainController;
    private int lastIndexSelected = -1;

    private Set<Integer> selectedModelData = new HashSet<>();

    public void attachFeedsController(Screen<FeedsController> controller) {
        this.feedsScreen = controller;
    }

    private static AtomicBoolean started = new AtomicBoolean(false);

    @FXML
    public void mergeProfile() {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();

        if (profile == null) {
            System.out.println("PROFILE IS NULL, NOT ADDING");
            return;
        }

        if (Profiles.getProfiles().length <= 1) {
            return;
        }

        selectUserFragment.showAsFragment(Profile.class, profile, selection -> {
            if (selection == null) {
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Profile Merge");
            alert.setHeaderText("The profile " + profile.getTitle() + " will be deleted. All training data will be added to the profile " + selection.getTitle());

            alert.setContentText("All profile information including historical data will be lost. Are you sure?");

            Optional<ButtonType> result = alert.showAndWait();
            if (!result.isPresent()) {
                return;
            }

            if (result.get() != ButtonType.OK) {
                return;
            }

            List<ModelEntry> images = deleteSelectedImages(false, null);

            AtomicInteger integer = new AtomicInteger(images.size());

            Consumer<Profile> finishConsumer = profileConsumtion -> {
                int index = integer.addAndGet(-1);
                if (integer.addAndGet(-1) != 0) {
                    return; //Waiting for other images to save
                }
                logger.info("RETRAINED MODEL!");

                Profiles.train();
            };

            for (ModelEntry entry : images) {
                Profiles.saveProfileImage(selection, entry.getImage(), finishConsumer, false, null);

                selection.getModel().getResolvedImages().add(entry.getLoadedImage());
            }

            currentProfile = null;

            select(selection);
            Profiles.delete(profile);

            Platform.runLater(() -> setProfiles(Profiles.getProfiles()));

        });
    }

    @FXML
    public void onDeleteTrainingDataClick() {
        deleteSelectedImages(true, selectedModelData);
    }

    private List<ModelEntry> deleteSelectedImages(boolean save, Set<Integer> selectedModelData) {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();
        List<ModelEntry> images = new ArrayList<>();

        if (profile == null) {
            System.out.println("PROFILE IS NULL, NOT DELETING");
            return images;
        }

        if (selectedModelData == null) {
            selectedModelData = new HashSet<>();
            for (int i = 0; i < profile.getModel().getResolvedImages().size(); i++) {
                selectedModelData.add(i);
            }
        }

        images.addAll(profile.getModel().getResolvedImageMats(selectedModelData));

        List<Node> nodes = new ArrayList<>();
        for (Integer index : selectedModelData) {
            nodes.add(imagePane.getChildren().get(index));
        }
        profile.getModel().removeAll(selectedModelData);

        Platform.runLater(() -> imagePane.getChildren().removeAll(nodes));

        if (save) {
            Profiles.train();
            Profiles.save();
        }

        return images;
    }

    @FXML
    public void onMoveTrainingDataClick() {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();

        if (profile == null) {
            System.out.println("PROFILE IS NULL, NOT MOVING");
            return;
        }

        selectUserFragment.showAsFragment(Profile.class, profile, selection -> {
            if (selection == null) {
                return;
            }

            List<ModelEntry> images = deleteSelectedImages(false, selectedModelData);

            AtomicInteger integer = new AtomicInteger(images.size());

            Consumer<Profile> finishConsumer = profileConsumtion -> {
                int index = integer.addAndGet(-1);
                if (integer.addAndGet(-1) != 0) {
                    return; //Waiting for other images to save
                }
                logger.info("RETRAINED MODEL!");

                Profiles.train();
            };

            for (ModelEntry entry : images) {
                Profiles.saveProfileImage(selection, entry.getImage(), finishConsumer, false, null);

                selection.getModel().getResolvedImages().add(entry.getLoadedImage());
            }

            currentProfile = null;
            select(profile);

            Profiles.save();
        });
    }

    @Override
    public void init(Screen screen) throws Exception {
        selectUserFragment = Screen.fragment("select-user", "Select user");

        imagePane.setCellWidth(120);
        imagePane.setCellHeight(120);

        profileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        profileList.getSelectionModel().selectedItemProperty().addListener(this);

        disableSearchTools();

        originalTrainingLabelText = trainLabel.getText();

        emptyState.visibleProperty().bind(profileList.getSelectionModel().selectedItemProperty().isNull());
        selectedState.visibleProperty().bind(profileList.getSelectionModel().selectedItemProperty().isNotNull());


        manageUserScreen = Screen.fragment("manage-user", "Profile");
        trainingScreen = Screen.load("fragment/trainImages", "Train User", 884, 827);
        selectImages = Screen.fragment("labeled-list", "Import Training Data");


        setProfiles(Profiles.getProfiles());

        filterTextField.textProperty().addListener(change -> filter(filterTextField.getText()));

        started.set(true);
    }

    public void filter(String text) {
        profileList.itemsProperty().get().clear();

        Profile selectedProfile = profileList.getSelectionModel().getSelectedItem();

        text = text.trim().toUpperCase();
        if (text.isEmpty()) {
            profileList.itemsProperty().get().addAll(profiles);
        } else {
            List<Profile> matchingProfiles = new ArrayList<>();

            for (Profile profile : profiles) {
                if (profile.toString().toUpperCase().contains(text)) {
                    matchingProfiles.add(profile);
                }
            }

            profileList.itemsProperty().get().addAll(matchingProfiles);
        }

        if (selectedProfile != null
                && profileList.getItems().contains(selectedProfile)) {
            profileList.getSelectionModel().select(selectedProfile);
            return;
        }

        if (profileList.getSelectionModel().getSelectedItem() == null
                && !profileList.getItems().isEmpty()) {
            profileList.getSelectionModel().select(0);
            return;
        }

        select(profileList.getSelectionModel().getSelectedItem());

    }

    public void setProfiles(Profile[] profiles) {
        this.profiles.clear();
        this.profiles.addAll(Arrays.asList(profiles));

        filter(filterTextField.getText());
    }

    public void assureProfile() {
        filter(filterTextField.getText());

        select(profileList.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void onTrainImageClick() {

    }

    @FXML
    public void importFaceImage() {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();

        if (profile == null) {
            System.out.println("PROFILE IS NULL, NOT IMPORTING");
            return;
        }

        Common.selectImageFiles("Images containing " + profile.getTitle(), images -> {
            if (images == null || images.isEmpty()) {
                return;
            }

            List<Image> fxImageList = images.stream().map(image -> {
                try {
                    return new Image(new FileInputStream(image));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }).filter(image -> image != null)
                    .collect(Collectors.toList());

            List<Labeled> labelList = fxImageList.stream().map(image -> new ModelLabel(profile, image)).collect(Collectors.toList());

            selectImages.showAsFragment(List.class, labelList, selection -> {

            });

        });
    }

    @FXML
    public void trainModel() {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();

        if (profile == null) {
            System.out.println("PROFILE IS NULL, NOT TRAINING");
            return;
        }

        trainingScreen.showAsFragment(
                new UserTrainAttachment(
                        feedsScreen.getController(),
                        profile
                ), result -> Platform.runLater(() -> {
                    currentProfile = null;
                    select(profile);
//
//                    mainController.resetProfile();
//                    mainController.reloadProfileList();
//
//                    if (currentProfile == profile) {
//                        currentProfile = null;
//
//                        select(profile);
//                    }
                }));
    }

    @FXML
    public void deleteProfile() {
        List<Profile> profilesToDelete = profileList.getSelectionModel().getSelectedItems();
        if (profilesToDelete.isEmpty()) {
            return;
        }

        profiles.removeAll(profilesToDelete);

        StringBuilder message = new StringBuilder();

        if (profilesToDelete.size() == 1) {
            Profile profile = profilesToDelete.get(0);

            message.append("This action will delete the profile belonging to "
                    + profile.getIdentity().getName().getFirst()
                    + ", "
                    + profile.getIdentity().getName().getLast());
        } else {
            message.append("The following profiles will be deleted - ");

            for (int i = 0; i < Math.min(5, profilesToDelete.size()); i++) {
                Profile profile = profilesToDelete.get(i);

                message.append("\n");
                message.append(profile.getTitle());
            }
            if (profilesToDelete.size() > 5) {
                message.append("... and ").append(profilesToDelete.size() - 5).append(" more.");
            }
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Profile Deletion");
        alert.setHeaderText(message.toString());

        alert.setContentText("All profile information including historical data will be lost. Are you sure?");

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) {
            return;
        }

        if (result.get() == ButtonType.OK) {
            Tasker.async(new ProfileDeleteTask(profilesToDelete),
                    complete -> {
                        Profiles.save();
                        Profiles.train();

                        Platform.runLater(() -> mainController.reloadProfileList());


                    }
            );
        }
    }

    @FXML
    public void editProfile() {
        Profile profile = profileList.getSelectionModel().selectedItemProperty().get();

        if (profile == null) {
            return;
        }

        manageUserScreen.showAsFragment(
                Profile.class,
                profile,
                result -> {
                    if (result == null) {
                        return;
                    }
                    Profiles.save(result);

                    setProfiles(Profiles.getProfiles());

                    if (currentProfile != null && currentProfile.getId().contentEquals(result.getId())) {
                        profileList.getSelectionModel().select(profile);

                        currentProfile = null;

                        select(result);
                    }
                }
        );
    }

    @FXML
    public void addUser() {
        manageUserScreen.showAsFragment(
                Profile.class,
                profile -> {
                    if (profile == null) {
                        return;
                    }
                    Profiles.add(profile);

                    profileList.getSelectionModel().clearSelection();
                    profileList.getSelectionModel().select(profile);
                }
        );
    }

    @FXML
    public void disableSearchTools() {
        toolsVBox.getChildren().removeAll(advancedSearchHBox, searchHBox);
        toolsVBox.getChildren().add(0, searchHBox);
    }

    @FXML
    public void enableSearchTools() {
        toolsVBox.getChildren().removeAll(advancedSearchHBox, searchHBox);
        toolsVBox.getChildren().add(0, advancedSearchHBox);
    }

    @Override
    public void onShowEvent() {
        filterTextField.requestFocus();
        imageScrollPane.requestLayout();
    }

    @Override
    public void onHideEvent() {
    }

    public static boolean isStarted() {
        return started.get();
    }

    private Profile currentProfile = null;

    private void select(Profile profile) {
        Platform.runLater(() -> {
            if (profile == null) {
                return;
            }

            if (!profile.hasProfileImage()) {
                profilePane.getChildren().remove(profileImage);
            } else {
                profileImage.setImage(profile.getProfileImage());

                if (!profilePane.getChildren().contains(profileImage)) {
                    profilePane.getChildren().add(profileImage);
                }
            }

            if (currentProfile == profile) {
                return;
            }

            trainingDataContainer.getChildren().remove(trainingManageContainer);
            selectedModelData.clear();
            imagePane.getChildren().removeAll(imagePane.getChildren());
            imagePane.layout();

            moveTrainingDataButton.setVisible(false);
            removeTrainingDataButton.setVisible(false);
            statusTrainingLabel.setText("");
            lastIndexSelected = -1;

            currentProfile = profile;

            userLabel.setText(profile.getTitle());

            idLabel.setText(profile.getId().split("-")[0]);
            roleLabel.setText(profile.getRole().getTitle());
            firstNameLabel.setText(profile.getIdentity().getName().getFirst());
            middleNameLabel.setText(profile.getIdentity().getName().getMiddleAsText());
            lastNameLabel.setText(profile.getIdentity().getName().getLast());
            raceLabel.setText(profile.getIdentity().getRace().getTitle());
            genderLabel.setText(profile.getIdentity().getGender().getTitle());
            dobLabel.setText(profile.getIdentity().getDobAsText());

            if (!profile.hasTrainingImages()) {
                selectedState.getChildren().remove(trainingDataContainer);

                trainLabel.setText("");
            } else {
                trainLabel.setText(originalTrainingLabelText + " (" + profile.getModel().getData().size() + ")");

                if (!selectedState.getChildren().contains(trainingDataContainer))
                    selectedState.getChildren().add(trainingDataContainer);

                ArrayList<Node> grid = new ArrayList<>();

                List<Image> images = profile.getModel().getResolvedImages();
                for (int i = 0; i < images.size(); i++) {

                    Image image = images.get(i);

                    ImageView view = new ImageView(image);
                    view.setFitHeight(110);
                    view.setFitWidth(110);

                    StackPane imageViewWrapper = new StackPane(view);
                    imageViewWrapper.getStyleClass().add("selectable");
                    imageViewWrapper.getProperties().put("image", view);
                    imageViewWrapper.setPrefSize(110, 110);
                    imageViewWrapper.setMinSize(110, 110);
//
                    view.getProperties().put("index", i);
                    view.getProperties().put("wrapper", imageViewWrapper);

                    grid.add(imageViewWrapper);

                    JFXDepthManager.setDepth(imageViewWrapper, 1);

                    view.addEventHandler(MouseEvent.MOUSE_CLICKED, this);

                }

                imagePane.getChildren().addAll(grid);
                trainingDataContainer.layout();

                JFXScrollPane.smoothScrolling(imageScrollPane);
            }

        });

    }

    @Override
    public void changed(ObservableValue<? extends Profile> observable,
                        Profile oldValue,
                        Profile newValue) {
        select(newValue);
    }

    public void attachMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void toggleModelSelection(int index, boolean state) {

        StackPane pane = (StackPane) imagePane.getChildren().get(index);
        ImageView view = (ImageView) pane.getProperties().get("image");

        ObservableMap<Object, Object> properties = view.getProperties();

        if (state) {
            selectedModelData.add(index);
            properties.put("selected", true);
            pane.setStyle("-fx-background-color:teal;");
        } else {
            selectedModelData.remove(Integer.valueOf(index));
            properties.remove("selected");
            pane.setStyle("-fx-background-color:transparent;");
        }

        if (selectedModelData.isEmpty()) {
            statusTrainingLabel.setText("");
            moveTrainingDataButton.setVisible(false);
            removeTrainingDataButton.setVisible(false);

            trainingDataContainer.getChildren().remove(trainingManageContainer);

        } else {
            moveTrainingDataButton.setVisible(true);
            removeTrainingDataButton.setVisible(true);
            statusTrainingLabel.setText(selectedModelData.size() + " selected");

            if (!trainingDataContainer.getChildren().contains(trainingManageContainer)) {
                trainingDataContainer.getChildren().add(trainingManageContainer);
            }
        }
    }

    @Override
    public void handle(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        if (node == null || !(node instanceof ImageView)) {
            return;
        }

        ObservableMap<Object, Object> properties = node.getProperties();

        int index = (int) properties.get("index");
        boolean isSelected = properties.containsKey("selected");

        if (lastIndexSelected != -1 && event.isShiftDown()) {
            int lower = Math.min(lastIndexSelected, index);
            int higher = Math.max(lastIndexSelected, index);

            for (int i = 0; i < lower; i++) {
                toggleModelSelection(i, false);
            }
            for (int i = higher + 1; i < imagePane.getChildren().size(); i++) {
                toggleModelSelection(i, false);
            }
            for (int i = lower; i <= higher; i++) {
                toggleModelSelection(i, true);
            }
        } else {
            toggleModelSelection(index, !isSelected);

            lastIndexSelected = index;
        }

    }
}
