package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tag management commands
 */
@Command(
    name = "tag",
    description = "Manage tags",
    subcommands = {
        TagCommand.ListCommand.class
    }
)
public class TagCommand extends SysMLBaseCommand {

    @Command(name = "list", description = "List tags in a project")
    public static class ListCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Override
        public void run() {
            try {
                debug("Listing tags for project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
                    getClient()
                );

                String response = client.getTags(projectId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Tags:");
                    for (JsonNode tag : root) {
                        String id = tag.has("@id") ? tag.get("@id").asText() : "unknown";
                        String name = tag.has("name") ? tag.get("name").asText() : id;
                        info("  " + name);
                    }
                } else {
                    info("No tags found");
                }

            } catch (Exception e) {
                error("Failed to list tags: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Tag commands:");
        info("  list - List tags in a project");
        info("");
        info("Use 'flexo sysml tag <command> --help' for more information");
    }
}
