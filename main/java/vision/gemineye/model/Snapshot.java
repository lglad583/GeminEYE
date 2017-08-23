package vision.gemineye.model;

import java.time.LocalTime;
import java.util.List;

public class Snapshot {

    private List<String> users;
    private String feed;
    private LocalTime time;

    public Snapshot(List<String> users, String feed, LocalTime time) {
        this.users = users;
        this.feed = feed;
        this.time = time;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getFeed() {
        return feed;
    }

    public void setFeed(String feed) {
        this.feed = feed;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }
}
