package vision.gemineye.model.entity;

import java.time.LocalTime;

public class Rules {

    private String restricted;
    private LocalTime curfew;
    private String[] conflicts;

    public Rules(String restricted, LocalTime curfew, String[] conflicts) {
        this.restricted = restricted;
        this.curfew = curfew;
        this.conflicts = conflicts;
    }

    public String getRestricted() {
        return restricted;
    }

    public void setRestricted(String restricted) {
        this.restricted = restricted;
    }

    public LocalTime getCurfew() {
        return curfew;
    }

    public void setCurfew(LocalTime curfew) {
        this.curfew = curfew;
    }

    public String[] getConflicts() {
        return conflicts;
    }

    public void setConflicts(String[] conflicts) {
        this.conflicts = conflicts;
    }
}
