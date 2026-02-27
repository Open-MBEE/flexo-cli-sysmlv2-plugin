package org.openmbee.flexo.sysmlv2.plugin.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Project management commands
 *
 * Usage: flexo sysml project <subcommand>
 */
@Command(
    name = "project",
    description = "Manage SysML projects"
)
public class ProjectCommand extends SysMLBaseCommand {

    @Command(name = "list", description = "List all projects")
    public static class ListCommand extends SysMLBaseCommand {
        @Option(names = {"--page"}, description = "Page number (default: 0)")
        private int page = 0;

        @Option(names = {"--size"}, description = "Page size (default: 20)")
        private int size = 20;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                debug("Using SysML v2 API at: " + url);
                debug("Listing projects (page=" + page + ", size=" + size + ")");

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                String response = client.getProjects(page, size);

                // Parse and display projects
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);

                if (root.isArray() && root.size() > 0) {
                    info("Projects:");
                    for (JsonNode project : root) {
                        String id = project.has("@id") ? project.get("@id").asText() : "unknown";
                        String name = project.has("name") ? project.get("name").asText() : "";

                        if (!name.isEmpty()) {
                            info("  " + id + " - " + name);
                        } else {
                            info("  " + id);
                        }
                    }
                } else {
                    info("No projects found");
                }

            } catch (Exception e) {
                error("Failed to list projects: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "get", description = "Get project details")
    public static class GetCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID (use --map-from to provide remote ID)")
        private String projectId;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                debug("Using SysML v2 API at: " + url);
                
                // Map project ID if --map-from is specified
                String actualProjectId = mapProjectId(projectId);
                if (!actualProjectId.equals(projectId)) {
                    info("Using mapped local project ID: " + actualProjectId);
                }
                
                debug("Getting project: " + actualProjectId);

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                String response = client.getProject(actualProjectId);

                // Parse and display project details
                ObjectMapper mapper = new ObjectMapper();
                JsonNode project = mapper.readTree(response);

                info("Project Details:");
                info("  ID: " + (project.has("@id") ? project.get("@id").asText() : "unknown"));
                if (project.has("name")) {
                    info("  Name: " + project.get("name").asText());
                }
                if (project.has("description")) {
                    info("  Description: " + project.get("description").asText());
                }

                if (isVerbose()) {
                    info("");
                    info("Full JSON:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(project));
                }

            } catch (Exception e) {
                error("Failed to get project: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "create", description = "Create a new project")
    public static class CreateCommand extends SysMLBaseCommand {
        @Option(names = {"--name", "-n"}, required = true, description = "Project name")
        private String name;

        @Option(names = {"--description", "-d"}, description = "Project description")
        private String description;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                debug("Using SysML v2 API at: " + url);
                debug("Creating project: " + name);

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                String response = client.createProject(name, description);

                // Parse response
                ObjectMapper mapper = new ObjectMapper();
                JsonNode project = mapper.readTree(response);

                String id = project.has("@id") ? project.get("@id").asText() : "unknown";
                success("Created project: " + id);

                if (isVerbose()) {
                    info("");
                    info("Full response:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(project));
                }

            } catch (Exception e) {
                error("Failed to create project: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "update", description = "Update a project")
    public static class UpdateCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--name", "-n"}, description = "New project name")
        private String name;

        @Option(names = {"--description", "-d"}, description = "New project description")
        private String description;

        @Override
        public void run() {
            try {
                String url = getSysMLUrl();
                debug("Using SysML v2 API at: " + url);
                debug("Updating project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                String response = client.updateProject(projectId, name, description);

                success("Updated project: " + projectId);

                if (isVerbose()) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode project = mapper.readTree(response);
                    info("");
                    info("Full response:");
                    info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(project));
                }

            } catch (Exception e) {
                error("Failed to update project: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Command(name = "delete", description = "Delete a project")
    public static class DeleteCommand extends SysMLBaseCommand {
        @Option(names = {"--project", "-p"}, required = true, description = "Project ID")
        private String projectId;

        @Option(names = {"--confirm"}, description = "Confirm deletion", required = true)
        private boolean confirm;

        @Override
        public void run() {
            if (!confirm) {
                error("Must specify --confirm to delete project");
                System.exit(1);
            }

            try {
                String url = getSysMLUrl();
                debug("Using SysML v2 API at: " + url);
                debug("Deleting project: " + projectId);

                SysMLv2Client client = new SysMLv2Client(
                    url,
                    getClient()
                );

                client.deleteProject(projectId);
                success("Deleted project: " + projectId);

            } catch (Exception e) {
                error("Failed to delete project: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }
    }

    @Override
    public void run() {
        info("Project commands:");
        info("  list    - List all projects");
        info("  get     - Get project details");
        info("  create  - Create a new project");
        info("  update  - Update a project");
        info("  delete  - Delete a project");
        info("");
        info("Use 'flexo sysml project <command> --help' for more information");
    }
}
