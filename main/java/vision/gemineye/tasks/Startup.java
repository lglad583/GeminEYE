package vision.gemineye.tasks;

import vision.gemineye.Common;
import vision.gemineye.controllers.SplashController;
import vision.gemineye.model.Attributes;
import vision.gemineye.model.BootstrapFeeds;
import vision.gemineye.model.Profiles;
import vision.gemineye.framework.tasker.TaskerExecutor;
import vision.gemineye.framework.tasker.TaskerResult;
import vision.gemineye.model.entity.AgeCNN;
import vision.gemineye.model.entity.GenderCNN;
import vision.gemineye.model.ui.Screen;
import vision.gemineye.framework.Task;
import vision.gemineye.model.Roles;

import java.util.List;

public class Startup extends TaskerExecutor {

    private Screen<SplashController> screen;

    public Startup(Screen<SplashController> screen) {
        super(
            new Common(),
            new BootstrapFeeds(),
            new AgeCNN(),
            new GenderCNN(),
            new Attributes(),
            new Roles(),
            new Profiles()
        );

        this.screen = screen;
    }

    @Override
    public void onTaskBlockComplete(Task task, TaskerResult result) {
        screen.getController().updateLabel(result.getMessage());
    }

    @Override
    public void onBeforeTask(Task task, TaskerResult result) throws Exception {
        screen.getController().updateLabel(result.getMessage());
    }

    @Override
    public boolean onTaskBlockError(Exception exception, Task task, TaskerResult result) throws Exception {
        Screen.error(this.screen, result.getMessage(), true);
        return false;
    }

    @Override
    public void onComplete(List<TaskerResult> result, boolean error) throws Exception {
        screen.getController().updateLabel("");
        screen.getStage().close();
    }

}
