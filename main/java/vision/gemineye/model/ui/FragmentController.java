package vision.gemineye.model.ui;

import javafx.application.Platform;

import java.util.function.Consumer;

public abstract class FragmentController<M, T> implements ScreenController {

    private Screen screen;
    private Consumer<T> resultConsumer;

    public void setScreen(Screen screen) {
        this.screen = screen;
    }


    public void setResultConsumer(Consumer<T> resultConsumer) {
        this.resultConsumer = resultConsumer;
    }

    public void finish(T task) {
        screen.hide();

        if(resultConsumer != null) {
            Platform.runLater(() -> resultConsumer.accept(task));
        }
    }

    public abstract void start(M model);

}
