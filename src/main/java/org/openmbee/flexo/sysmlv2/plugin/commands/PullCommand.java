package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
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
 * This command translates remote project/branch IDs to local flexo IDs using mappings,
 * then delegates to the underlying Flexo CLI pull command.
 * 
 * Usage: flexo sysml pull <remote-project-id> [options]
 */
@Command(
    name = "pull",
    description = "Pull model from remote SysML v2 project",
    mixinStandardHelpOptions = true
)
public class PullCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Remote project ID")
    private String remoteProjectId;

    @Option(names = {"-b", "--branch"}, description = "Remote branch ID (optional)")
    private String remoteBranchId;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private String outputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Pulling from remote...");
            debug("Remote project ID: " + remoteProjectId);
            if (remoteBranchId != null) {
                debug("Remote branch ID: " + remoteBranchId);
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

            // Step 2: Load configuration and mappings
            SysMLConfigHelper sysmlConfig = new SysMLConfigHelper();
            
            // Step 3: Look up project mapping (remote -> local)
            ProjectMapping projectMapping = sysmlConfig.getProjectMappingByRemote(remoteName, remoteProjectId);
            String localProjectId;
            
            if (projectMapping == null) {
                // No mapping found - assume this is a local-only project
                info("  No project mapping found - treating as local-only project");
                localProjectId = remoteProjectId; // Use the provided ID as local ID
                remoteProjectId = localProjectId; // Keep them the same
            } else {
                localProjectId = projectMapping.getLocalProjectId();
                
                info("  Project mapping:");
                info("    Remote: " + remoteName + "/" + remoteProjectId);
                info("    Local:  " + localProjectId);
            }

            // Step 4: Get remote URL
            String remoteUrl = sysmlConfig.getRemoteUrl(remoteName);
            if (remoteUrl == null) {
                error("Remote '" + remoteName + "' not found in configuration");
                info("Add it with: flexo sysml remote add " + remoteName + " <url>");
                System.exit(1);
                return;
            }

            info("  Remote URL: " + remoteUrl);

            // Step 5: Look up branch mapping (remote -> local)
            String localBranchId = null;
            if (remoteBranchId != null && !remoteBranchId.isEmpty()) {
                // User specified a remote branch - look up its mapping
                BranchMapping branchMapping = sysmlConfig.getBranchMappingByRemote(localProjectId, remoteBranchId);
                if (branchMapping == null) {
                    // No mapping found - assume this is a local-only branch
                    info("  No branch mapping found - treating as local-only branch");
                    localBranchId = remoteBranchId; // Use the provided ID as local ID
                } else {
                    localBranchId = branchMapping.getLocalBranchId();
                    info("  Branch mapping:");
                    info("    Remote: " + remoteBranchId);
                    info("    Local:  " + localBranchId);
                }
            } else {
                // No branch specified - use remote project's default branch
                info("  No branch specified, fetching remote default branch...");
                try {
                    SysMLv2Client client = new SysMLv2Client(remoteUrl, getClient());
                    String remoteProjectResponse = client.getProject(remoteProjectId);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode remoteProject = mapper.readTree(remoteProjectResponse);
                    
                    if (remoteProject.has("defaultBranch") && remoteProject.get("defaultBranch").has("@id")) {
                        String remoteDefaultBranchId = remoteProject.get("defaultBranch").get("@id").asText();
                        info("  Remote default branch: " + remoteDefaultBranchId);
                        
                        // Try to find mapping for remote default branch
                        BranchMapping branchMapping = sysmlConfig.getBranchMappingByRemote(localProjectId, remoteDefaultBranchId);
                        if (branchMapping != null) {
                            localBranchId = branchMapping.getLocalBranchId();
                            remoteBranchId = remoteDefaultBranchId;
                            info("  Mapped to local branch: " + localBranchId);
                        } else {
                            // No mapping found - assume local-only branch
                            info("  No branch mapping found - treating as local-only branch");
                            localBranchId = remoteDefaultBranchId;
                            remoteBranchId = remoteDefaultBranchId;
                        }
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

            // Step 6: Build flexo pull command
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
            command.add(remoteProjectId);  // REMOTE project ID for the SysMLv2 API endpoint
            command.add("--remote");
            command.add(remoteName);
            
            if (remoteBranchId != null && !remoteBranchId.isEmpty()) {
                command.add("--branch");
                command.add(remoteBranchId);  // REMOTE branch ID for the SysMLv2 API endpoint
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
