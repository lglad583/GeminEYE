package vision.gemineye.framework.tasker;

import javafx.application.Platform;
import vision.gemineye.framework.Task;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TaskerExecutor {

    private Task[] tasks;
    private TaskerResolver resolver;
    private TaskerEvent then;

    public TaskerExecutor(Task... tasks) {
        this.tasks = tasks;
        this.resolver = new TaskerResolver();
    }

    public TaskerExecutor then(TaskerEvent event) {
        this.then = event;
        return this;
    }

    public void onTaskComplete(TaskerResult result) throws Exception {
    }

    public abstract void onTaskBlockComplete(Task task, TaskerResult result) throws Exception;

    public abstract void onBeforeTask(Task task, TaskerResult result) throws Exception;

    public abstract boolean onTaskBlockError(Exception exception, Task task, TaskerResult result) throws Exception;

    public abstract void onComplete(List<TaskerResult> result, boolean error) throws Exception;


    public TaskerExecutor execute() {
        for (Task task : tasks) {
            AtomicBoolean firedCompleteTask = new AtomicBoolean(false);

            final TaskerService service = Tasker.async(
                    task,
                    result -> Platform.runLater(() -> {
                        try {
                            onBeforeTask(task, result);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }),
                    result -> Platform.runLater(() -> {
                        try {
                            onTaskBlockComplete(task, result);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }),
                    result -> Platform.runLater(() -> {
                        try {
                            if (!onTaskBlockError(result.getException(), task, result)) {
                               resolver.abort();
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }),
                    result -> Platform.runLater(() -> {
                        try {
                            onTaskComplete(result);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        if (!firedCompleteTask.get() && resolver.isComplete()) {
                            firedCompleteTask.set(true);

                            try {
                                onComplete(resolver.getResults(), resolver.isError());

                                if(then != null) {
                                    then.onEvent(result);
                                }

                            } catch(Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                    })
            );

            resolver.submit(service);
        }
        return this;
    }

}
