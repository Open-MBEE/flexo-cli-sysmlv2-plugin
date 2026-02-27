package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * Clone command - Clone a SysML v2 project from a remote to local
 * 
 * This command:
 * 1. Fetches project metadata from remote
 * 2. Creates a new local project
 * 3. Clones all branches
 * 4. Clones all elements from each branch
 * 5. Creates project and branch mappings automatically
 * 
 * Usage: flexo sysml clone <remote-project-id> [options]
 */
@Command(
    name = "clone",
    description = "Clone a SysML v2 project from remote to local",
    mixinStandardHelpOptions = true
)
public class CloneCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Remote project ID to clone")
    private String remoteProjectId;

    @Option(names = {"--name", "-n"}, description = "Local project name (default: use remote project name)")
    private String localProjectName;

    @Option(names = {"--description", "-d"}, description = "Local project description")
    private String localProjectDescription;

    @Option(names = {"--to-local"}, description = "Target local SysML v2 API URL (default: http://localhost:9000)")
    private String localUrl = "http://localhost:9000";

    @Option(names = {"--branch", "-b"}, description = "Clone specific branch only (default: clone all branches)")
    private String specificBranch;

    @Option(names = {"--dry-run"}, description = "Show what would be cloned without actually cloning")
    private boolean dryRun = false;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run() {
        try {
            info("Cloning project from remote...");
            info("  Remote project ID: " + remoteProjectId);
            
            // Get remote URL
            String remoteUrl = getSysMLUrl();
            String remoteName = getRemoteName();
            if (remoteName == null) {
                try {
                    SysMLConfigHelper config = new SysMLConfigHelper();
                    remoteName = config.getDefaultRemote();
                } catch (Exception e) {
                    remoteName = "origin";
                }
            }
            info("  Remote URL: " + remoteUrl + " (" + remoteName + ")");
            info("  Local URL: " + localUrl);
            info("");

            // Step 1: Fetch remote project
            SysMLv2Client remoteClient = new SysMLv2Client(remoteUrl, getClient());
            info("Step 1: Fetching remote project metadata...");
            String remoteProjectJson = remoteClient.getProject(remoteProjectId);
            JsonNode remoteProject = mapper.readTree(remoteProjectJson);
            
            String remoteProjectName = remoteProject.has("name") ? remoteProject.get("name").asText() : "Unnamed Project";
            String remoteProjectDesc = remoteProject.has("description") ? remoteProject.get("description").asText() : "";
            
            info("  Remote project: " + remoteProjectName);
            if (!remoteProjectDesc.isEmpty()) {
                info("  Description: " + remoteProjectDesc);
            }

            // Determine local project name
            if (localProjectName == null || localProjectName.isEmpty()) {
                localProjectName = remoteProjectName;
            }
            if (localProjectDescription == null || localProjectDescription.isEmpty()) {
                localProjectDescription = remoteProjectDesc;
            }

            if (dryRun) {
                info("");
                info("DRY RUN - Would create local project: " + localProjectName);
            }

            // Step 2: Create local project
            SysMLv2Client localClient = new SysMLv2Client(localUrl, getClient());
            String localProjectId;
            
            if (!dryRun) {
                info("");
                info("Step 2: Creating local project...");
                String localProjectJson = localClient.createProject(localProjectName, localProjectDescription);
                JsonNode localProject = mapper.readTree(localProjectJson);
                localProjectId = localProject.get("@id").asText();
                success("  Created local project: " + localProjectId);
            } else {
                localProjectId = "[would-be-created]";
                info("  Would create project: " + localProjectName);
            }

            // Step 3: Fetch branches
            info("");
            info("Step 3: Fetching branches...");
            String branchesJson = remoteClient.getBranches(remoteProjectId);
            JsonNode branches = mapper.readTree(branchesJson);
            
            if (!branches.isArray() || branches.size() == 0) {
                warn("  No branches found in remote project");
                return;
            }

            info("  Found " + branches.size() + " branch(es)");

            // Map to store branch mappings
            Map<String, String> branchMappings = new HashMap<>();

            // Step 4: Clone each branch
            int branchCount = 0;
            for (JsonNode branch : branches) {
                String remoteBranchId = branch.get("@id").asText();
                String branchName = branch.has("name") ? branch.get("name").asText() : remoteBranchId;

                // Skip if specific branch requested and this isn't it
                if (specificBranch != null && !branchName.equals(specificBranch) && !remoteBranchId.equals(specificBranch)) {
                    debug("  Skipping branch: " + branchName + " (not requested)");
                    continue;
                }

                branchCount++;
                info("");
                info("Step 4." + branchCount + ": Cloning branch '" + branchName + "'...");
                info("  Remote branch ID: " + remoteBranchId);

                // Get head commit for this branch
                String headCommitId = branch.has("head") ? branch.get("head").asText() : null;
                if (headCommitId == null) {
                    warn("  Branch has no head commit, skipping");
                    continue;
                }

                info("  Head commit: " + headCommitId);

                // Fetch all elements from this branch
                info("  Fetching elements...");
                int page = 0;
                int pageSize = 100;
                int totalElements = 0;
                ArrayNode allElements = mapper.createArrayNode();

                while (true) {
                    String elementsJson = remoteClient.getElements(remoteProjectId, headCommitId, page, pageSize);
                    JsonNode elementsPage = mapper.readTree(elementsJson);
                    
                    if (!elementsPage.isArray() || elementsPage.size() == 0) {
                        break;
                    }

                    for (JsonNode element : elementsPage) {
                        allElements.add(element);
                        totalElements++;
                    }

                    if (elementsPage.size() < pageSize) {
                        break; // Last page
                    }
                    page++;
                }

                info("  Found " + totalElements + " element(s)");

                if (!dryRun && totalElements > 0) {
                    // Create elements in local project
                    info("  Creating elements in local project...");
                    
                    // Note: We need to create elements in the local project's HEAD commit
                    // For now, we'll assume the local project has a default branch/commit
                    // In a real implementation, you might need to create a branch first
                    
                    // Get local project branches to find HEAD
                    String localBranchesJson = localClient.getBranches(localProjectId);
                    JsonNode localBranches = mapper.readTree(localBranchesJson);
                    
                    String localHeadCommitId = null;
                    String localBranchId = null;
                    if (localBranches.isArray() && localBranches.size() > 0) {
                        JsonNode localBranch = localBranches.get(0);
                        localBranchId = localBranch.get("@id").asText();
                        localHeadCommitId = localBranch.has("head") ? localBranch.get("head").asText() : null;
                    }

                    if (localHeadCommitId == null) {
                        warn("  Could not find local branch HEAD commit, skipping element creation");
                    } else {
                        info("  Using local commit: " + localHeadCommitId);
                        
                        // Create elements in batches
                        int batchSize = 50;
                        for (int i = 0; i < totalElements; i += batchSize) {
                            int end = Math.min(i + batchSize, totalElements);
                            ArrayNode batch = mapper.createArrayNode();
                            for (int j = i; j < end; j++) {
                                batch.add(allElements.get(j));
                            }
                            
                            String batchJson = mapper.writeValueAsString(batch);
                            localClient.createElements(localProjectId, localHeadCommitId, batchJson);
                            debug("  Created elements " + (i + 1) + "-" + end + " of " + totalElements);
                        }
                        
                        success("  Created " + totalElements + " element(s) in local project");
                        
                        // Store branch mapping
                        if (localBranchId != null) {
                            branchMappings.put(remoteBranchId, localBranchId);
                        }
                    }
                } else if (dryRun) {
                    info("  Would create " + totalElements + " element(s)");
                }
            }

            // Step 5: Create mappings
            if (!dryRun) {
                info("");
                info("Step 5: Creating project and branch mappings...");
                SysMLConfigHelper config = new SysMLConfigHelper();
                
                // Create project mapping
                ProjectMapping projectMapping = new ProjectMapping();
                projectMapping.setLocalProjectId(localProjectId);
                projectMapping.setRemoteName(remoteName);
                projectMapping.setRemoteProjectId(remoteProjectId);
                config.setProjectMapping(projectMapping);
                
                info("  Created project mapping: " + localProjectId + " <-> " + remoteName + "/" + remoteProjectId);
                
                // Create branch mappings
                for (Map.Entry<String, String> entry : branchMappings.entrySet()) {
                    BranchMapping branchMapping = new BranchMapping();
                    branchMapping.setLocalProjectId(localProjectId);
                    branchMapping.setLocalBranchId(entry.getValue());
                    branchMapping.setRemoteBranchId(entry.getKey());
                    config.setBranchMapping(branchMapping);
                    
                    debug("  Created branch mapping: " + entry.getValue() + " <-> " + entry.getKey());
                }
                
                if (branchMappings.size() > 0) {
                    info("  Created " + branchMappings.size() + " branch mapping(s)");
                }
                
                config.save();
                success("  Mappings saved to configuration");
            } else {
                info("");
                info("Would create project mapping: [local-project] <-> " + remoteName + "/" + remoteProjectId);
                info("Would create " + branchMappings.size() + " branch mapping(s)");
            }

            // Done!
            info("");
            if (!dryRun) {
                success("Clone complete!");
                info("");
                info("Local project ID: " + localProjectId);
                info("You can now work with this project using:");
                info("  flexo sysml project get --project " + localProjectId);
                info("  flexo sysml element list --project " + localProjectId);
            } else {
                success("Dry run complete - no changes made");
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
