package vision.gemineye.model.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import vision.gemineye.model.entity.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TabContainer implements ChangeListener<Tab> {

    private Map<String, Screen<?>> tabs = new HashMap<>();
    private List<TabReference> references = new ArrayList<>();
    private TabPane pane;
    private String path;

    public TabContainer(TabPane pane, String path) {
        this.pane = pane;
        this.path = path;
    }

    public void init() {
        pane.getSelectionModel().selectedItemProperty().removeListener(this);
        pane.getSelectionModel().selectedItemProperty().addListener(this);
    }

    public void add(String title, String fxml) {
        add(title, fxml, null);
    }

    public void add(String title, String fxml, Predicate<Profile> predicate) {
        references.add(new TabReference(title, fxml, predicate));
    }

    public void add(String title, Screen screen, Predicate<Profile> predicate) {
        references.add(new TabReference(title, screen, predicate));
    }

    public void attach(Profile profile) {
        pane.getTabs().clear();

        for (TabReference reference : references) {
            if (profile == null || reference.getPredicate() == null) {
                addTab(reference.getTitle(), reference.getFxml(), reference.getScreen());
            } else {
                if (reference.getPredicate().test(profile)) {
                    addTab(reference.getTitle(), reference.getFxml(), reference.getScreen());
                }
            }
        }

    }

    private void addTab(String title, String fxmlTitle, Screen screen) {

        if(fxmlTitle != null) {
            if (tabs.containsKey(fxmlTitle)) {
                screen = tabs.get(fxmlTitle);
            } else {
                screen = Screen.load(path + fxmlTitle, title);
                tabs.put(fxmlTitle, screen);
            }
        } else {
            tabs.put(screen.getTitle(), screen);
        }


        Tab tab = new Tab(title);
        tab.getProperties().put("controller", screen.getController());
        tab.setContent(screen.getNode());

        screen.getNode().getProperties().put("tabIndex", pane.getTabs().size());

        pane.getTabs().add(tab);

        tabs.put(fxmlTitle, screen);
    }

    public <T extends ScreenController> Screen<T> getTab(String key) {
        return (Screen<T>) tabs.get(key);
    }

    @Override
    public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab currentTab) {
        if (oldTab != null) {
            Object controller = oldTab.getProperties().get("controller");
            if (controller instanceof TabController) {
                Platform.runLater(((TabController) controller)::onHideEvent);
            }
        }

        if (currentTab != null) {
            Object controller = currentTab.getProperties().get("controller");
            if (controller instanceof TabController) {
                Platform.runLater(((TabController) controller)::onShowEvent);
            }
        }
    }

    public int getTabIndex(String screenId) {
        return (Integer) tabs.get(screenId).getNode().getProperties().get("tabIndex");
    }
}
