package vision.gemineye.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.ScreenController;
import vision.gemineye.model.Profiles;
import vision.gemineye.model.entity.Profile;

public class AuthTextController implements ScreenController {

    private Screen<AuthController> authScreen;

    @FXML
    private TextField usernameTextField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private Label errorLabel;

    @FXML
    private void auth() {
        errorLabel.setText("");

        String username = usernameTextField.getText().toLowerCase().trim();
        String password = passwordTextField.getText().toLowerCase().trim();

        if (username.isEmpty()) {
            errorLabel.setText("Please provide a username.");
            return;
        }

        if (password.isEmpty()) {
            errorLabel.setText("Please provide a password.");
            return;
        }

        Profile profile = Profiles.findByUsername(username);

        if (profile == null) {
            errorLabel.setText("User not found. Please check the username and try again!");
            return;
        }

        if (!profile.getCredentials().getPassword().contentEquals(password)) {
            errorLabel.setText("Password is incorrect. Please check the password and try again!");
            return;
        }

        usernameTextField.setText("");
        passwordTextField.setText("");
        errorLabel.setText("");

        authScreen.getController().auth(profile);
    }

    public void attachAuthScreen(Screen<AuthController> authScreen) {
        this.authScreen = authScreen;
    }

    @FXML
    private void toFaceUnlock() {
        this.authScreen.getController().toAuthMode(true);
    }

    @Override
    public void init(Screen screen) throws Exception {

    }

}
