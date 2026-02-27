package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.plugin.PluginCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Commit management commands
 */
@Command(
    name = "commit",
    description = "Manage commits",
    subcommands = {
        CommitCommand.ListCommand.class
    }
)
public class CommitCommand extends PluginCommand {

    @Command(name = "list", description = "List commits in a project")
    public static class ListCommand extends PluginCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--branch", "-b"}, description = "Branch name")
        private String branch;

        @Override
        public void run() {
            try {
                debug("Listing commits for project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    getConfig().getMmsUrl(),
                    getClient()
                );

                String response = client.getCommits(projectId, branch);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Commits:");
                    for (JsonNode commit : root) {
                        String id = commit.has("@id") ? commit.get("@id").asText() : "unknown";
                        String message = commit.has("message") ? commit.get("message").asText() : "";
                        info("  " + id + (message.isEmpty() ? "" : " - " + message));
                    }
                } else {
                    info("No commits found");
                }

            } catch (Exception e) {
                error("Failed to list commits: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Commit commands:");
        info("  list - List commits in a project");
        info("");
        info("Use 'flexo sysml commit <command> --help' for more information");
    }
}
