package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Query management commands
 */
@Command(
    name = "query",
    description = "Execute and manage queries",
    subcommands = {
        QueryCommand.ListCommand.class
    }
)
public class QueryCommand extends PluginCommand {

    @Command(name = "list", description = "List queries in a project")
    public static class ListCommand extends PluginCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Override
        public void run() {
            try {
                debug("Listing queries for project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    getConfig().getMmsUrl(),
                    getClient()
                );

                String response = client.getQueries(projectId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Queries:");
                    for (JsonNode query : root) {
                        String id = query.has("@id") ? query.get("@id").asText() : "unknown";
                        info("  " + id);
                    }
                } else {
                    info("No queries found");
                }

            } catch (Exception e) {
                error("Failed to list queries: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Query commands:");
        info("  list - List queries in a project");
        info("");
        info("Use 'flexo sysml query <command> --help' for more information");
    }
}
