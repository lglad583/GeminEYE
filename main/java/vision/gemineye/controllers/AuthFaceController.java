package vision.gemineye.controllers;

import javafx.fxml.FXML;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.ScreenController;

public class AuthFaceController implements ScreenController {

    private Screen<AuthController> authScreen;

    public void attachAuthScreen(Screen<AuthController> authScreen) {
        this.authScreen = authScreen;
    }

    @FXML
    private void toTextUnlock() {
        this.authScreen.getController().toAuthMode(false);
    }

    @Override
    public void init(Screen screen) throws Exception {

    }

}
