package vision.gemineye.model.entity;

public class Attribute {

    public Attribute(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private String id;
    private String title;

    public String toString() {
        return title;
    }

}
