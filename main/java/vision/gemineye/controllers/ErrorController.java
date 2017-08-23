package vision.gemineye.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.model.ui.ScreenController;

public class ErrorController implements ScreenController {

    private Stage stage;
    private boolean fatal;

    @FXML
    private TextArea error;

    @FXML
    private void onClose() {
        if (fatal) {
            System.exit(-1);
        } else {
            stage.close();
        }
    }

    public void init(Screen screen) throws Exception {
        this.stage = screen.getStage();
    }

    public void setErrorState(String message, boolean fatal) {
        this.fatal = fatal;
        this.stage.setOnCloseRequest(event -> onClose());

        Platform.runLater(() -> error.setText(message));
    }
}
