package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Element query and retrieval commands
 *
 * Note: The SysML v2 API requires element IDs to be UUIDs. Elements created
 * via the 'push' command with arbitrary URIs may not work with get/roots
 * operations. Use the 'list' command to see all elements, or use the SysML v2
 * API's createCommit endpoint to create elements with proper UUID-based IDs.
 *
 * Usage: flexo sysml element <subcommand>
 */
@Command(
    name = "element",
    description = "Query and retrieve elements",
    subcommands = {
        ElementCommand.ListCommand.class,
        ElementCommand.GetCommand.class,
        ElementCommand.RootsCommand.class
    }
)
public class ElementCommand extends SysMLBaseCommand {

    @Command(name = "list", description = "List elements in a project/commit")
    public static class ListCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--commit"}, description = "Commit ID (default: HEAD)")
        private String commitId = "HEAD";

        @Option(names = {"--page"}, description = "Page number (default: 0)")
        private int page = 0;

        @Option(names = {"--size"}, description = "Page size (default: 20)")
        private int size = 20;
        
        @Option(names = {"--exclude-used"}, description = "Exclude elements from ProjectUsages")
        private boolean excludeUsed = false;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                
                debug("Listing elements (project=" + projectId + ", commit=" + commitId + ")");

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                // Use new API with excludeUsed parameter
                String response = client.getElements(projectId, commitId, 
                    excludeUsed ? Boolean.TRUE : null, null, null, size > 0 ? size : null);

                // Parse and display elements
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (isVerbose()) {
                    debug("Raw response:");
                    debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
                }

                if (root.isArray() && root.size() > 0) {
                    info("Elements:");
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
                } else {
                    info("No elements found");
                }

            } catch (Exception e) {
                error("Failed to list elements: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "get", description = "Get element by ID")
    public static class GetCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--commit"}, description = "Commit ID (default: HEAD)")
        private String commitId = "HEAD";

        @Parameters(index = "0", description = "Element ID")
        private String elementId;
        
        @Option(names = {"--exclude-used"}, description = "Exclude elements from ProjectUsages")
        private boolean excludeUsed = false;

        @Override
        public void run() {
            try {
                debug("Getting element: " + elementId);

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
                    getClient()
                );

                // Use new API with excludeUsed parameter
                String response = client.getElement(projectId, commitId, elementId, 
                    excludeUsed ? Boolean.TRUE : null);

                // Parse and display element details
                ObjectMapper mapper = new ObjectMapper();
                JsonNode element = mapper.readTree(response);

                info("Element Details:");
                info("  ID: " + (element.has("@id") ? element.get("@id").asText() : "unknown"));
                if (element.has("@type")) {
                    info("  Type: " + element.get("@type").asText());
                }
                if (element.has("name")) {
                    info("  Name: " + element.get("name").asText());
                }
                if (element.has("owner")) {
                    info("  Owner: " + element.get("owner").asText());
                }

                if (isVerbose()) {
                    info("");
                    info("Full JSON:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(element));
                }

            } catch (Exception e) {
                error("Failed to get element: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "roots", description = "Get root elements in a project")
    public static class RootsCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--commit"}, description = "Commit ID (default: HEAD)")
        private String commitId = "HEAD";
        
        @Option(names = {"--exclude-used"}, description = "Exclude elements from ProjectUsages")
        private boolean excludeUsed = false;

        @Override
        public void run() {
            try {
                debug("Getting root elements (project=" + projectId + ", commit=" + commitId + ")");

                SysMLv2Client client = new SysMLv2Client(
                    getSysMLUrl(),
                    getClient()
                );

                // Use new API with excludeUsed parameter
                String response = client.getRootElements(projectId, commitId, 
                    excludeUsed ? Boolean.TRUE : null, null, null, null);

                // Parse and display root elements
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Root Elements:");
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
                } else {
                    info("No root elements found");
                }

            } catch (Exception e) {
                error("Failed to get root elements: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Element commands:");
        info("  list   - List elements in a project/commit");
        info("  get    - Get element by ID");
        info("  roots  - Get root elements in a project");
        info("");
        info("Use 'flexo sysml element <command> --help' for more information");
    }
}
