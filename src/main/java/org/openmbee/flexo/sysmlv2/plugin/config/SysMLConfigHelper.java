package org.openmbee.flexo.sysmlv2.plugin.config;

import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;

import java.io.*;
import java.util.*;

/**
 * Helper class to manage SysML v2 remote configurations.
 * Reads and writes to ~/.flexo/config file using sysmlv2.remote.* properties.
 */
public class SysMLConfigHelper {
    
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "/.flexo/config";
    private static final String REMOTE_PREFIX = "sysmlv2.remote.";
    private static final String DEFAULT_REMOTE_KEY = "sysmlv2.default.remote";
    
    private Properties properties;
    
    public SysMLConfigHelper() throws IOException {
        properties = new Properties();
        File configFile = new File(CONFIG_FILE_PATH);
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                properties.load(reader);
            }
        }
    }
    
    /**
     * Get all configured SysML v2 remotes
     */
    public Map<String, SysMLRemote> getRemotes() {
        Map<String, SysMLRemote> remotes = new LinkedHashMap<>();
        
        // Find all sysmlv2.remote.*.url properties
        Set<String> remoteNames = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(REMOTE_PREFIX) && key.endsWith(".url")) {
                String remoteName = extractRemoteName(key);
                remoteNames.add(remoteName);
            }
        }
        
        // Build SysMLRemote objects
        for (String name : remoteNames) {
            SysMLRemote remote = new SysMLRemote();
            remote.setName(name);
            remote.setUrl(properties.getProperty(REMOTE_PREFIX + name + ".url"));
            remotes.put(name, remote);
        }
        
        return remotes;
    }
    
    /**
     * Get a specific remote by name
     */
    public SysMLRemote getRemote(String name) {
        String url = properties.getProperty(REMOTE_PREFIX + name + ".url");
        if (url == null) {
            return null;
        }
        
        SysMLRemote remote = new SysMLRemote();
        remote.setName(name);
        remote.setUrl(url);
        return remote;
    }
    
    /**
     * Add or update a remote
     */
    public void setRemote(SysMLRemote remote) {
        properties.setProperty(REMOTE_PREFIX + remote.getName() + ".url", remote.getUrl());
    }
    
    /**
     * Remove a remote
     */
    public void removeRemote(String name) {
        String prefix = REMOTE_PREFIX + name;
        List<String> keysToRemove = new ArrayList<>();
        
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix + ".")) {
                keysToRemove.add(key);
            }
        }
        
        for (String key : keysToRemove) {
            properties.remove(key);
        }
    }
    
    /**
     * Check if a remote exists
     */
    public boolean hasRemote(String name) {
        return properties.containsKey(REMOTE_PREFIX + name + ".url");
    }
    
    /**
     * Get the default remote name
     */
    public String getDefaultRemote() {
        return properties.getProperty(DEFAULT_REMOTE_KEY, "origin");
    }
    
    /**
     * Set the default remote
     */
    public void setDefaultRemote(String remoteName) {
        properties.setProperty(DEFAULT_REMOTE_KEY, remoteName);
    }
    
    /**
     * Get URL for a specific remote, or default remote URL
     */
    public String getRemoteUrl(String remoteName) {
        if (remoteName == null) {
            remoteName = getDefaultRemote();
        }
        
        SysMLRemote remote = getRemote(remoteName);
        if (remote != null) {
            return remote.getUrl();
        }
        
        // Fallback to sysmlv2.url for backward compatibility
        return properties.getProperty("sysmlv2.url", "http://localhost:9000");
    }
    
    /**
     * Rename a remote
     */
    public void renameRemote(String oldName, String newName) {
        SysMLRemote remote = getRemote(oldName);
        if (remote == null) {
            throw new IllegalArgumentException("Remote '" + oldName + "' does not exist");
        }
        
        if (hasRemote(newName)) {
            throw new IllegalArgumentException("Remote '" + newName + "' already exists");
        }
        
        // Add with new name
        remote.setName(newName);
        setRemote(remote);
        
        // Remove old name
        removeRemote(oldName);
        
        // Update default if needed
        if (getDefaultRemote().equals(oldName)) {
            setDefaultRemote(newName);
        }
    }
    
    /**
     * Save configuration to file
     */
    public void save() throws IOException {
        File configFile = new File(CONFIG_FILE_PATH);
        
        // Ensure directory exists
        File configDir = configFile.getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            properties.store(writer, "Flexo CLI Configuration");
        }
    }
    
    /**
     * Extract remote name from property key like "sysmlv2.remote.origin.url"
     */
    private String extractRemoteName(String key) {
        // Remove prefix "sysmlv2.remote."
        String withoutPrefix = key.substring(REMOTE_PREFIX.length());
        // Get everything before ".url"
        int dotIndex = withoutPrefix.indexOf(".url");
        return withoutPrefix.substring(0, dotIndex);
    }
}
