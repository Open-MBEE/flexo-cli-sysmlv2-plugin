package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Root SysML v2 command
 *
 * Usage: flexo sysml <subcommand>
 */
@Command(
    name = "sysml",
    description = "SysML v2 API operations",
    subcommands = {
        InitCommand.class,
        CloneCommand.class,
        PullCommand.class,
        PushCommand.class,
        RemoteCommand.class,
        MapCommand.class,
        ProjectCommand.class,
        ElementCommand.class,
        BranchCommand.class,
        CommitCommand.class,
        QueryCommand.class,
        TagCommand.class,
        RelationshipCommand.class
    }
)
public class SysMLCommand extends PluginCommand {
    
    @Override
    public void run() {
        info("SysML v2 Plugin for Flexo CLI");
        info("");
        info("Available commands:");
        info("  init          - Initialize local SysML v2 API service");
        info("  clone         - Clone a project from remote to local");
        info("  pull          - Pull model from remote project");
        info("  push          - Push model changes to remote project");
        info("  remote        - Manage remote SysML v2 servers");
        info("  map           - Manage project mappings (local <-> remote)");
        info("  project       - Manage SysML projects");
        info("  element       - Query and retrieve elements");
        info("  branch        - Manage project branches");
        info("  commit        - Manage commits");
        info("  query         - Execute and manage queries");
        info("  tag           - Manage tags");
        info("  relationship  - Query element relationships");
        info("");
        info("Use 'flexo sysml <command> --help' for more information");
        info("Use 'flexo --remote <name> sysml <command>' to use a specific remote");
    }
}
