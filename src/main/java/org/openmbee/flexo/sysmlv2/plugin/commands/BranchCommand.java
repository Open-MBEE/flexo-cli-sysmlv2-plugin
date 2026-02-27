package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Branch management commands
 */
@Command(
    name = "branch",
    description = "Manage project branches",
    subcommands = {
        BranchCommand.ListCommand.class
    }
)
public class BranchCommand extends SysMLBaseCommand {

    @Command(name = "list", description = "List branches in a project")
    public static class ListCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID (use --map-from to provide remote ID)")
        private String projectId;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                
                // Map project ID if --map-from is specified
                String actualProjectId = mapProjectId(projectId);
                if (!actualProjectId.equals(projectId)) {
                    info("Using mapped local project ID: " + actualProjectId);
                }
                
                debug("Listing branches for project: " + actualProjectId);

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                String response = client.getBranches(actualProjectId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Branches:");
                    for (JsonNode branch : root) {
                        String id = branch.has("@id") ? branch.get("@id").asText() : "unknown";
                        String name = branch.has("name") ? branch.get("name").asText() : id;
                        info("  " + name + " (ID: " + id + ")");
                    }
                } else {
                    info("No branches found");
                }

            } catch (Exception e) {
                error("Failed to list branches: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Branch commands:");
        info("  list - List branches in a project");
        info("");
        info("Use 'flexo sysml branch <command> --help' for more information");
    }
}
