package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Clone command - Clone a SysML v2 project from a remote
 * 
 * This command:
 * 1. Creates a new local SysML v2 project
 * 2. Creates a project mapping (remote project ID -> local project ID)
 * 3. Delegates to the parent Flexo CLI pull command to fetch the data
 * 
 * Usage: flexo sysml clone <remote-project-id> [options]
 */
@Command(
    name = "clone",
    description = "Clone a SysML v2 project from remote",
    mixinStandardHelpOptions = true
)
public class CloneCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Remote project ID to clone")
    private String remoteProjectId;

    @Option(names = {"--name", "-n"}, description = "Name for the new local project (default: same as remote project)")
    private String projectName;

    @Option(names = {"--description", "-d"}, description = "Description for the new local project")
    private String projectDescription;

    @Option(names = {"--branch", "-b"}, description = "Clone specific branch only (default: clone all branches)")
    private String specificBranch;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private String outputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Cloning project from remote...");
            info("  Remote project ID: " + remoteProjectId);
            info("");

            // Step 1: Get remote name from context
            String remoteName = getRemoteName();
            if (remoteName == null) {
                try {
                    SysMLConfigHelper config = new SysMLConfigHelper();
                    remoteName = config.getDefaultRemote();
                } catch (Exception e) {
                    remoteName = "origin";
                }
            }
            info("  Remote: " + remoteName);

            // Step 2: Create a new local project
            info("");
            info("Step 1: Creating new local project...");
            
            String url = getSysMLUrl();
            debug("Using SysML v2 API at: " + url);
            
            SysMLv2Client client = new SysMLv2Client(url, getClient());
            
            // Use provided name or default to remote project ID
            String localProjectName = (projectName != null && !projectName.isEmpty()) 
                ? projectName 
                : "clone-of-" + remoteProjectId;
            
            String response = client.createProject(localProjectName, projectDescription);
            
            // Parse response to get the new project ID
            ObjectMapper mapper = new ObjectMapper();
            JsonNode project = mapper.readTree(response);
            String localProjectId = project.has("@id") ? project.get("@id").asText() : null;
            
            if (localProjectId == null || localProjectId.isEmpty()) {
                error("Failed to create local project: No project ID returned");
                System.exit(1);
                return;
            }
            
            success("  Created local project: " + localProjectId);
            info("  Project name: " + localProjectName);

            // Step 3: Create project mapping
            info("");
            info("Step 2: Creating project mapping...");
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            ProjectMapping projectMapping = new ProjectMapping();
            projectMapping.setLocalProjectId(localProjectId);
            projectMapping.setRemoteName(remoteName);
            projectMapping.setRemoteProjectId(remoteProjectId);
            config.setProjectMapping(projectMapping);
            config.save();
            
            success("  Created project mapping: " + localProjectId + " <-> " + remoteName + "/" + remoteProjectId);

            // Step 4: Build flexo pull command to fetch the data
            info("");
            info("Step 3: Pulling project data...");
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("pull");
            command.add("--org");
            command.add(remoteProjectId);
            command.add("--repo");
            command.add("default");  // SysML v2 uses a default repo concept
            command.add("--remote");
            command.add(remoteName);
            
            if (specificBranch != null && !specificBranch.isEmpty()) {
                command.add("--branch");
                command.add(specificBranch);
            }
            
            if (outputFile != null && !outputFile.isEmpty()) {
                command.add("--output");
                command.add(outputFile);
            }
            
            if (format != null && !format.isEmpty()) {
                command.add("--format");
                command.add(format);
            }

            if (isVerbose()) {
                debug("Command: " + String.join(" ", command));
            }

            // Step 5: Execute the pull command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit stdin/stdout/stderr from parent process
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                info("");
                success("Clone complete!");
                info("");
                info("Local project ID: " + localProjectId);
                info("Remote project ID: " + remoteProjectId);
                info("Remote: " + remoteName);
                info("");
                info("You can now work with this project using:");
                info("  flexo sysml pull " + localProjectId);
                info("  flexo sysml push " + localProjectId + " -m \"commit message\"");
            } else {
                error("Clone failed with exit code: " + exitCode);
                System.exit(exitCode);
            }

        } catch (Exception e) {
            error("Clone failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
