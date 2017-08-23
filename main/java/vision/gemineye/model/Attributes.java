package vision.gemineye.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import vision.gemineye.framework.TaskBlocks;
import vision.gemineye.model.entity.Attribute;
import vision.gemineye.model.ui.Screen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Attributes extends TaskBlocks<String> {

    public static final String TAG = "ATTRIBUTES";

    private static Map<String, Map<String, Attribute>> attributes = new HashMap<>();

    public static Attribute getDefault(String key) {
        return attributes.get(key).get("-1");
    }

    public Attributes() {
        super("race", "gender");
    }

    public String resolveFor(String file) throws Exception {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.setLenient();

        Gson gson = builder.create();



        Path path = Screen.getPath("attribute/" + file + ".json", "attribute", "json");
        String JSON = new String(Files.readAllBytes(path));

        Attribute[] resolve = gson.fromJson(JSON, Attribute[].class);

        Map<String, Attribute> resolveMappings = new HashMap<>();

        for(Attribute attribute : resolve) {
            resolveMappings.put(attribute.getId(), attribute);
        }

        attributes.put(file, resolveMappings);

        return "Loaded " + file + " attributes";
    }

    @Override
    public String bootstrap() throws Exception {
        return "Loading attributes";
    }

    @Override
    public String label() {
        return TAG;
    }

    public static List<Attribute> getList(String race) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        Map<String, Attribute> mappings = Attributes.attributes.get(race);

        for(Map.Entry<String, Attribute> entry : mappings.entrySet()) {
            attributes.add(entry.getValue());
        }

        return attributes;
    }

    public static Attribute get(String key, String subKey) {
        return attributes.get(key).get(subKey);
    }
}
