package org.openmbee.flexo.sysmlv2.plugin.config;

import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;

import java.io.*;
import java.util.*;

/**
 * Helper class to manage SysML v2 remote configurations.
 * Reads and writes to ~/.flexo/config file using sysmlv2.remote.* properties.
 */
public class SysMLConfigHelper {
    
    private static final String REMOTE_PREFIX = "sysmlv2.remote.";
    private static final String DEFAULT_REMOTE_KEY = "sysmlv2.default.remote";
    private static final String MAPPING_PREFIX = "sysmlv2.mapping.";
    
    private Properties properties;
    
    /**
     * Get the config file path dynamically based on current user.home property
     */
    private String getConfigFilePath() {
        return System.getProperty("user.home") + "/.flexo/config";
    }
    
    public SysMLConfigHelper() throws IOException {
        properties = new Properties();
        File configFile = new File(getConfigFilePath());
        
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
            remote.setFlexoRemote(properties.getProperty(REMOTE_PREFIX + name + ".flexoRemote"));
            remote.setOrg(properties.getProperty(REMOTE_PREFIX + name + ".org"));
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
        remote.setFlexoRemote(properties.getProperty(REMOTE_PREFIX + name + ".flexoRemote"));
        remote.setOrg(properties.getProperty(REMOTE_PREFIX + name + ".org"));
        return remote;
    }
    
    /**
     * Add or update a remote
     */
    public void setRemote(SysMLRemote remote) {
        properties.setProperty(REMOTE_PREFIX + remote.getName() + ".url", remote.getUrl());
        if (remote.getFlexoRemote() != null && !remote.getFlexoRemote().isEmpty()) {
            properties.setProperty(REMOTE_PREFIX + remote.getName() + ".flexoRemote", remote.getFlexoRemote());
        }
        if (remote.getOrg() != null && !remote.getOrg().isEmpty()) {
            properties.setProperty(REMOTE_PREFIX + remote.getName() + ".org", remote.getOrg());
        }
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
        File configFile = new File(getConfigFilePath());
        
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
    
    // ========== Project Mapping Management ==========
    
    /**
     * Get all project mappings
     */
    public Map<String, ProjectMapping> getProjectMappings() {
        Map<String, ProjectMapping> mappings = new LinkedHashMap<>();
        
        // Find all sysmlv2.mapping.*.remote properties
        Set<String> localProjectIds = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(MAPPING_PREFIX) && key.endsWith(".remote")) {
                String localProjectId = extractLocalProjectId(key);
                localProjectIds.add(localProjectId);
            }
        }
        
        // Build ProjectMapping objects
        for (String localProjectId : localProjectIds) {
            ProjectMapping mapping = new ProjectMapping();
            mapping.setLocalProjectId(localProjectId);
            mapping.setRemoteName(properties.getProperty(MAPPING_PREFIX + localProjectId + ".remote"));
            mapping.setRemoteProjectId(properties.getProperty(MAPPING_PREFIX + localProjectId + ".remoteProjectId"));
            mappings.put(localProjectId, mapping);
        }
        
