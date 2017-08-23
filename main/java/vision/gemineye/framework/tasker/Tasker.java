package vision.gemineye.framework.tasker;

import vision.gemineye.framework.SingleTask;
import vision.gemineye.framework.Task;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

public class Tasker {

    private Task task;
    private boolean async;
    private boolean background;
    private TaskerEvent onBlockCompleteEvent;
    private TaskerEvent onBeforeEvent;
    private TaskerEvent onBlockErrorEvent;
    private TaskerEvent onCompleteEvent;

    public Tasker(Task task) {
        this(task, Tasker.NOOP, Tasker.NOOP, Tasker.NOOP, Tasker.NOOP);
    }

    public Tasker(Task task, TaskerEvent onBeforeEvent, TaskerEvent onBlockCompleteEvent, TaskerEvent onBlockErrorEvent, TaskerEvent onCompleteEvent) {
        this(task, true, true, onBeforeEvent, onBlockCompleteEvent, onBlockErrorEvent, onCompleteEvent);
    }

    public Tasker(Task task, boolean async, boolean background, TaskerEvent onBeforeEvent, TaskerEvent onBlockCompleteEvent, TaskerEvent onBlockErrorEvent, TaskerEvent onCompleteEvent) {
        this.task = task;
        this.async = async;
        this.background = background;
        this.onBeforeEvent = onBeforeEvent;
        this.onBlockCompleteEvent = onBlockCompleteEvent;
        this.onBlockErrorEvent = onBlockErrorEvent;
        this.onCompleteEvent = onCompleteEvent;
    }

    public TaskerEvent getOnBeforeEvent() {
        return onBeforeEvent;
    }

    public void setOnBeforeEvent(TaskerEvent onBeforeEvent) {
        this.onBeforeEvent = onBeforeEvent;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public TaskerEvent getOnBlockCompleteEvent() {
        return onBlockCompleteEvent;
    }

    public void setOnBlockCompleteEvent(TaskerEvent onBlockCompleteEvent) {
        this.onBlockCompleteEvent = onBlockCompleteEvent;
    }

    public TaskerEvent getOnBlockErrorEvent() {
        return onBlockErrorEvent;
    }

    public void setOnBlockErrorEvent(TaskerEvent onBlockErrorEvent) {
        this.onBlockErrorEvent = onBlockErrorEvent;
    }

    public TaskerEvent getOnCompleteEvent() {
        return onCompleteEvent;
    }

    public void setOnCompleteEvent(TaskerEvent onCompleteEvent) {
        this.onCompleteEvent = onCompleteEvent;
    }

    public TaskerService execute() {
        return Tasker.execute(this);
    }

    private static ExecutorService pool = Executors.newCachedThreadPool();
    public static TaskerEvent NOOP = result -> {
    };

    public static TaskerService execute(Tasker tasker) {
        return execute(
                tasker.task,
                tasker.async,
                tasker.background,
                tasker.onBeforeEvent,
                tasker.onBlockCompleteEvent,
                tasker.onBlockErrorEvent,
                tasker.onCompleteEvent
        );
    }

    public static TaskerService async(Task task,
                                      TaskerEvent onBeforeEvent,
                                      TaskerEvent onUpdateEvent,
                                      TaskerEvent onErrorEvent,
                                      TaskerEvent onCompleteEvent) {
        return execute(task, true, true, onBeforeEvent, onUpdateEvent, onErrorEvent, onCompleteEvent);
    }

    public static TaskerService sync(Task task,
                                     TaskerEvent onBeforeEvent,
                                     TaskerEvent onUpdateEvent,
                                     TaskerEvent onErrorEvent,
                                     TaskerEvent onCompleteEvent) {
        return execute(task, false, true, onBeforeEvent, onUpdateEvent, onErrorEvent, onCompleteEvent);
    }

    public static TaskerService execute(
            Task task,
            boolean async,
            boolean background,
            TaskerEvent onBeforeEvent,
            TaskerEvent onBlockCompleteEvent,
            TaskerEvent onBlockErrorEvent,
            TaskerEvent onCompleteEvent) {

        final int length = task.length();

        TaskerService service = new TaskerService(task, length);

        if (async) {
            for (int i = 0; i < length; i++) {
                Future<?> future = pool.submit(service.resolveNextBlock(onBeforeEvent, onBlockCompleteEvent, onBlockErrorEvent, onCompleteEvent));

                service.getSiblings().add(future);
            }
        } else {
            Future<?> future = pool.submit(() -> {
                for (int i = 0; i < length; i++) {
                    service.resolveNextBlock(onBeforeEvent, onBlockCompleteEvent, onBlockErrorEvent, onCompleteEvent).run();
                }
            });

            service.getSiblings().add(future);
        }

        if (!background) {
            try {
                service.getLatch().await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return service;
    }

    public static TaskerService async(Task task) {
        return Tasker.execute(new Tasker(task));
    }

    public static TaskerService async(Task task, TaskerEvent onCompleteEvent) {
        return Tasker.async(task, Tasker.NOOP, Tasker.NOOP, Tasker.NOOP, onCompleteEvent);
    }

    public static void background(Consumer function) {
        Tasker tasker = new Tasker(new Task() {
            @Override
            public String bootstrap() throws Exception {
                return "";
            }
            @Override
            public String resolve(int index) throws Exception {
                function.accept(index);
                return "";
            }

            @Override
            public int length() {
                return 1;
            }

            @Override
            public String label() {
                return "tasker-task_" + System.currentTimeMillis();
            }
        });
    }
}
