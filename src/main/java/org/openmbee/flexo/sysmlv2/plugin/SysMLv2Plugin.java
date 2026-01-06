package org.openmbee.flexo.sysmlv2.plugin;

import org.openmbee.flexo.cli.plugin.FlexoPlugin;
import org.openmbee.flexo.cli.plugin.PluginContext;
import org.openmbee.flexo.sysmlv2.plugin.commands.SysMLCommand;
import picocli.CommandLine.Model.CommandSpec;

/**
 * SysML v2 Plugin for Flexo CLI
 *
 * Provides commands for interacting with SysML v2 API services:
 * - Project management
 * - Element operations
 * - Branch/commit operations
 * - Query execution
 * - Tag management
 * - Relationship navigation
 */
public class SysMLv2Plugin implements FlexoPlugin {
    private PluginContext context;

    @Override
    public String getName() {
        return "sysml";
    }

    @Override
    public String getDescription() {
        return "SysML v2 API operations";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void initialize(PluginContext context) {
        this.context = context;
    }

    @Override
    public CommandSpec getCommand() {
        SysMLCommand cmd = new SysMLCommand();
        cmd.setContext(context);
        return CommandSpec.forAnnotatedObject(cmd);
    }
}
