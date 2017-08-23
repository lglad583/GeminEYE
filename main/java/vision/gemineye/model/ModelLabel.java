package vision.gemineye.model;

import javafx.scene.image.Image;
import vision.gemineye.controllers.fragments.labeled.Labeled;
import vision.gemineye.model.entity.Profile;

public class ModelLabel extends Labeled {

    private Profile profile;

    public ModelLabel(Profile profile, Image image) {
        super(image, null);
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }




}
