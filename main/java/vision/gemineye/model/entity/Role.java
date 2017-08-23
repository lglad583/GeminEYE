package vision.gemineye.model.entity;

import vision.gemineye.model.Permission;

import java.util.EnumSet;

public class Role {

    private String title;
    private EnumSet<Permission> permissions;

    public Role(String title, EnumSet<Permission> permissions) {
        this.title = title;
        this.permissions = permissions;
    }

    public boolean has(Permission permission) {
        return this.permissions.contains(permission);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(EnumSet<Permission> permissions) {
        this.permissions = permissions;
    }

    public String toString() {
        return title;
    }
}
