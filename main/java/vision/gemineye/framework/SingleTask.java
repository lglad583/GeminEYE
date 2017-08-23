package vision.gemineye.framework;

import java.util.function.Function;

public interface SingleTask extends Function {

    public void execute() throws Exception;

}
