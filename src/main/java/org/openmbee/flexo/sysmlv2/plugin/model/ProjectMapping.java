package org.openmbee.flexo.sysmlv2.plugin.model;

import java.util.Objects;

/**
 * Represents a mapping between a local SysML v2 project and a remote project.
 * This allows synchronization between projects with different IDs.
 */
public class ProjectMapping {
    private String localProjectId;
    private String remoteName;
    private String remoteProjectId;

    public ProjectMapping() {
    }

    public ProjectMapping(String localProjectId, String remoteName, String remoteProjectId) {
        this.localProjectId = localProjectId;
        this.remoteName = remoteName;
        this.remoteProjectId = remoteProjectId;
    }

    public String getLocalProjectId() {
        return localProjectId;
    }

    public void setLocalProjectId(String localProjectId) {
        this.localProjectId = localProjectId;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

    public String getRemoteProjectId() {
        return remoteProjectId;
    }

    public void setRemoteProjectId(String remoteProjectId) {
        this.remoteProjectId = remoteProjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectMapping that = (ProjectMapping) o;
        return Objects.equals(localProjectId, that.localProjectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localProjectId);
    }

    @Override
    public String toString() {
        return localProjectId + " -> " + remoteName + ":" + remoteProjectId;
    }
}
