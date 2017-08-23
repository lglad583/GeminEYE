package vision.gemineye.model;

import vision.gemineye.controllers.modules.FeedsController;
import vision.gemineye.framework.Task;

public class BootstrapFeeds implements Task {

    @Override
    public String bootstrap() throws Exception {
        return "Loading feeds";
    }

    @Override
    public String resolve(int index) throws Exception {
        FeedsController.loadClassifer();

        return "Loaded feeds";
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public String label() {
        return "FEEDS";
    }
}
