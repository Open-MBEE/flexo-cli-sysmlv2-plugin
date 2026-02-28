package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.client.AuthenticationHandler;
import org.openmbee.flexo.cli.client.FlexoMmsClient;
import org.openmbee.flexo.cli.config.FlexoConfig;
import org.openmbee.flexo.cli.plugin.PluginCommand;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;

/**
 * Base class for SysML commands that need remote-aware URL resolution
 * and project ID mapping support
 */
public abstract class SysMLBaseCommand extends PluginCommand {
    
    /**
     * Get an authenticated HTTP client for MMS API calls, using the Flexo backend
     * remote mapped to the current SysML v2 remote
     *
     * @return FlexoMmsClient ready for use
     */
    @Override
    protected FlexoMmsClient getClient() {
        try {
            SysMLConfigHelper sysmlConfig = new SysMLConfigHelper();
            String sysmlRemoteName = getRemoteName();
            
            // Get the SysML v2 remote configuration
            SysMLRemote sysmlRemote = sysmlConfig.getRemote(sysmlRemoteName != null ? sysmlRemoteName : sysmlConfig.getDefaultRemote());
            
            // Check if this SysML v2 remote has a mapped Flexo backend remote
            String flexoRemoteName = (sysmlRemote != null) ? sysmlRemote.getFlexoRemote() : null;
            
            if (flexoRemoteName != null) {
                // Use the mapped Flexo backend remote for authentication
                if (isVerbose()) {
                    debug("Using Flexo backend remote '" + flexoRemoteName + "' for authentication");
                }
                return createClientForFlexoRemote(flexoRemoteName);
            } else {
                // Fall back to default behavior (uses default Flexo remote)
                if (isVerbose()) {
                    debug("No Flexo backend remote mapped, using default configuration");
                }
                return super.getClient();
            }
        } catch (Exception e) {
            if (isVerbose()) {
                debug("Failed to resolve Flexo backend remote: " + e.getMessage());
                debug("Using default client configuration");
            }
            return super.getClient();
        }
    }
    
    /**
     * Create a FlexoMmsClient for a specific Flexo backend remote
     */
    private FlexoMmsClient createClientForFlexoRemote(String remoteName) {
        try {
            FlexoConfig flexoConfig = getConfig();
            
            // Get the remote configuration
            org.openmbee.flexo.cli.model.Remote flexoRemote = flexoConfig.getRemote(remoteName);
            if (flexoRemote == null) {
                warn("Flexo backend remote '" + remoteName + "' not found, using default");
                return super.getClient();
            }
            
            // Get authentication settings from remote (with fallback to global settings)
            boolean authEnabled = flexoRemote.getAuthEnabled() != null 
                ? Boolean.parseBoolean(flexoRemote.getAuthEnabled())
                : flexoConfig.isAuthEnabled();
                
            String sshKeyPath = flexoRemote.getSshKeyPath() != null
                ? flexoRemote.getSshKeyPath()
                : flexoConfig.getSshKeyPath();
                
            boolean localMode = flexoRemote.getLocalMode() != null
                ? Boolean.parseBoolean(flexoRemote.getLocalMode())
                : flexoConfig.isLocalMode();
                
            String localUser = flexoRemote.getLocalUser() != null
                ? flexoRemote.getLocalUser()
                : flexoConfig.getLocalUser();
                
            String localJwtSecret = flexoRemote.getLocalJwtSecret() != null
                ? flexoRemote.getLocalJwtSecret()
                : flexoConfig.getLocalJwtSecret();
            
            // Create authentication handler with remote-specific settings
            AuthenticationHandler authHandler = new AuthenticationHandler(
                    authEnabled,
                    sshKeyPath,
                    localMode,
                    localUser,
                    localJwtSecret
            );
            
            return new FlexoMmsClient(flexoRemote.getUrl(), authHandler);
        } catch (Exception e) {
            warn("Failed to create client for Flexo remote '" + remoteName + "': " + e.getMessage());
            return super.getClient();
        }
    }
    
    /**
     * Get the SysML v2 API URL, resolving from remote if specified
     */
    protected String getSysMLUrl() {
        try {
            SysMLConfigHelper config = new SysMLConfigHelper();
            // Get remote from global options via context
            String remoteName = getConfig().get("sysmlv2.default.remote");
            return config.getRemoteUrl(remoteName);
        } catch (Exception e) {
            // Fallback to default
            if (isVerbose()) {
                debug("Failed to resolve remote URL: " + e.getMessage());
                debug("Using default URL: http://localhost:9000");
            }
            return "http://localhost:9000";
        }
    }
    
    /**
     * Get the remote name if specified
     */
    protected String getRemoteName() {
        try {
            return getConfig().get("sysmlv2.default.remote");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if project ID mapping should be used
     * This would be set via a global flag which we'll implement later
     */
    protected boolean shouldMapProjectId() {
        // For now, always return false
        // TODO: Implement --map-from flag handling
        return false;
    }
    
    /**
     * Map a remote project ID to local project ID if mapping is enabled
     * Returns the original ID if mapping is disabled or no mapping exists
     */
    protected String mapProjectId(String projectId) {
        if (!shouldMapProjectId()) {
            return projectId;
        }
        
        try {
            SysMLConfigHelper config = new SysMLConfigHelper();
            String remoteName = getRemoteName();
            
            String localProjectId;
            if (remoteName != null) {
                localProjectId = config.mapToLocalProjectId(remoteName, projectId);
            } else {
                localProjectId = config.mapToLocalProjectId(projectId);
            }
            
            if (localProjectId != null) {
                if (isVerbose()) {
                    debug("Mapped remote project ID '" + projectId + "' to local ID '" + localProjectId + "'");
                }
                return localProjectId;
            } else {
                if (isVerbose()) {
                    debug("No mapping found for remote project ID '" + projectId + "', using as-is");
                }
                return projectId;
            }
        } catch (Exception e) {
            if (isVerbose()) {
                debug("Failed to map project ID: " + e.getMessage());
            }
            return projectId;
        }
    }
    
    /**
     * Map a remote branch ID to local branch ID if mapping is enabled
     * Requires the local project ID (which may already be mapped)
     * Returns the original ID if mapping is disabled or no mapping exists
     */
    protected String mapBranchId(String localProjectId, String branchId) {
        if (!shouldMapProjectId()) {
            return branchId;
        }
        
        try {
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            String localBranchId = config.mapToLocalBranchId(localProjectId, branchId);
            
            if (localBranchId != null) {
                if (isVerbose()) {
                    debug("Mapped remote branch ID '" + branchId + "' to local ID '" + localBranchId + "'");
                }
                return localBranchId;
            } else {
                if (isVerbose()) {
                    debug("No mapping found for remote branch ID '" + branchId + "', using as-is");
                }
                return branchId;
            }
        } catch (Exception e) {
            if (isVerbose()) {
                debug("Failed to map branch ID: " + e.getMessage());
            }
            return branchId;
        }
    }
}
