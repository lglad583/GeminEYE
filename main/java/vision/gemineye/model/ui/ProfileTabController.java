package vision.gemineye.model.ui;

import vision.gemineye.model.entity.Profile;
import vision.gemineye.model.ui.TabController;

public abstract class ProfileTabController implements TabController {

    private Profile profile;

    public void setProfile(Profile profile) {
        this.profile = profile;

        onProfileSelect(profile);
    }

    public Profile getProfile() {
        return profile;
    }

    protected abstract void onProfileSelect(Profile profile);


}