        return mappings;
    }
    
    /**
     * Get a specific project mapping by local project ID
     */
    public ProjectMapping getProjectMapping(String localProjectId) {
        String remoteName = properties.getProperty(MAPPING_PREFIX + localProjectId + ".remote");
        if (remoteName == null) {
            return null;
        }
        
        ProjectMapping mapping = new ProjectMapping();
        mapping.setLocalProjectId(localProjectId);
        mapping.setRemoteName(remoteName);
        mapping.setRemoteProjectId(properties.getProperty(MAPPING_PREFIX + localProjectId + ".remoteProjectId"));
        
        return mapping;
    }
    
    /**
     * Get a specific project mapping by remote project ID and remote name
     * This allows looking up the local project ID given a remote project ID
     */
    public ProjectMapping getProjectMappingByRemote(String remoteName, String remoteProjectId) {
        Map<String, ProjectMapping> allMappings = getProjectMappings();
        for (ProjectMapping mapping : allMappings.values()) {
            if (remoteName.equals(mapping.getRemoteName()) && 
                remoteProjectId.equals(mapping.getRemoteProjectId())) {
                return mapping;
            }
        }
        return null;
    }
    
    /**
     * Add or update a project mapping
     */
    public void setProjectMapping(ProjectMapping mapping) {
        String prefix = MAPPING_PREFIX + mapping.getLocalProjectId();
        properties.setProperty(prefix + ".remote", mapping.getRemoteName());
        properties.setProperty(prefix + ".remoteProjectId", mapping.getRemoteProjectId());
    }
    
    /**
     * Remove a project mapping
     */
    public void removeProjectMapping(String localProjectId) {
        String prefix = MAPPING_PREFIX + localProjectId;
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
     * Check if a project mapping exists
     */
    public boolean hasProjectMapping(String localProjectId) {
        return properties.containsKey(MAPPING_PREFIX + localProjectId + ".remote");
    }
    
    /**
     * Get local project ID from remote project ID and remote name
     * This allows reverse lookup: given a remote project ID, find the local project ID
     */
    public String getLocalProjectId(String remoteName, String remoteProjectId) {
        Map<String, ProjectMapping> mappings = getProjectMappings();
        
        for (ProjectMapping mapping : mappings.values()) {
            if (mapping.getRemoteName().equals(remoteName) && 
                mapping.getRemoteProjectId().equals(remoteProjectId)) {
                return mapping.getLocalProjectId();
            }
        }
        
        return null;
    }
    
    /**
     * Map a remote project ID to local project ID using the default remote
     */
    public String mapToLocalProjectId(String remoteProjectId) {
        String defaultRemote = getDefaultRemote();
        return getLocalProjectId(defaultRemote, remoteProjectId);
    }
    
    /**
     * Map a remote project ID to local project ID using a specific remote
     */
    public String mapToLocalProjectId(String remoteName, String remoteProjectId) {
        return getLocalProjectId(remoteName, remoteProjectId);
    }
    
    /**
     * Extract local project ID from property key like "sysmlv2.mapping.PROJECT_ID.remote"
     */
    private String extractLocalProjectId(String key) {
        // Remove prefix "sysmlv2.mapping."
        String withoutPrefix = key.substring(MAPPING_PREFIX.length());
        // Get everything before ".remote"
        int dotIndex = withoutPrefix.indexOf(".remote");
        return withoutPrefix.substring(0, dotIndex);
    }
    
    // ========== Branch Mapping Management ==========
    
    /**
     * Get all branch mappings for a specific local project
     */
    public Map<String, BranchMapping> getBranchMappings(String localProjectId) {
        Map<String, BranchMapping> mappings = new LinkedHashMap<>();
        String branchPrefix = MAPPING_PREFIX + localProjectId + ".branch.";
        
        // Find all branch mapping properties
        Set<String> localBranchIds = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(branchPrefix) && key.endsWith(".remoteBranchId")) {
                String localBranchId = extractLocalBranchId(key, branchPrefix);
                localBranchIds.add(localBranchId);
            }
        }
        
        // Build BranchMapping objects
        for (String localBranchId : localBranchIds) {
            BranchMapping mapping = new BranchMapping();
            mapping.setLocalProjectId(localProjectId);
            mapping.setLocalBranchId(localBranchId);
            mapping.setRemoteBranchId(properties.getProperty(branchPrefix + localBranchId + ".remoteBranchId"));
            mappings.put(localBranchId, mapping);
        }
        
        return mappings;
    }
    
    /**
     * Get a specific branch mapping
     */
    public BranchMapping getBranchMapping(String localProjectId, String localBranchId) {
        String key = MAPPING_PREFIX + localProjectId + ".branch." + localBranchId + ".remoteBranchId";
        String remoteBranchId = properties.getProperty(key);
        
        if (remoteBranchId == null) {
            return null;
        }
        
        return new BranchMapping(localProjectId, localBranchId, remoteBranchId);
    }
    
    /**
     * Get a specific branch mapping by remote branch ID
     * This allows looking up the local branch ID given a remote branch ID
     */
    public BranchMapping getBranchMappingByRemote(String localProjectId, String remoteBranchId) {
        Map<String, BranchMapping> allBranchMappings = getBranchMappings(localProjectId);
        for (BranchMapping mapping : allBranchMappings.values()) {
            if (remoteBranchId.equals(mapping.getRemoteBranchId())) {
                return mapping;
            }
        }
        return null;
    }
    
    /**
     * Add or update a branch mapping
     */
    public void setBranchMapping(BranchMapping mapping) {
        String key = MAPPING_PREFIX + mapping.getLocalProjectId() + ".branch." + 
                     mapping.getLocalBranchId() + ".remoteBranchId";
        properties.setProperty(key, mapping.getRemoteBranchId());
    }
    
    /**
     * Remove a branch mapping
     */
    public void removeBranchMapping(String localProjectId, String localBranchId) {
        String prefix = MAPPING_PREFIX + localProjectId + ".branch." + localBranchId;
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
     * Check if a branch mapping exists
     */
    public boolean hasBranchMapping(String localProjectId, String localBranchId) {
        String key = MAPPING_PREFIX + localProjectId + ".branch." + localBranchId + ".remoteBranchId";
        return properties.containsKey(key);
    }
    
    /**
     * Get local branch ID from remote branch ID
     * Requires knowing the local project ID
     */
    public String getLocalBranchId(String localProjectId, String remoteBranchId) {
        Map<String, BranchMapping> mappings = getBranchMappings(localProjectId);
        
        for (BranchMapping mapping : mappings.values()) {
            if (mapping.getRemoteBranchId().equals(remoteBranchId)) {
                return mapping.getLocalBranchId();
            }
        }
        
        return null;
    }
    
    /**
     * Map a remote branch ID to local branch ID
     * Requires the local project ID (which may itself be mapped)
     */
    public String mapToLocalBranchId(String localProjectId, String remoteBranchId) {
        return getLocalBranchId(localProjectId, remoteBranchId);
    }
    
    /**
     * Extract local branch ID from property key
     */
    private String extractLocalBranchId(String key, String branchPrefix) {
        // Remove branch prefix
        String withoutPrefix = key.substring(branchPrefix.length());
        // Get everything before ".remoteBranchId"
        int dotIndex = withoutPrefix.indexOf(".remoteBranchId");
        return withoutPrefix.substring(0, dotIndex);
    }
}
