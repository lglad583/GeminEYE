package vision.gemineye.controllers.fragments.labeled;

import com.jfoenix.controls.JFXMasonryPane;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LabeledList implements EventHandler<MouseEvent> {

    private final LabeledListController controller;
    private int lastIndexSelected = -1;
    private List<Labeled> labeledList;
    private List<Labeled> isValid;
    private List<Labeled> inValid;

    public List<Labeled> getLabeledList() {
        return labeledList;
    }

    public void setLabeledList(List<Labeled> labeledList) {
        this.labeledList = labeledList;
    }

    public List<Labeled> getIsValid() {
        return isValid;
    }

    public void setIsValid(List<Labeled> isValid) {
        this.isValid = isValid;
    }

    public List<Labeled> getInValid() {
        return inValid;
    }

    public void setInValid(List<Labeled> inValid) {
        this.inValid = inValid;
    }

    private void remove(List<Node> elements, Node node) {
        if(elements.contains(node)) {
            elements.remove(node);
        }
    }

    private void add(List<Node> elements, Node node) {
        if (!elements.contains(node)) {
            elements.add(node);
        }
    }

    private void usingList(List<Labeled> model) {
        this.labeledList = model;

        this.isValid = model.stream().filter(Labeled::isValid).collect(Collectors.toList());
        this.inValid = model.stream().filter(label -> !label.isValid()).collect(Collectors.toList());

        this.controller.getSelection().clear();
        this.controller.getSelection().addAll(isValid);

        remove( this.controller.getContent().getChildren(),  this.controller.getValidSection());
        add( this.controller.getSplitContainer().getItems(),  this.controller.getValidSection());
        this.controller.getSplitContainer().visibleProperty().set(true);

        if (inValid.isEmpty()) {
            this.controller.getSplitContainer().visibleProperty().set(false);
            add( this.controller.getContent().getChildren(), this.controller.getValidSection());
        }
    }

    private void addToPane(Pane pane, List<Labeled> labeledList, boolean selectable) {
        List<Node> grid = new ArrayList<>();

        for(int i = 0; i < labeledList.size(); i++) {
            grid.add(labeledList.get(i).asNode(i, selectable ? this : null, selectable));


        }

        pane.getChildren().addAll(grid);

        if(selectable) {
            for(int i = 0; i < labeledList.size(); i++) {
                toggleModelSelection(i, true);
            }
        }
    }

    public LabeledList(LabeledListController controller) {
        this.controller = controller;
    }

    public void attach(List<Labeled> labeledList) {
        usingList(labeledList);

        controller.getInValidContainer().getChildren().removeAll(controller.getInValidContainer().getChildren());
        controller.getInValidContainer().layout();

        controller.getValidContainer().getChildren().removeAll(controller.getValidContainer().getChildren());
        controller.getValidContainer().layout();

        addToPane(controller.getValidContainer(), isValid, true);
        addToPane(controller.getInValidContainer(), inValid, true);

        controller.layout(!inValid.isEmpty());

    }

    private void toggleModelSelection(int index, boolean state) {

        StackPane pane = (StackPane) this.controller.getValidContainer().getChildren().get(index);
        ImageView view = (ImageView) pane.getProperties().get("image");

        ObservableMap<Object, Object> properties = view.getProperties();

        if (state) {

            Labeled option = isValid.get(index);
            if(!controller.getSelection().contains(option))
                controller.getSelection().add(option);

            properties.put("selected", true);
            pane.setStyle("-fx-background-color:teal;");
        } else {
            controller.getSelection().remove(isValid.get(index));
            properties.remove("selected");
            pane.setStyle("-fx-background-color:transparent;");
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
            for (int i = higher + 1; i < controller.getValidContainer().getChildren().size(); i++) {
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
