package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Relationship query commands
 */
@Command(
    name = "relationship",
    description = "Query element relationships"
)
public class RelationshipCommand extends PluginCommand {

    @Command(name = "list", description = "List relationships for an element")
    public static class ListCommand extends PluginCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--commit", "-c"}, description = "Commit ID (default: HEAD)")
        private String commitId = "HEAD";

        @Parameters(index = "0", description = "Element ID")
        private String elementId;

        @Override
        public void run() {
            try {
                debug("Listing relationships for element: " + elementId);

                SysMLv2Client client = new SysMLv2Client(
                    getConfig().getMmsUrl(),
                    getClient()
                );

                String response = client.getRelationships(projectId, commitId, elementId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Relationships:");
                    for (JsonNode rel : root) {
                        String type = rel.has("@type") ? rel.get("@type").asText() : "unknown";
                        String target = rel.has("target") ? rel.get("target").asText() : "";
                        info("  " + type + " -> " + target);
                    }
                } else {
                    info("No relationships found");
                }

            } catch (Exception e) {
                error("Failed to list relationships: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Relationship commands:");
        info("  list - List relationships for an element");
        info("");
        info("Use 'flexo sysml relationship <command> --help' for more information");
    }
}
