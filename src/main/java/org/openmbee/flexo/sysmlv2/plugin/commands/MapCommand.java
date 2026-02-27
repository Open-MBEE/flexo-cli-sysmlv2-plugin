package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

/**
 * Map command - Manage project and branch mappings between local and remote
 * 
 * Usage: flexo sysml map <subcommand>
 */
@Command(
        name = "map",
        description = "Manage project and branch mappings between local and remote",
        mixinStandardHelpOptions = true,
        subcommands = {
                MapCommand.AddCommand.class,
                MapCommand.RemoveCommand.class,
                MapCommand.ListCommand.class,
                MapCommand.ShowCommand.class,
                MapCommand.AddBranchCommand.class,
                MapCommand.RemoveBranchCommand.class,
                MapCommand.ListBranchesCommand.class
        }
)
public class MapCommand extends PluginCommand {

    @Override
    public void run() {
        // Default behavior: list mappings
        try {
            new ListCommand().run();
        } catch (Exception e) {
            error("Failed to list mappings: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * List all project mappings
     */
    @Command(
            name = "list",
            aliases = {"ls"},
            description = "List all project mappings",
            mixinStandardHelpOptions = true
    )
    static class ListCommand extends PluginCommand {
        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();
                Map<String, ProjectMapping> mappings = config.getProjectMappings();

                if (mappings.isEmpty()) {
                    info("No project mappings configured");
                    info("");
                    info("Add a mapping with: flexo sysml map add <local-project-id> <remote-name> <remote-project-id>");
                    return;
                }

                info("Project Mappings:");
                info("");
                info(String.format("%-30s %-20s %-30s", "Local Project ID", "Remote", "Remote Project ID"));
                info(String.format("%-30s %-20s %-30s", "---", "---", "---"));
                for (ProjectMapping mapping : mappings.values()) {
                    info(String.format("%-30s %-20s %-30s", 
                        mapping.getLocalProjectId(), 
                        mapping.getRemoteName(), 
                        mapping.getRemoteProjectId()));
                }
            } catch (Exception e) {
                error("Failed to list mappings: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to list mappings", e);
            }
        }
    }

    /**
     * Add a new project mapping
     */
    @Command(
            name = "add",
            description = "Map a local project to a remote project",
            mixinStandardHelpOptions = true
    )
    static class AddCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Parameters(index = "1", description = "Remote name")
        private String remoteName;

        @Parameters(index = "2", description = "Remote project ID")
        private String remoteProjectId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                // Check if mapping already exists
                if (config.hasProjectMapping(localProjectId)) {
                    ProjectMapping existing = config.getProjectMapping(localProjectId);
                    warn("Mapping already exists for local project '" + localProjectId + "'");
                    info("Current mapping: " + existing);
                    info("Use 'flexo sysml map remove " + localProjectId + "' first to replace it");
                    throw new RuntimeException("Mapping already exists for project: " + localProjectId);
                }

                // Check if remote exists
                if (!config.hasRemote(remoteName)) {
                    error("Remote '" + remoteName + "' does not exist");
                    info("Add it with: flexo sysml remote add " + remoteName + " <url>");
                    throw new RuntimeException("Remote does not exist: " + remoteName);
                }

                // Create mapping
                ProjectMapping mapping = new ProjectMapping(localProjectId, remoteName, remoteProjectId);
                config.setProjectMapping(mapping);
                config.save();

                success("Project mapping created:");
                info("  Local:  " + localProjectId);
                info("  Remote: " + remoteName + ":" + remoteProjectId);
                info("");
                info("You can now use:");
                info("  flexo sysml push " + localProjectId);
                info("  flexo sysml pull " + localProjectId);

            } catch (Exception e) {
                error("Failed to add mapping: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to add mapping", e);
            }
        }
    }

    /**
     * Remove a project mapping
     */
    @Command(
            name = "remove",
            aliases = {"rm"},
            description = "Remove a project mapping",
            mixinStandardHelpOptions = true
    )
    static class RemoveCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasProjectMapping(localProjectId)) {
                    error("No mapping found for project: " + localProjectId);
                    info("Use 'flexo sysml map list' to see configured mappings");
                    throw new RuntimeException("No mapping found for project: " + localProjectId);
                }

                config.removeProjectMapping(localProjectId);
                config.save();
                success("Mapping removed for project: " + localProjectId);

            } catch (Exception e) {
                error("Failed to remove mapping: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to remove mapping", e);
            }
        }
    }

    /**
     * Show details about a project mapping
     */
    @Command(
            name = "show",
            description = "Show details about a project mapping",
            mixinStandardHelpOptions = true
    )
    static class ShowCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                ProjectMapping mapping = config.getProjectMapping(localProjectId);
                if (mapping == null) {
                    error("No mapping found for project: " + localProjectId);
                    info("Use 'flexo sysml map list' to see configured mappings");
                    throw new RuntimeException("No mapping found for project: " + localProjectId);
                }

                info("Project Mapping:");
                info("  Local Project ID:  " + mapping.getLocalProjectId());
                info("  Remote Name:       " + mapping.getRemoteName());
                info("  Remote Project ID: " + mapping.getRemoteProjectId());

            } catch (Exception e) {
                error("Failed to show mapping: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to show mapping", e);
            }
        }
    }
    
    // ========== Branch Mapping Commands ==========
    
    /**
     * Add a branch mapping
     */
    @Command(
            name = "add-branch",
            description = "Map a local branch to a remote branch",
            mixinStandardHelpOptions = true
    )
    static class AddBranchCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Parameters(index = "1", description = "Local branch ID")
        private String localBranchId;

        @Parameters(index = "2", description = "Remote branch ID")
        private String remoteBranchId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                // Check if project mapping exists
                if (!config.hasProjectMapping(localProjectId)) {
                    error("No project mapping found for: " + localProjectId);
                    info("Create project mapping first: flexo sysml map add " + localProjectId + " <remote> <remote-project-id>");
                    throw new RuntimeException("Project mapping required before adding branch mapping");
                }

                // Check if branch mapping already exists
                if (config.hasBranchMapping(localProjectId, localBranchId)) {
                    BranchMapping existing = config.getBranchMapping(localProjectId, localBranchId);
                    warn("Branch mapping already exists: " + existing);
                    info("Use 'flexo sysml map remove-branch " + localProjectId + " " + localBranchId + "' first to replace it");
                    throw new RuntimeException("Branch mapping already exists");
                }

                // Create mapping
                BranchMapping mapping = new BranchMapping(localProjectId, localBranchId, remoteBranchId);
                config.setBranchMapping(mapping);
                config.save();

                success("Branch mapping created:");
                info("  Project:       " + localProjectId);
                info("  Local Branch:  " + localBranchId);
                info("  Remote Branch: " + remoteBranchId);

            } catch (Exception e) {
                error("Failed to add branch mapping: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to add branch mapping", e);
            }
        }
    }
    
    /**
     * Remove a branch mapping
     */
    @Command(
            name = "remove-branch",
            aliases = {"rm-branch"},
            description = "Remove a branch mapping",
            mixinStandardHelpOptions = true
    )
    static class RemoveBranchCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Parameters(index = "1", description = "Local branch ID")
        private String localBranchId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                if (!config.hasBranchMapping(localProjectId, localBranchId)) {
                    error("No branch mapping found for: " + localProjectId + ":" + localBranchId);
                    info("Use 'flexo sysml map list-branches " + localProjectId + "' to see configured mappings");
                    throw new RuntimeException("No branch mapping found");
                }

                config.removeBranchMapping(localProjectId, localBranchId);
                config.save();
                success("Branch mapping removed: " + localProjectId + ":" + localBranchId);

            } catch (Exception e) {
                error("Failed to remove branch mapping: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to remove branch mapping", e);
            }
        }
    }
    
    /**
     * List branch mappings for a project
     */
    @Command(
            name = "list-branches",
            aliases = {"ls-branches"},
            description = "List branch mappings for a project",
            mixinStandardHelpOptions = true
    )
    static class ListBranchesCommand extends PluginCommand {
        @Parameters(index = "0", description = "Local project ID")
        private String localProjectId;

        @Override
        public void run() {
            try {
                SysMLConfigHelper config = new SysMLConfigHelper();

                Map<String, BranchMapping> mappings = config.getBranchMappings(localProjectId);

                if (mappings.isEmpty()) {
                    info("No branch mappings for project: " + localProjectId);
                    info("");
                    info("Add a branch mapping with: flexo sysml map add-branch " + localProjectId + " <local-branch-id> <remote-branch-id>");
                    return;
                }

                info("Branch Mappings for Project: " + localProjectId);
                info("");
                info(String.format("%-40s %-40s", "Local Branch ID", "Remote Branch ID"));
                info(String.format("%-40s %-40s", "---", "---"));
                for (BranchMapping mapping : mappings.values()) {
                    info(String.format("%-40s %-40s", 
                        mapping.getLocalBranchId(), 
                        mapping.getRemoteBranchId()));
                }
            } catch (Exception e) {
                error("Failed to list branch mappings: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Failed to list branch mappings", e);
            }
        }
    }
}
