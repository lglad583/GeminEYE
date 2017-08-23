package vision.gemineye.controllers.fragments.labeled;

import com.jfoenix.effects.JFXDepthManager;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class Labeled {

    private boolean valid;
    private Image mainLabel;
    private Image secondaryLabel;
    private String mainText;
    private String secondaryText;

    public Labeled(boolean valid, Image mainLabel, Image secondaryLabel, String mainText, String secondaryText) {
        this.valid = valid;
        this.mainLabel = mainLabel;
        this.secondaryLabel = secondaryLabel;
        this.mainText = mainText;
        this.secondaryText = secondaryText;
    }

    public Labeled(Image mainLabel, Image secondaryLabel) {
        this(true, mainLabel, secondaryLabel, null, null);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Image getMainLabel() {
        return mainLabel;
    }

    public void setMainLabel(Image mainLabel) {
        this.mainLabel = mainLabel;
    }

    public Image getSecondaryLabel() {
        return secondaryLabel;
    }

    public void setSecondaryLabel(Image secondaryLabel) {
        this.secondaryLabel = secondaryLabel;
    }

    public String getMainText() {
        return mainText;
    }

    public void setMainText(String mainText) {
        this.mainText = mainText;
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }

    public Node asNode(int index, EventHandler<MouseEvent> parent, boolean isSelectable) {
        ImageView view = new ImageView(mainLabel);
        view.setFitHeight(70);
        view.setFitWidth(70);

        StackPane imageViewWrapper = new StackPane(view);
        imageViewWrapper.getStyleClass().add("selectable");
        imageViewWrapper.getProperties().put("image", view);
        imageViewWrapper.setPrefSize(70, 70);
        imageViewWrapper.setMinSize(70, 70);
//
        view.getProperties().put("index", index);
        view.getProperties().put("wrapper", imageViewWrapper);

        JFXDepthManager.setDepth(imageViewWrapper, 1);

        if(parent != null && isSelectable)
        view.addEventHandler(MouseEvent.MOUSE_CLICKED, parent);

        return imageViewWrapper;
    }

}
