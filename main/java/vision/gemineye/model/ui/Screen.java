package vision.gemineye.model.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bytedeco.javacpp.Loader;
import vision.gemineye.Core;
import vision.gemineye.controllers.ErrorController;
import vision.gemineye.model.Snapshot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Screen<T extends ScreenController> {

    private static Screen<ErrorController> errorScreen;
    private static Logger logger = Logger.getLogger(Screen.class.getName());

    private T controller;
    private Parent node;
    private Stage stage;
    private Scene scene;
    private String title;
    private boolean hasShown;

    public Screen(T controller, String title, Parent node, Stage stage, Scene scene) {
        this.controller = controller;
        this.title = title;
        this.node = node;
        this.stage = stage;
        this.scene = scene;
    }

    public T getController() {
        return controller;
    }

    public void setController(T controller) {
        this.controller = controller;
    }

    public Parent getNode() {
        return node;
    }

    public void setNode(Parent node) {
        this.node = node;
    }

    public <E extends FragmentController<M, R>, M, R> void showAsFragment(Class<R> finish, Screen<?> parent, M attachment, Consumer<R> onFinishConsumer) {
        Platform.runLater(() -> {
            if (parent != null && !getStage().isShowing() && !hasShown) {
                getStage().initOwner(parent.getStage());
                getStage().initModality(Modality.WINDOW_MODAL);
            }

            FragmentController<M, R> controller = ((FragmentController<M, R>) getController());
            controller.setScreen(this);
            controller.start(attachment);
            controller.setResultConsumer(onFinishConsumer);

            hasShown = true;

            show();

            getStage().toFront();
        });

    }

    public <E extends FragmentController<M, R>, M, R> void showAsFragment(Class<R> finish, M attachment, Consumer<R> onFinishConsumer) {
        showAsFragment(finish, Core.getMainScreen(), attachment, onFinishConsumer);
    }

    public <E extends FragmentController<M, R>, M, R> void showAsFragment(Class<R> finish, Consumer<R> onFinishConsumer) {
        showAsFragment(finish, Core.getMainScreen(), null, onFinishConsumer);
    }

    public <E extends FragmentController<M, R>, M, R> void showAsFragment(Screen<?> parent, M attachment) {
        showAsFragment(Object.class, parent, attachment, null);
    }

    public <E extends FragmentController<M, R>, M, R> void showAsFragment(M attachment, Consumer onFinishConsumer) {
        showAsFragment(Object.class, Core.getMainScreen(), attachment, onFinishConsumer);
    }

    public <T extends ScreenController> Screen<T> show() {


        stage.show();
        return (Screen<T>) this;
    }

    public <T extends ScreenController> Screen<T> hide() {
        stage.hide();
        return (Screen<T>) this;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public static <T extends ScreenController> Screen<T> load(String fxml) {
        return load(fxml, fxml);
    }

    public static <T extends ScreenController> Screen<T> load(String fxml, String title, int width, int height) {
        return load(fxml, new Stage(), title, false, width, height);
    }

    public static <T extends ScreenController> Screen<T> load(String fxml, String title) {
        return load(fxml, null, title, true, -1, -1);
    }

    public static <T extends ScreenController> Screen<T> load(String fxml, Stage stage, String title, boolean headless, int width, int height) {
        try {

            logger.info("Loading FXML " + fxml);

            String path = ClassLoader.getSystemClassLoader().getResource("scene/" + fxml + ".fxml").toExternalForm();

            logger.info("Path: " + path);

            URL url = new URL(path);

            FXMLLoader loader = new FXMLLoader(url);

            Parent root = loader.load();
            T controller = loader.getController();

            Scene scene = null;

            if (!headless) {
                if (width != -1 && height != -1) {
                    scene = new Scene(root, width, height);
                } else {
                    scene = new Scene(root);
                }

                stage.setTitle(title);
                stage.setScene(scene);

                setIcons(stage);
            }

            if(scene != null) {
                scene.getStylesheets().addAll(ClassLoader.getSystemClassLoader().getResource("style/core.css").toExternalForm());
            }

            Screen<T> screen = new Screen<>(controller, title, root, stage, scene);

            controller.init(screen);


            return screen;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setErrorScreen(Screen<ErrorController> errorScreen) {
        Screen.errorScreen = errorScreen;
    }

    private static InputStream getInputStream(String location, String prefix, String suffix) {
        try {
            URL url = new URL(ClassLoader.getSystemClassLoader().getResource(location).toExternalForm());
            File file = Loader.extractResource(url, null, prefix, suffix);

            return new FileInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Path getPath(String location, String prefix, String suffix) {
        try {
            URL url = new URL(ClassLoader.getSystemClassLoader().getResource(location).toExternalForm());
            File file = Loader.extractResource(url, null, prefix, suffix);

            return Paths.get(file.toURI());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getFile(String location, String prefix, String suffix) {
        try {
            logger.info("ASSET: " + location);

            URL nonExternalURL = ClassLoader.getSystemClassLoader().getResource(location);

            logger.info("ASSET URL: " + nonExternalURL);

            URL url = new URL(nonExternalURL.toExternalForm());


            return Loader.extractResource(url, null, prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static URI getURI(String location, String prefix, String suffix) {
        return getFile(location, prefix, suffix).toURI();
    }


    private static URL getURL(String location, String prefix, String suffix) {
        try {
            return getURI(location, prefix, suffix).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getPNGAssetStream(String name) {
        return getInputStream("assets/" + name + ".png", "asset", ".png");
    }

    public static URL getPNGAssetURL(String name) {
        return getURL("assets/" + name + ".png", "asset", ".png");
    }

    public static void setIcons(Stage stage) {
        stage.getIcons().addAll(
                new Image(getPNGAssetStream("16_logo")),
                new Image(getPNGAssetStream("32_logo")),
                new Image(getPNGAssetStream("48_logo")),
                new Image(getPNGAssetStream("64_logo"))
        );
    }

    public static void error(Screen parent, String message, boolean fatal) {
        Platform.runLater(() -> {
            errorScreen.getController().setErrorState(message, fatal);

            if (!errorScreen.getStage().isShowing()) {
                errorScreen.getStage().initOwner(parent.getStage());
                errorScreen.getStage().initModality(Modality.WINDOW_MODAL);
                errorScreen.getStage().show();
            }

        });
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static <T extends ScreenController> Screen<T> fragment(String fxml, String title) {
        return Screen.load("fragment/" + fxml, title, -1, -1);
    }


}
