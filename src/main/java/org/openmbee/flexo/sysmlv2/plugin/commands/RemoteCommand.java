package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

/**
 * Remote command - Manage remote SysML v2 API instances
 * Similar to git remote command
 */
@Command(
        name = "remote",
        description = "Manage remote SysML v2 API instances",
        mixinStandardHelpOptions = true,
        subcommands = {
                RemoteCommand.AddCommand.class,
                RemoteCommand.RemoveCommand.class,
                RemoteCommand.ListCommand.class,
                RemoteCommand.SetUrlCommand.class,
                RemoteCommand.SetOrgCommand.class,
                RemoteCommand.SetFlexoRemoteCommand.class,
                RemoteCommand.ShowCommand.class,
                RemoteCommand.RenameCommand.class
        }
)
public class RemoteCommand extends PluginCommand {

    @Override
    public void run() {
        // Default behavior: list remotes
        try {
            new ListCommand().run();
        } catch (Exception e) {
            error("Failed to list remotes: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * List all remotes
     */
    @Command(
            name = "list",
            aliases = {"ls", "-v"},
            description = "List all configured SysML v2 remotes",
            mixinStandardHelpOptions = true
    )
    static class ListCommand extends PluginCommand {
        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();
                Map<String, SysMLRemote> remotes = config.getRemotes();

                if (remotes.isEmpty()) {
                    info("No SysML v2 remotes configured");
                    info("");
                    info("Add a remote with: flexo sysml remote add <name> <url>");
                    return;
                }

                String defaultRemote = config.getDefaultRemote();
                info("Configured SysML v2 remotes:");
                for (SysMLRemote remote : remotes.values()) {
                    String marker = remote.getName().equals(defaultRemote) ? " *" : "";
                    info("  " + remote.getName() + marker + "\t" + remote.getUrl());
                }
            } catch (Exception e) {
                error("Failed to list remotes: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to list remotes", e);
            }
        }
    }

    /**
     * Add a new remote
     */
    @Command(
            name = "add",
            description = "Add a new SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class AddCommand extends PluginCommand {
        @Parameters(index = "0", description = "Remote name")
        private String name;

        @Parameters(index = "1", description = "Remote URL (e.g., http://localhost:9000)")
        private String url;

        @Option(names = {"--set-default"}, description = "Set as default remote")
        private boolean setDefault;

        @Option(names = {"--flexo-remote"}, description = "Name of Flexo backend remote to use for authentication")
        private String flexoRemote;

        @Option(names = {"--org"}, description = "Organization name (defaults to 'sysmlv2')")
        private String org;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                // Check if remote already exists
                if (config.hasRemote(name)) {
                    error("Remote '" + name + "' already exists");
                    info("Use 'flexo sysml remote set-url " + name + " <url>' to update URL");
                    throw new RuntimeException("Remote '" + name + "' already exists");
                }

                // Validate Flexo remote exists if specified
                if (flexoRemote != null) {
                    validateFlexoRemote(flexoRemote);
                }

                // Create and configure remote
                SysMLRemote remote = new SysMLRemote(name, url, flexoRemote);
                
                // Set organization if specified
                if (org != null && !org.isEmpty()) {
                    remote.setOrg(org);
                }
                
                config.setRemote(remote);

                // Set as default if requested
                if (setDefault) {
                    config.setDefaultRemote(name);
                }

                config.save();
                success("Remote '" + name + "' added: " + url);
                if (org != null && !org.isEmpty()) {
                    info("Organization: " + org);
                } else {
                    info("Organization: sysmlv2 (default)");
                }
                if (flexoRemote != null) {
                    info("Linked to Flexo backend remote: " + flexoRemote);
                }
                if (setDefault) {
                    info("Set as default remote");
                }
            } catch (Exception e) {
                error("Failed to add remote: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to add remote", e);
            }
        }
    }

    /**
     * Remove a remote
     */
    @Command(
            name = "remove",
            aliases = {"rm"},
            description = "Remove a SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class RemoveCommand extends PluginCommand {
        @Parameters(index = "0", description = "Remote name")
        private String name;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasRemote(name)) {
                    error("Remote '" + name + "' does not exist");
                    info("Use 'flexo sysml remote list' to see configured remotes");
                    throw new RuntimeException("Remote '" + name + "' does not exist");
                }

                config.removeRemote(name);
                config.save();
                success("Remote '" + name + "' removed");
            } catch (Exception e) {
                error("Failed to remove remote: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to remove remote", e);
            }
        }
    }

    /**
     * Set URL for a remote
     */
    @Command(
            name = "set-url",
            description = "Change the URL for a SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class SetUrlCommand extends PluginCommand {
        @Parameters(index = "0", description = "Remote name")
        private String name;

        @Parameters(index = "1", description = "New URL")
        private String url;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasRemote(name)) {
                    error("Remote '" + name + "' does not exist");
                    info("Use 'flexo sysml remote add " + name + " <url>' to add it");
                    throw new RuntimeException("Remote '" + name + "' does not exist");
                }

                SysMLRemote remote = config.getRemote(name);
                remote.setUrl(url);
                config.setRemote(remote);
                config.save();
                success("Remote '" + name + "' URL updated to: " + url);
            } catch (Exception e) {
                error("Failed to update remote URL: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to update remote URL", e);
            }
        }
    }

    /**
     * Set organization for a SysML v2 remote
     */
    @Command(
            name = "set-org",
            description = "Set or update the organization for a SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class SetOrgCommand extends PluginCommand {
        @Parameters(index = "0", description = "SysML v2 remote name")
        private String name;

        @Parameters(index = "1", description = "Organization name (use 'none' to reset to default 'sysmlv2')")
        private String org;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasRemote(name)) {
                    error("Remote '" + name + "' does not exist");
                    info("Use 'flexo sysml remote add " + name + " <url>' to add it");
                    throw new RuntimeException("Remote '" + name + "' does not exist");
                }

                SysMLRemote remote = config.getRemote(name);
                
                // Handle unsetting (reset to default)
                if ("none".equalsIgnoreCase(org)) {
                    remote.setOrg(null);
                    config.setRemote(remote);
                    config.save();
                    success("Organization reset for '" + name + "' (will use default: sysmlv2)");
                } else {
                    remote.setOrg(org);
                    config.setRemote(remote);
                    config.save();
                    success("Remote '" + name + "' organization set to: " + org);
                }
            } catch (Exception e) {
                error("Failed to update organization: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to update organization", e);
            }
        }
    }

    /**
     * Set Flexo backend remote for a SysML v2 remote
     */
    @Command(
            name = "set-flexo-remote",
            description = "Set or update the Flexo backend remote for authentication",
            mixinStandardHelpOptions = true
    )
    static class SetFlexoRemoteCommand extends PluginCommand {
        @Parameters(index = "0", description = "SysML v2 remote name")
        private String name;

        @Parameters(index = "1", description = "Flexo backend remote name (use 'none' to unset)")
        private String flexoRemote;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasRemote(name)) {
                    error("Remote '" + name + "' does not exist");
                    info("Use 'flexo sysml remote add " + name + " <url>' to add it");
                    throw new RuntimeException("Remote '" + name + "' does not exist");
                }

                SysMLRemote remote = config.getRemote(name);
                
                // Handle unsetting
                if ("none".equalsIgnoreCase(flexoRemote)) {
                    remote.setFlexoRemote(null);
                    config.setRemote(remote);
                    config.save();
                    success("Flexo backend remote unset for '" + name + "'");
                    info("Will use default Flexo backend configuration");
                } else {
                    // Validate Flexo remote exists
                    validateFlexoRemote(flexoRemote);
                    
                    remote.setFlexoRemote(flexoRemote);
                    config.setRemote(remote);
                    config.save();
                    success("Remote '" + name + "' now uses Flexo backend remote: " + flexoRemote);
                }
            } catch (Exception e) {
                error("Failed to update Flexo backend remote: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to update Flexo backend remote", e);
            }
        }
    }

    /**
     * Show details about a remote
     */
    @Command(
            name = "show",
            description = "Show details about a SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class ShowCommand extends PluginCommand {
        @Parameters(index = "0", description = "Remote name")
        private String name;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                SysMLRemote remote = config.getRemote(name);
                if (remote == null) {
                    error("Remote '" + name + "' does not exist");
                    info("Use 'flexo sysml remote list' to see configured remotes");
                    throw new RuntimeException("Remote '" + name + "' does not exist");
                }

                String defaultRemote = config.getDefaultRemote();
                boolean isDefault = remote.getName().equals(defaultRemote);

                info("Remote: " + remote.getName() + (isDefault ? " (default)" : ""));
                info("  URL: " + remote.getUrl());
                info("  Organization: " + remote.getOrgOrDefault());
                if (remote.getFlexoRemote() != null) {
                    info("  Flexo Backend: " + remote.getFlexoRemote());
                }
            } catch (Exception e) {
                error("Failed to show remote: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to show remote", e);
            }
        }
    }

    /**
     * Rename a remote
     */
    @Command(
            name = "rename",
            description = "Rename a SysML v2 remote",
            mixinStandardHelpOptions = true
    )
    static class RenameCommand extends PluginCommand {
        @Parameters(index = "0", description = "Old name")
        private String oldName;

        @Parameters(index = "1", description = "New name")
        private String newName;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasRemote(oldName)) {
                    error("Remote '" + oldName + "' does not exist");
                    info("Use 'flexo sysml remote list' to see configured remotes");
                    throw new RuntimeException("Remote '" + oldName + "' does not exist");
                }

                if (config.hasRemote(newName)) {
                    error("Remote '" + newName + "' already exists");
                    throw new RuntimeException("Remote '" + newName + "' already exists");
                }

                config.renameRemote(oldName, newName);
                config.save();
                success("Remote '" + oldName + "' renamed to '" + newName + "'");
            } catch (Exception e) {
                error("Failed to rename remote: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to rename remote", e);
            }
        }
    }

    /**
     * Validate that a Flexo backend remote exists in the configuration
     */
    private static void validateFlexoRemote(String flexoRemoteName) {
        try {
            // Access the parent Flexo CLI configuration to check if the remote exists
            org.openmbee.flexo.cli.config.FlexoConfig flexoConfig = new org.openmbee.flexo.cli.config.FlexoConfig();
            String remoteUrl = flexoConfig.getRemoteUrl(flexoRemoteName);
            
            if (remoteUrl == null) {
                throw new IllegalArgumentException(
                    "Flexo backend remote '" + flexoRemoteName + "' does not exist.\n" +
                    "Add it first with: flexo remote add " + flexoRemoteName + " <url>"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate Flexo backend remote: " + e.getMessage(), e);
        }
    }
}
