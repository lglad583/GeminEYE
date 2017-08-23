package vision.gemineye.model.entity;

public class Name {

    public Name(String first, String middle, String last) {
        this.first = first;
        this.middle = middle;
        this.last = last;
    }

    @Override
    public String toString() {
        return first + " " + last;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getMiddle() {
        return middle;
    }

    public void setMiddle(String middle) {
        this.middle = middle;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    private String first;
    private String middle;
    private String last;

    public boolean isInvalid() {
        return first == null || first.isEmpty() || last == null || last.isEmpty();
    }

    public String getMiddleAsText() {
        if(middle == null || middle.isEmpty()) {
            return  "--";
        }
        return middle;
     }
}
