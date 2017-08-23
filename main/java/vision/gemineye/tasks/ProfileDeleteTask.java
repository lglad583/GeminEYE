package vision.gemineye.tasks;

import vision.gemineye.Core;
import vision.gemineye.framework.Task;
import vision.gemineye.model.Profiles;
import vision.gemineye.model.entity.Profile;

import java.util.List;

public class ProfileDeleteTask implements Task {

    private List<Profile> toDelete;

    public ProfileDeleteTask(List<Profile> toDelete) {
        this.toDelete = toDelete;
    }


    @Override
    public String bootstrap() throws Exception {
        return "Deleting " + toDelete.size() + " profiles.";
    }

    @Override
    public String resolve(int index) throws Exception {
        Profile profile = toDelete.get(index);

        Profiles.delete(profile, index == toDelete.size()-1);

//        Core.getMainScreen().getController().reloadProfileList();

        return "Deleted profile " + profile.getTitle();
    }

    @Override
    public int length() {
        return toDelete.size();
    }

    @Override
    public String label() {
        return "DELETE PROFILE";
    }
}
