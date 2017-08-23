package vision.gemineye.framework;

import vision.gemineye.Common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppLogger {

    private Logger logger;

    private AppLogger(String title) {
        this.logger = Logger.getLogger(title);
    }

    public AppLogger log(Object... args) {
        return log(Level.INFO, args);
    }

    public AppLogger info(Object... args) {
        return log(args);
    }

    public AppLogger error(Object... args) {
        return log(Level.SEVERE, args);
    }


    public AppLogger log(Level level, Object... args) {

        List<StringBuilder> lines = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        lines.add(builder);

        for(int i = 0; i < args.length; i++) {
            Object arg = args[i];

            if(arg == null && i < args.length-1) {
                builder = new StringBuilder();
                lines.add(builder);
                continue;
            }

            if (arg instanceof Exception) {
               Exception exception = (Exception) arg;

               arg = Common.getStackTrace(exception);
            }

            builder.append(arg);
        }

        for (StringBuilder stringBuilder : lines) {
            logger.log(
                    level,
                    stringBuilder.toString()
            );
        }



        return this;
    }

    public static AppLogger get(Class controller) {
        return new AppLogger(controller.getName());
    }

    public AppLogger blocks(Object... args) {
        List<Object> asList = Arrays.asList(args);
        String title = asList.get(0).toString();
        asList.remove(0);
        return blocks(title, asList.toArray());
    }

    public AppLogger blocks(String title, Object... args) {
        return blocks(Level.INFO, title, args);
    }

    public AppLogger blocks(Level level, String title, Object... args) {
        List<Object> message = new ArrayList<>();

        message.add(null);
        message.add("----------------------------");
        message.add(null);
        message.add(title);
        message.add(null);
        message.add("---------------");

        for (int i = 0; i < args.length; i += 2) {
            message.add(null);
            message.add("----\t");
            message.add(args[i]);
            message.add(":");
            message.add(args[i+1]);
        }

        return log(level, message.toArray());
    }
}
