package vision.gemineye.model.ui;

import vision.gemineye.model.entity.Profile;

import java.util.function.Predicate;

public class TabReference {

    private String title;
    private String fxml;
    private Predicate<Profile> predicate;
    private Screen screen;

    public TabReference(String title, Screen screen, Predicate<Profile> predicate) {
        this.title = title;
        this.screen = screen;
        this.predicate = predicate;
    }

    public TabReference(String title, String fxml, Predicate<Profile> predicate) {
        this.title = title;
        this.fxml = fxml;
        this.predicate = predicate;
    }

    public TabReference(String title, String fxml) {
        this(title, fxml, null);
    }

    public boolean isPreloaded() {
        return this.screen != null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFxml() {
        return fxml;
    }

    public void setFxml(String fxml) {
        this.fxml = fxml;
    }

    public Predicate<Profile> getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate<Profile> predicate) {
        this.predicate = predicate;
    }

    public Screen getScreen() {
        return screen;
    }
}
