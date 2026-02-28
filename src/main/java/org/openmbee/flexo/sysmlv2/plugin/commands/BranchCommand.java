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
        BranchCommand.ListCommand.class,
        BranchCommand.GetCommand.class,
        BranchCommand.CreateCommand.class,
        BranchCommand.DeleteCommand.class
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

    @Command(name = "get", description = "Get branch details by ID")
    public static class GetCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID (use --map-from to provide remote ID)")
        private String projectId;

        @Option(names = {"--branch", "-b"}, required = true, description = "Branch ID")
        private String branchId;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                
                String actualProjectId = mapProjectId(projectId);
                if (!actualProjectId.equals(projectId)) {
                    info("Using mapped local project ID: " + actualProjectId);
                }
                
                debug("Getting branch: " + branchId);

                SysMLv2Client client = new SysMLv2Client(url, getClient());
                String response = client.getBranch(actualProjectId, branchId);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode branch = mapper.readTree(response);

                info("Branch Details:");
                info("  ID: " + (branch.has("@id") ? branch.get("@id").asText() : "unknown"));
                if (branch.has("name")) {
                    info("  Name: " + branch.get("name").asText());
                }
                if (branch.has("description")) {
                    info("  Description: " + branch.get("description").asText());
                }

                if (isVerbose()) {
                    info("");
                    info("Full JSON:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(branch));
                }

            } catch (Exception e) {
                error("Failed to get branch: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "create", description = "Create a new branch")
    public static class CreateCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID (use --map-from to provide remote ID)")
        private String projectId;

        @Option(names = {"--name", "-n"}, required = true, description = "Branch name")
        private String name;

        @Option(names = {"--description", "-d"}, description = "Branch description")
        private String description;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                
                String actualProjectId = mapProjectId(projectId);
                if (!actualProjectId.equals(projectId)) {
                    info("Using mapped local project ID: " + actualProjectId);
                }
                
                debug("Creating branch: " + name);

                SysMLv2Client client = new SysMLv2Client(url, getClient());
                String response = client.createBranch(actualProjectId, name, description);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode branch = mapper.readTree(response);

                String branchId = branch.has("@id") ? branch.get("@id").asText() : "unknown";
                success("Created branch: " + branchId);
                info("  Name: " + name);

                if (isVerbose()) {
                    info("");
                    info("Full response:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(branch));
                }

            } catch (Exception e) {
                error("Failed to create branch: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "delete", description = "Delete a branch")
    public static class DeleteCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID (use --map-from to provide remote ID)")
        private String projectId;

        @Option(names = {"--branch", "-b"}, required = true, description = "Branch ID")
        private String branchId;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                
                String actualProjectId = mapProjectId(projectId);
                if (!actualProjectId.equals(projectId)) {
                    info("Using mapped local project ID: " + actualProjectId);
                }
                
                debug("Deleting branch: " + branchId);

                SysMLv2Client client = new SysMLv2Client(url, getClient());
                client.deleteBranch(actualProjectId, branchId);

                success("Branch deleted: " + branchId);

            } catch (Exception e) {
                error("Failed to delete branch: " + e.getMessage());
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
        info("  list   - List branches in a project");
        info("  get    - Get branch details by ID");
        info("  create - Create a new branch");
        info("  delete - Delete a branch");
        info("");
        info("Use 'flexo sysml branch <command> --help' for more information");
    }
}
