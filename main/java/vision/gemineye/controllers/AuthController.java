package vision.gemineye.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.ScreenController;
import vision.gemineye.model.entity.Profile;

public class AuthController implements ScreenController {

    private Screen<AuthController> screen;
    private Screen<AuthTextController> authTextScreen;
    private Screen<AuthFaceController> authFaceController;
    private Screen<MainController> mainScreen;

    public void attachMainScreen(Screen<MainController> mainScreen) {
        this.mainScreen = mainScreen;
    }

    @FXML
    private VBox authContainerVBOX;

    @Override
    public void init(Screen screen) throws Exception {
        this.screen = screen;

        authTextScreen = Screen.load("auth/auth-text");
        authFaceController = Screen.load("auth/auth-face");

        authTextScreen.getController().attachAuthScreen(screen);
        authFaceController.getController().attachAuthScreen(screen);

        toAuthMode(false);
    }

    public void toAuthMode(boolean face) {
        authContainerVBOX.getChildren().removeAll(authTextScreen.getNode(), authFaceController.getNode());

        if(!face) {
            authContainerVBOX.getChildren().add(authTextScreen.getNode());
        } else {
            authContainerVBOX.getChildren().add(authFaceController.getNode());
        }
    }

    public void auth(Profile profile) {
        Profile.IDENTITY = profile;

        screen.hide();

        mainScreen.getController().auth(profile);

        mainScreen.show();
    }
}
