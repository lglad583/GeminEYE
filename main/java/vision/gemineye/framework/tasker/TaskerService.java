package vision.gemineye.framework.tasker;

import vision.gemineye.Common;
import vision.gemineye.framework.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskerService {

    private final Task task;
    private final int length;
    private final double size;

    private AtomicInteger progress;
    private AtomicInteger index;

    private boolean error;
    private boolean completed;

    private List<Exception> exceptions;
    private List<String> errorMessages;
    private List<TaskerResult> results;
    private List<Future<?>> siblings;

    private CountDownLatch latch;

    public TaskerService(Task task, int length) {
        this.task = task;
        this.length = length;
        this.latch = new CountDownLatch(length);
        this.siblings = new ArrayList<>();
        this.exceptions = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.results = new ArrayList<>();
        this.size = (1D / (double) length);
        this.index = new AtomicInteger(0);
        this.progress = new AtomicInteger(0);
    }

    public Runnable resolveNextBlock(
            TaskerEvent onBeforeEvent,
            TaskerEvent onBlockCompleteEvent,
            TaskerEvent onBlockErrorEvent,
            TaskerEvent onCompleteEvent) {
        return () -> {
            getLatch().countDown();

            TaskerResult result = resolve(onBeforeEvent);

            if (result.isError()) {
                onBlockErrorEvent.onEvent(result);
            } else {
                onBlockCompleteEvent.onEvent(result);
            }

            if (completed) {
                result.setError(isError());

                onCompleteEvent.onEvent(result);
            }
        };
    }

    private TaskerResult resolve(TaskerEvent onBeforeEvent) {
        if (completed) {
            throw new RuntimeException("Task already resolved");
        }

        int current = index.getAndAdd(1);

        String resolve;
        String message;
        Exception exception = null;
        boolean localError = false;

        try {
            resolve = task.bootstrap();

            onBeforeEvent.onEvent(TaskerResult.snippet(resolve, progress.get()));

        } catch (Exception e) {

            e.printStackTrace();

            String bootstrapError = Common.getStackTrace(e);

            errorMessages.add(bootstrapError);
            exceptions.add(e);

            onBeforeEvent.onEvent(TaskerResult.snippet(e, progress.get()));
        }

        try {
            message = task.resolve(current);

            progress.addAndGet((int) size);
        } catch (Throwable error) {
            Exception exceptionResolve;
            if(error instanceof Exception) {
                exceptionResolve = (Exception) error;
            } else {
                exceptionResolve = new RuntimeException(error);
            }

            exception = exceptionResolve;
            exception.printStackTrace();

            message = Common.getStackTrace(exceptionResolve);
            localError = true;

            errorMessages.add(message);

            exceptions.add(exception);

            this.error = true;
        }

        progress.set(Math.min(progress.get(), 100));

        if (index.get() >= length - 1) {
            completed = true;
        }

        TaskerResult result = new TaskerResult(
                localError,
                message,
                progress.get(),
                exception
        );

        result.setService(this);
        result.setTask(task);

        results.add(result);

        return result;

    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public List<TaskerResult> getResults() {
        return results;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isError() {
        return error;
    }

    public void abort() {
        try {
            siblings.forEach(future -> future.cancel(true));
        } catch (Exception e) {
            // Ignore
        }

        try {
            int currentIndex = index.get();

            for (int i = currentIndex; i < length; i++) {
                latch.countDown();
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public List<Future<?>> getSiblings() {
        return siblings;
    }

    public CountDownLatch getLatch() {
        return latch;
    }
}
