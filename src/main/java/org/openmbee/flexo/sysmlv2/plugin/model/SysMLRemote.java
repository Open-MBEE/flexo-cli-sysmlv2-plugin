package org.openmbee.flexo.sysmlv2.plugin.model;

import java.util.Objects;

/**
 * Represents a remote SysML v2 API instance.
 * Similar to git remotes, allows working with multiple SysML v2 servers.
 */
public class SysMLRemote {
    private String name;
    private String url;

    public SysMLRemote() {
    }

    public SysMLRemote(String name, String url) {
        this.name = name;
        this.url = url;
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
