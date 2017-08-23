package vision.gemineye.framework;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class TaskStorage extends TaskAttachment<Path> {

    public TaskStorage() {
        super(Paths.get(System.getenv("APPDATA"), "FlexVision"));
    }

    public abstract String process(Path path) throws Exception;

    public String load(Path attachment) throws Exception {
        if (!Files.exists(attachment)) {
            Files.createDirectories(attachment);
        }

        return process(attachment);
    }

}
