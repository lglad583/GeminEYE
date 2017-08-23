package vision.gemineye.framework.tasker;

import vision.gemineye.Common;
import vision.gemineye.framework.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskerResult {


    public TaskerResult(boolean error, String message, int progress, Exception exception) {
        this.completed = completed;
        this.error = error;
        this.message = message;
        this.progress = progress;
        this.exception = exception;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    private final Exception exception;
    private boolean completed;
    private boolean error;
    private String message;
    private int progress;
    private TaskerService service;
    private Task task;

    public TaskerService getService() {
        return service;
    }

    public void setService(TaskerService service) {
        this.service = service;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public List<Exception> getExceptions() {
        if(service == null) {
            return new ArrayList<>();
        }
        return service.getExceptions();
    }

    public Exception getException() {
        return exception;
    }


    public static TaskerResult snippet(String resolve, int progress) {
        return snippet(resolve, progress, false, null)    ;
    }

    public static TaskerResult snippet(Exception exception, int progress) {
        return snippet(Common.getStackTrace(exception), progress, true, exception)    ;
    }

    public static TaskerResult snippet(String resolve, int progress, boolean error, Exception exception) {

        return new TaskerResult(error, resolve, progress, exception);
    }
}
