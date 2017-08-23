package vision.gemineye.model.entity;

import javafx.event.EventType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.bytedeco.javacpp.opencv_core;
import vision.gemineye.Common;
import vision.gemineye.model.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.bytedeco.javacpp.opencv_core.*;

public class Profile {

    public static Profile IDENTITY;

    public static String PROFILE_IMAGE_PREFIX = "normal";
    public static String GALLERY_IMAGE_PREFIX = "gallery";
    public static String IMAGE_FORMAT = ".jpeg";

    private transient Image profileImage;
    private transient Boundary lastBoundary;
    public transient StateTracker state;

    private Identity identity;
    private Model model;
    private Model galleryImages;
    private Credentials credentials;
    private Role role;
    private String id;
    private String profileImageId;
    private Rules rules;
    private boolean unknown;

    public Profile(String id, Identity identity, Model model, Model galleryImages, Credentials credentials, Role role, Rules rules) {
        this.id = id;
        this.identity = identity;
        this.model = model;
        this.galleryImages = galleryImages;
        this.credentials = credentials;
        this.role = role;
        this.rules = rules;
    }

    public Model getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(Model galleryImages) {
        this.galleryImages = galleryImages;
    }

    public Boundary getLastBoundary() {
        return lastBoundary;
    }

    public void setLastBoundary(Boundary lastBoundary) {
        this.lastBoundary = lastBoundary;
    }

    public String getId() {
        return id;
    }


    public Image getProfileImage() {
        if(profileImage == null) {
            return Common.PROFILE;
        }
        return profileImage;
    }

    public void reloadProfileImage() {
        profileImage = Profiles.loadProfileImage(this);
    }

    public void reloadGalleryImages() {
        if(galleryImages == null) {
            galleryImages = new Model(new ArrayList<>());
        }

        galleryImages.getResolvedImages().clear();


        Profiles.loadModelImages(GALLERY_IMAGE_PREFIX, galleryImages);
    }

    public void reloadTrainingImages() {
        if(model == null) {
            model = new Model(new ArrayList<>());
        }

        model.getResolvedImages().clear();

        Profiles.loadModelImages(null, model);
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }


    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean has(Permission permission) {
        if(role == null) {
            return false;
        }
        return role.has(permission);
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean hasModelUUID(String uuid) {
        return model.getData().contains(uuid);
    }

    public String getProfileImageId() {
        return profileImageId;
    }

    public void setProfileImageId(String profileImageId) {
        this.profileImageId = profileImageId;
    }

    public boolean hasProfileImage() {
        return profileImageId != null && !profileImageId.isEmpty();
    }

    public void setRules(Rules rules){
        this.rules = rules;
    }
    public Rules getRules(){
        return rules;
    }


    public void setProfileImage(Image profileImage) {
        this.profileImage = profileImage;
    }

    public String getTitle() {
        String name = "--";

        if(identity.getName() != null &&
                identity.getName().getFirst() != null &&
                !identity.getName().getFirst().isEmpty()) {
            name = identity.getName().getFirst();

            if(identity.getName().getLast() != null
                    && !identity.getName().getLast().isEmpty()) {
                name = name + " " + identity.getName().getLast();
            }

        }

        if(credentials == null) {
            name = name + " (" + role.getTitle() + ")";
        } else {
            if(!credentials.getUsername().equalsIgnoreCase(identity.getName().getFirst())) {
                name = name + " (" + credentials.getUsername() + ")";
            }
        }

        return name;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public void setLastBoundary(Rect cvRect) {
        if(this.lastBoundary == null) {
            this.lastBoundary = new Boundary(cvRect);
        } else {
            this.lastBoundary.set(cvRect);
        }
    }

    public boolean hasTrainingImages() {
        return model != null && model.getResolvedImages() != null && !model.getResolvedImages().isEmpty();
    }

    public boolean inLocation(int x, int y) {
        return this.state != null &&
                this.state.location != null &&
                this.state.location.contains(new Point(x, y));
    }

    private transient Set<EventType> interactionState;

    public Set<EventType> getInteractionState() {
        if(interactionState == null) {
            interactionState = new HashSet<>();
        }
        return interactionState;
    }

    public boolean is(EventType state) {
        return interactionState != null && interactionState.contains(state);
    }

    public Profile set(EventType state) {
        getInteractionState().add(state);
        return this;
    }

    public Profile unset(EventType state) {
        if(interactionState == null) {
            return this;
        }

        interactionState.remove(state);
        return this;
    }

    public FeedIdentifier getFeedIdentifier() {
        FeedIdentifier identifier = new FeedIdentifier();
        identifier.boundsStrokeColor = CvScalar.GREEN;
        identifier.boundsStrokeWidth = 1;

        if(is(MouseEvent.MOUSE_ENTERED_TARGET)) {
            identifier.boundsStrokeWidth = 3;
        }
        if(is(MouseEvent.MOUSE_PRESSED)) {
            identifier.boundsStrokeWidth = 5;
        }

        if(state != null && state.isChecked()) {
            identifier.boundsStrokeWidth = 5;
            identifier.boundsStrokeColor = CvScalar.MAGENTA;
        }

        return identifier;
    }


    public String getInitals() {
        return identity.getName().getFirst().toUpperCase().substring(0, 1)
                + "." + identity.getName().getLast().toUpperCase().substring(0, 1) + ".";
    }
}
