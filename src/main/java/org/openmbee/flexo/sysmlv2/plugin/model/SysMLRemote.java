package org.openmbee.flexo.sysmlv2.plugin.model;

import java.util.Objects;

/**
 * Represents a remote SysML v2 API instance.
 * Similar to git remotes, allows working with multiple SysML v2 servers.
 */
public class SysMLRemote {
    private String name;
    private String url;
    private String flexoRemote;  // Name of the Flexo backend remote to use for authentication
    private String org;  // Flexo MMS organization name (defaults to "sysmlv2")

    public SysMLRemote() {
    }

    public SysMLRemote(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public SysMLRemote(String name, String url, String flexoRemote) {
        this.name = name;
        this.url = url;
        this.flexoRemote = flexoRemote;
    }
    
    public SysMLRemote(String name, String url, String flexoRemote, String org) {
        this.name = name;
        this.url = url;
        this.flexoRemote = flexoRemote;
        this.org = org;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFlexoRemote() {
        return flexoRemote;
    }

    public void setFlexoRemote(String flexoRemote) {
        this.flexoRemote = flexoRemote;
    }
    
    public String getOrg() {
        return org;
    }
    
    public void setOrg(String org) {
        this.org = org;
    }
    
    /**
     * Get the organization name with fallback to default
     */
    public String getOrgOrDefault() {
        return org != null && !org.isEmpty() ? org : "sysmlv2";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysMLRemote that = (SysMLRemote) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + "\t" + url;
    }
}
