package vision.gemineye.model.ui;

import vision.gemineye.controllers.modules.FeedsController;
import vision.gemineye.model.entity.Profile;

public class UserTrainAttachment {

    private FeedsController feedsController;
    private Profile profile;

    public UserTrainAttachment(FeedsController feedsController, Profile profile) {
        this.feedsController = feedsController;
        this.profile = profile;
    }

    public FeedsController getFeedsController() {
        return feedsController;
    }

    public void setFeedsController(FeedsController feedsController) {
        this.feedsController = feedsController;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
