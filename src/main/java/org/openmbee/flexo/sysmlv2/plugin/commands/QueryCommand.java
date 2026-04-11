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
        QueryCommand.ListCommand.class,
        QueryCommand.GetCommand.class,
        QueryCommand.ExecuteCommand.class
    }
)
public class QueryCommand extends SysMLBaseCommand {

    @Command(name = "list", description = "List queries in a project")
    public static class ListCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Override
        public void run() {
            try {
                debug("Listing queries for project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
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

    @Command(name = "get", description = "Get query details by ID")
    public static class GetCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--query", "-q"}, required = true, description = "Query ID")
        private String queryId;

        @Override
        public void run() {
            try {
                debug("Getting query: " + queryId);

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
                    getClient()
                );

                String response = client.getQuery(projectId, queryId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode query = mapper.readTree(response);

                info("Query Details:");
                info("  ID: " + (query.has("@id") ? query.get("@id").asText() : "unknown"));
                if (query.has("@type")) {
                    info("  Type: " + query.get("@type").asText());
                }

                if (isVerbose()) {
                    info("");
                    info("Full JSON:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(query));
                }

            } catch (Exception e) {
                error("Failed to get query: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "execute", description = "Execute a query and get results")
    public static class ExecuteCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--query", "-q"}, required = true, description = "Query ID")
        private String queryId;

        @Option(names = {"--commit"}, description = "Commit ID (default: HEAD of default branch)")
        private String commitId;

        @Override
        public void run() {
            try {
                if (commitId != null && !commitId.isEmpty()) {
                    debug("Executing query " + queryId + " at commit " + commitId);
                } else {
                    debug("Executing query " + queryId + " at HEAD");
                }

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
                    getClient()
                );

                String response = client.getQueryResults(projectId, queryId, commitId, null, null, null);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Query Results (" + root.size() + " elements):");
                    for (JsonNode element : root) {
                        String id = element.has("@id") ? element.get("@id").asText() : "unknown";
                        String type = element.has("@type") ? element.get("@type").asText() : "";
                        String name = element.has("name") ? element.get("name").asText() : "";

                        if (!name.isEmpty()) {
                            info("  " + id + " (" + type + ") - " + name);
                        } else {
                            info("  " + id + " (" + type + ")");
                        }
                    }

                    if (isVerbose()) {
                        info("");
                        info("Full JSON:");
                        info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
                    }
                } else {
                    info("No results found");
                }

            } catch (Exception e) {
                error("Failed to execute query: " + e.getMessage());
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
        info("  list    - List queries in a project");
        info("  get     - Get query details by ID");
        info("  execute - Execute a query and get results");
        info("");
        info("Use 'flexo sysml query <command> --help' for more information");
    }
}
