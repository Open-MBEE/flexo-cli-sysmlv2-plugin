package org.openmbee.flexo.sysmlv2.plugin.model;

import java.util.Objects;

/**
 * Represents a mapping between a local branch and a remote branch.
 * Branch mappings are scoped under project mappings.
 */
public class BranchMapping {
    private String localProjectId;
    private String localBranchId;
    private String remoteBranchId;

    public BranchMapping() {
    }

    public BranchMapping(String localProjectId, String localBranchId, String remoteBranchId) {
        this.localProjectId = localProjectId;
        this.localBranchId = localBranchId;
        this.remoteBranchId = remoteBranchId;
    }

    public String getLocalProjectId() {
        return localProjectId;
    }

    public void setLocalProjectId(String localProjectId) {
        this.localProjectId = localProjectId;
    }

    public String getLocalBranchId() {
        return localBranchId;
    }

    public void setLocalBranchId(String localBranchId) {
        this.localBranchId = localBranchId;
    }

    public String getRemoteBranchId() {
        return remoteBranchId;
    }

    public void setRemoteBranchId(String remoteBranchId) {
        this.remoteBranchId = remoteBranchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchMapping that = (BranchMapping) o;
        return Objects.equals(localProjectId, that.localProjectId) &&
               Objects.equals(localBranchId, that.localBranchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localProjectId, localBranchId);
    }

    @Override
    public String toString() {
        return localProjectId + ":" + localBranchId + " -> " + remoteBranchId;
    }
}
