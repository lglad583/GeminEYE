package vision.gemineye.controllers.fragments.labeled;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXMasonryPane;
import com.jfoenix.controls.JFXScrollPane;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import vision.gemineye.model.ui.FragmentController;
import vision.gemineye.model.ui.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LabeledListController extends FragmentController<List<Labeled>, List<Labeled>> {

    @FXML
    private AnchorPane content;
    @FXML
    private JFXButton confirmButton;
    @FXML
    private JFXMasonryPane validContainer;
    @FXML
    private JFXMasonryPane inValidContainer;
    @FXML
    private Label countLabel;
    @FXML
    private Label validContainerLabel;
    @FXML
    private HBox validContainerRoot;
    @FXML
    private HBox invalidContainerRoot;
    @FXML
    private SplitPane splitContainer;
    @FXML
    private AnchorPane validSection;
    @FXML
    private ScrollPane validScrollPane;
    @FXML
    private ScrollPane invalidScrollPane;
    private ObservableList<Labeled> selection = FXCollections.observableArrayList();

    private LabeledList labeledList;

    @FXML
    public void onNevermindAction() {
        finish(null);
    }
    @FXML
    public void onBackAction() {
        this.onNevermindAction();
    }
    @FXML
    public void onConfirmAction() {
        finish(selection);

        validContainer.getChildren().clear();
        inValidContainer.getChildren().clear();
    }



    @Override
    public void start(List<Labeled> model) {
        if(model == null) {
            model = new ArrayList<Labeled>();
        }

        final List<Labeled> labeledList = model;

        Platform.runLater(() -> {
            this.labeledList.attach(labeledList);
        });
    }

    @Override
    public void init(Screen screen) throws Exception {
        this.labeledList = new LabeledList(this);

        selection.addListener((ListChangeListener<? super Labeled>) onChange -> {
            Platform.runLater(() -> {
                int size = onChange.getList().size();

                countLabel.setText(onChange.getList().size() + "");;
                confirmButton.setDisable(size == 0);
            });
        });

    }

    public AnchorPane getContent() {
        return content;
    }

    public void setContent(AnchorPane content) {
        this.content = content;
    }

    public JFXButton getConfirmButton() {
        return confirmButton;
    }

    public void setConfirmButton(JFXButton confirmButton) {
        this.confirmButton = confirmButton;
    }

    public JFXMasonryPane getValidContainer() {
        return validContainer;
    }

    public void setValidContainer(JFXMasonryPane validContainer) {
        this.validContainer = validContainer;
    }

    public JFXMasonryPane getInValidContainer() {
        return inValidContainer;
    }

    public void setInValidContainer(JFXMasonryPane inValidContainer) {
        this.inValidContainer = inValidContainer;
    }

    public Label getCountLabel() {
        return countLabel;
    }

    public void setCountLabel(Label countLabel) {
        this.countLabel = countLabel;
    }

    public Label getValidContainerLabel() {
        return validContainerLabel;
    }

    public void setValidContainerLabel(Label validContainerLabel) {
        this.validContainerLabel = validContainerLabel;
    }

    public HBox getValidContainerRoot() {
        return validContainerRoot;
    }

    public void setValidContainerRoot(HBox validContainerRoot) {
        this.validContainerRoot = validContainerRoot;
    }

    public HBox getInvalidContainerRoot() {
        return invalidContainerRoot;
    }

    public void setInvalidContainerRoot(HBox invalidContainerRoot) {
        this.invalidContainerRoot = invalidContainerRoot;
    }

    public SplitPane getSplitContainer() {
        return splitContainer;
    }

    public void setSplitContainer(SplitPane splitContainer) {
        this.splitContainer = splitContainer;
    }

    public AnchorPane getValidSection() {
        return validSection;
    }

    public void setValidSection(AnchorPane validSection) {
        this.validSection = validSection;
    }

    public ScrollPane getValidScrollPane() {
        return validScrollPane;
    }

    public void setValidScrollPane(ScrollPane validScrollPane) {
        this.validScrollPane = validScrollPane;
    }

    public ScrollPane getInvalidScrollPane() {
        return invalidScrollPane;
    }

    public void setInvalidScrollPane(ScrollPane invalidScrollPane) {
        this.invalidScrollPane = invalidScrollPane;
    }

    public ObservableList<Labeled> getSelection() {
        return selection;
    }

    public void setSelection(ObservableList<Labeled> selection) {
        this.selection = selection;
    }

    public void layout(boolean invalid) {
        validContainerRoot.layout();
        invalidContainerRoot.layout();

        JFXScrollPane.smoothScrolling(validScrollPane);

        if(invalid)
        JFXScrollPane.smoothScrolling(invalidScrollPane);

    }
}
