package vision.gemineye.controllers.modules;

import com.jfoenix.controls.JFXMasonryPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import vision.gemineye.model.Profiles;
import vision.gemineye.model.entity.Profile;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.TabController;

import java.util.*;


public class TargetsController implements TabController {

    @FXML
    private JFXMasonryPane restrictedPane;
    @FXML
    private JFXMasonryPane conflictsPane;
    @FXML
    private JFXMasonryPane curfewsPane;

    @FXML
    private ScrollPane restrictedScrollPane;
    @FXML
    private ScrollPane conflictsScrollPane;
    @FXML
    private ScrollPane curfewsScrollPane;


    private HashMap<String, Node> restricted = new HashMap<>();
    private HashMap<String, Node> conflicted = new HashMap<>();
    private HashMap<String, Node> curfews = new HashMap<>();

    public void updateTargets(
            List<String> restricted,
            List<String> conflicted,
            List<String> curfews
    ) {
        Platform.runLater(() -> {
           setProfilesFeed(restricted, this.restricted, restrictedPane, restrictedScrollPane);
           setProfilesFeed(conflicted, this.conflicted, conflictsPane, conflictsScrollPane);
           setProfilesFeed(curfews, this.curfews, curfewsPane, curfewsScrollPane);
        });

    }

    private void setProfilesFeed(List<String> id, HashMap<String, Node> feed, JFXMasonryPane pane, ScrollPane scrollPane) {
        Set<String> toRemove = new HashSet<>();
        for(String key : feed.keySet()) {
            if (!id.contains(key)) {
                toRemove.add(key);
            }
        }

        Set<String> toAdd = new HashSet<>();
        for(String key : id) {
            if(!feed.containsKey(key)) {
                toAdd.add(key);
            }
        }

        for(String key: toRemove) {
            pane.getChildren().remove(feed.get(key));

            feed.remove(key);
        }

        for(String key: toAdd) {
            Profile profile = Profiles.findProfileForId(key);
            if(profile == null) {
                continue;
            }

            String title = profile.getIdentity().getName().getFirst() + " (" + profile.getId().split("-")[0] + ")";
            Image image = profile.getProfileImage();

            ImageView view = new ImageView(image);
            view.setFitWidth(100);
            view.setFitHeight(110);

            VBox box = new VBox();
            Label label = new Label(title);
            box.getChildren().add(label);
            box.getChildren().add(view);

            box.setPrefWidth(150);
            box.setPrefHeight(110);

            pane.getChildren().add(box);
            feed.put(key, box);
        }

//        scrollPane.requestLayout();
    }

    @Override
    public void init(Screen screen) throws Exception {

    }

    @Override
    public void onShowEvent() {

    }

    @Override
    public void onHideEvent() {

    }
}
