package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import picocli.CommandLine.ParentCommand;

/**
 * Base class for SysML commands that need remote-aware URL resolution
 */
public abstract class SysMLBaseCommand extends PluginCommand {
    
    @ParentCommand
    private SysMLCommand parent;
    
    /**
     * Get the SysML v2 API URL, resolving from remote if specified
     */
    protected String getSysMLUrl() {
        try {
            SysMLConfigHelper config = new SysMLConfigHelper();
            String remoteName = parent != null ? parent.getRemoteName() : null;
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
        return parent != null ? parent.getRemoteName() : null;
    }
}
