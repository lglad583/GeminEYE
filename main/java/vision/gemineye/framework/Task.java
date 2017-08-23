package vision.gemineye.framework;

public interface Task {

    String bootstrap() throws Exception;

    String resolve(int index) throws Exception;

    int length();

    String label();

}
