package vision.gemineye.model;

import com.google.gson.Gson;
import org.omg.CORBA.UNKNOWN;
import vision.gemineye.framework.TaskStorage;
import vision.gemineye.model.entity.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;

public class Roles extends TaskStorage {

    public static final String TAG = "ROLES";
    public static Role[] role;
    public static Role UNKNOWN = new Role("Unknown", EnumSet.noneOf(Permission.class));

    private static Role[] DEFAULT_ROLES = {

            UNKNOWN,

            new Role("Visitor", EnumSet.noneOf(Permission.class)),
            new Role("Inmate", EnumSet.noneOf(Permission.class)),

            new Role("Employee", EnumSet.of(
                    Permission.ACCESS,
                    Permission.ACCESS_EXPLORER,
                    Permission.ACCESS_TARGETS,
                    Permission.ACCESS_FEEDS
            )),

            new Role("Admin", EnumSet.allOf(Permission.class))

    };

    public String process(Path path) throws Exception {
        path = path.resolve("roles.json");

        Gson gson = new Gson();

        if (!Files.exists(path)) {
            String JSON = gson.toJson(DEFAULT_ROLES);

            Files.createFile(path);

            Files.write(path, JSON.getBytes());

            role = DEFAULT_ROLES.clone();

            return "Created Roles";
        }

        String JSON = new String(Files.readAllBytes(path));

        role = gson.fromJson(JSON, Role[].class);

        return "Loaded " + role.length + " roles";
    }

    public static Role[] getRoles() {
        return role;
    }

    @Override
    public String bootstrap() throws Exception {
        return "Loading roles";
    }

    @Override
    public String label() {
        return TAG;
    }


    public static Role get(Role abstractRole) {
        if(abstractRole == null) {
            return null;
        }
        for(Role role: role) {
            if (role.getTitle().contentEquals(abstractRole.getTitle())) {
                return role;
            }
        }
        return null;
    }
}
