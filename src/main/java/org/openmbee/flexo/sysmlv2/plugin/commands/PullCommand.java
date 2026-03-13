package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Pull command for SysML v2 - Fetch model from a remote SysML project/branch
 * 
 * This command delegates to the underlying Flexo CLI pull command to fetch data
 * from the remote MMS backend.
 * 
 * Usage: flexo sysml pull <project-id> [options]
 */
@Command(
    name = "pull",
    description = "Pull model from remote SysML v2 project",
    mixinStandardHelpOptions = true
)
public class PullCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Project ID")
    private String projectId;

    @Option(names = {"-b", "--branch"}, description = "Branch ID (optional)")
    private String branchId;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private String outputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Pulling from remote...");
            debug("Project ID: " + projectId);
            if (branchId != null) {
                debug("Branch ID: " + branchId);
            }

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

            // Step 2: Load configuration
            SysMLConfigHelper sysmlConfig = new SysMLConfigHelper();
            
            // Step 3: Get remote URL
            String remoteUrl = sysmlConfig.getRemoteUrl(remoteName);
            if (remoteUrl == null) {
                error("Remote '" + remoteName + "' not found in configuration");
                info("Add it with: flexo sysml remote add " + remoteName + " <url>");
                System.exit(1);
                return;
            }

            info("  Remote: " + remoteName);
            info("  Remote URL: " + remoteUrl);

            // Step 4: If no branch specified, fetch remote project's default branch
            if (branchId == null || branchId.isEmpty()) {
                info("  No branch specified, fetching remote default branch...");
                try {
                    SysMLv2Client client = new SysMLv2Client(remoteUrl, getClient());
                    String remoteProjectResponse = client.getProject(projectId);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode remoteProject = mapper.readTree(remoteProjectResponse);
                    
                    if (remoteProject.has("defaultBranch") && remoteProject.get("defaultBranch").has("@id")) {
                        branchId = remoteProject.get("defaultBranch").get("@id").asText();
                        info("  Using default branch: " + branchId);
                    }
                } catch (Exception e) {
                    warn("Failed to fetch remote project's default branch: " + e.getMessage());
                    if (isVerbose()) {
                        e.printStackTrace();
                    }
                    // Continue without branch specification
                }
            }
            info("");

            // Step 5: Build flexo pull command
            info("Executing: flexo pull...");
            
            // Get Flexo organization name from remote config (defaults to "sysmlv2")
            SysMLRemote remote = sysmlConfig.getRemote(remoteName);
            String flexoOrg = (remote != null) ? remote.getOrgOrDefault() : "sysmlv2";
            
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("pull");
            command.add("--org");
            command.add(flexoOrg);  // Get org from remote config (configurable per remote)
            command.add("--repo");
            command.add(projectId);
            command.add("--remote");
            command.add(remoteName);
            
            if (branchId != null && !branchId.isEmpty()) {
                command.add("--branch");
                command.add(branchId);
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

            // Step 6: Execute the command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit stdin/stdout/stderr from parent process
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                success("Pull completed successfully");
            } else {
                error("Pull failed with exit code: " + exitCode);
                System.exit(exitCode);
            }

        } catch (Exception e) {
            error("Pull failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
