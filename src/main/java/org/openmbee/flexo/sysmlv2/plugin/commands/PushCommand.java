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

import java.util.ArrayList;
import java.util.List;

/**
 * Push command for SysML v2 - Commit model changes to a remote SysML project/branch
 * 
 * This command translates remote project/branch IDs to local flexo IDs using mappings,
 * then delegates to the underlying Flexo CLI push command.
 * 
 * Usage: flexo sysml push <remote-project-id> [options]
 * Call with remote project ID (no change to how you call). Internal logic uses remote repo/branch IDs
 * when pushing to a remote MMS so the target Layer1 finds the repo.
 */
@Command(
    name = "push",
    description = "Push model to remote SysML v2 project",
    mixinStandardHelpOptions = true
)
public class PushCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Remote project ID")
    private String remoteProjectId;

    @Option(names = {"-b", "--branch"}, description = "Remote branch ID (optional)")
    private String remoteBranchId;

    @Option(names = {"-m", "--message"}, description = "Commit message", required = true)
    private String commitMessage;

    @Option(names = {"-i", "--input"}, description = "Input file (default: stdin)")
    private String inputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Pushing to remote...");
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
                info("  No project mapping found - treating as local-only project");
                localProjectId = remoteProjectId;
                remoteProjectId = localProjectId;
            } else {
                localProjectId = projectMapping.getLocalProjectId();
                remoteProjectId = projectMapping.getRemoteProjectId();
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

            // Step 6: Build flexo push command
            info("Executing: flexo push...");
            
            // Get Flexo organization name from remote config (defaults to "sysmlv2")
            SysMLRemote remote = sysmlConfig.getRemote(remoteName);
            String flexoOrg = (remote != null) ? remote.getOrgOrDefault() : "sysmlv2";
            
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("push");
            command.add("--org");
            command.add(flexoOrg);
            // When pushing to a non-local remote (e.g. dev), the MMS only knows remote repo/branch IDs.
            // When pushing to local (origin), use local repo/branch IDs.
            boolean pushToLocal = "origin".equals(remoteName) || remoteName == null;
            String repoIdForPush = pushToLocal ? localProjectId : remoteProjectId;
            String branchIdForPush = pushToLocal ? localBranchId : remoteBranchId;
            command.add("--repo");
            command.add(repoIdForPush);
            command.add("--remote");
            command.add(remoteName);
            command.add("--message");
            command.add(commitMessage);
            
            if (branchIdForPush != null && !branchIdForPush.isEmpty()) {
                command.add("--branch");
                command.add(branchIdForPush);
            }
            
            if (inputFile != null && !inputFile.isEmpty()) {
                command.add("--input");
                command.add(inputFile);
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
                success("Push completed successfully");
            } else {
                error("Push failed with exit code: " + exitCode);
                System.exit(exitCode);
            }

        } catch (Exception e) {
            error("Push failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
