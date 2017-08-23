package vision.gemineye.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.ScreenController;


public class SplashController implements ScreenController {

    @FXML
    private ImageView icon;
    @FXML
    private Label label;

    public void updateLabel(String message) {
        Platform.runLater(() -> label.setText(message));
    }

    public void init(Screen screen) throws Exception {
        this.icon.setImage(new Image(Screen.getPNGAssetURL("48_logo").toString()));
    }

}
