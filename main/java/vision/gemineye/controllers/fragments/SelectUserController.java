package vision.gemineye.controllers.fragments;

import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import vision.gemineye.model.Profiles;
import vision.gemineye.model.entity.Profile;
import vision.gemineye.model.ui.FragmentController;
import vision.gemineye.model.ui.Screen;

import java.util.ArrayList;
import java.util.List;

public class SelectUserController extends FragmentController<Profile, Profile> implements EventHandler<MouseEvent> {

    @FXML
    private Label statusLabel;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private JFXMasonryPane profilesPane;
    @FXML
    private AnchorPane rootAnchor;
    @FXML
    private VBox container;
    private int selection;

    @FXML
    private void confirm() {
        if(selection == -1) {
            statusLabel.setText("Please select a profile!");
            return;
        }
        StackPane pane = (StackPane) profilesPane.getChildren().get(selection);
        Profile profile = (Profile) pane.getProperties().get("profile");
        finish(profile);
    }

    @FXML
    private void cancel() {
        finish(null);
    }

    @Override
    public void init(Screen screen) throws Exception {
        profilesPane.setCellWidth(120);
        profilesPane.setCellHeight(120);
    }

    @Override
    public void start(Profile model) {
        Platform.runLater(() -> {
            selection = -1;

            statusLabel.setText("");

            profilesPane.getChildren().removeAll(profilesPane.getChildren());
            profilesPane.layout();

            ArrayList<Node> grid = new ArrayList<>();

            int indexOffset = 0;
            for (int i = 0; i < Profiles.getProfiles().length; i++) {
                Profile profile = Profiles.getProfiles()[i];
                if (model != null && model.getId() == profile.getId()) {
                    indexOffset++;
                    continue;
                }

                Image image = profile.getProfileImage();

                ImageView view = new ImageView(image);
                view.setFitHeight(90);
                view.setFitWidth(90);

                VBox box = new VBox();
                box.getChildren().add(new Label(profile.getTitle()));
                box.getChildren().add(view);

                box.setPrefSize(100, 110);

                StackPane imageViewWrapper = new StackPane(box);
                imageViewWrapper.getStyleClass().add("selectable");
                imageViewWrapper.getProperties().put("image", view);
                imageViewWrapper.getProperties().put("profile", profile);
                imageViewWrapper.setPrefSize(120, 120);
                imageViewWrapper.setMinSize(120, 120);

                view.getProperties().put("index", i - indexOffset);
                view.getProperties().put("wrapper", imageViewWrapper);

                grid.add(imageViewWrapper);

                view.addEventHandler(MouseEvent.MOUSE_CLICKED, this);
            }

            profilesPane.getChildren().addAll(grid);
            profilesPane.layout();

            container.layout();
        });

    }

    @Override
    public void handle(MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        if (node == null || !(node instanceof ImageView)) {
            return;
        }

        ObservableMap<Object, Object> properties = node.getProperties();

        int index = (int) properties.get("index");

        StackPane pane = (StackPane) profilesPane.getChildren().get(index);
        Profile profile = (Profile) pane.getProperties().get("profile");

        pane.setStyle("-fx-background-color:teal;");

        if(selection != -1) {
            StackPane otherPane = (StackPane) profilesPane.getChildren().get(selection);
            otherPane.setStyle("-fx-background-color:transparent;");
        }

        selection = index;

        statusLabel.setText("Selected " + profile.getTitle());

    }
}
