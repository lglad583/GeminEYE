package vision.gemineye.framework;

public abstract class TaskAttachment<T> implements Task {

    private final T attachment;

    public TaskAttachment(T attachment) {
        this.attachment = attachment;
    }

    public abstract String load(T attachment) throws Exception;

    public String resolve(int index) throws Exception  {
        return load(this.attachment);
    }

    public int length() {
        return 1;
    }

}
