# Flexo SysML v2 Plugin Demo Procedure

This document provides a complete walkthrough of all flexo-cli-sysmlv2-plugin features.

## Architecture Overview

**User-Facing IDs**: Users work with REMOTE project and branch IDs (from the SysMLv2 server), similar to how git users work with remote branch names.

**Internal Mappings**: The plugin maintains internal mappings between REMOTE IDs (user-facing) and LOCAL flexo storage IDs. These mappings are created automatically during `clone` and are transparent to users.

**Local-Only Mode**: Projects without mappings are treated as local-only - the plugin uses the provided ID directly without requiring mapping configuration.

## Overview

The SysML v2 plugin extends Flexo CLI with commands for interacting with SysML v2 API services. It provides:
- Git-like remote management for multiple SysML v2 servers
- Project cloning with automatic ID mapping
- Pull/push operations using REMOTE project/branch IDs
- Local SysML v2 API deployment via Docker
- Complete SysML v2 API coverage (projects, elements, branches, commits, etc.)
- Support for both mapped (synced) and local-only projects

## Prerequisites

```bash
# Build the plugin
cd /path/to/flexo-cli-sysmlv2-plugin
./gradlew jar

# Copy to Flexo plugins directory
mkdir -p ~/.flexo/plugins
cp build/libs/flexo-cli-sysmlv2-plugin-1.0.0.jar ~/.flexo/plugins/

# Verify installation
flexo --help
# Should show 'sysml' command available
```

**Important**: The SysML v2 plugin requires a Flexo MMS backend service. This demo assumes you have the Flexo MMS Layer 1 service running on `http://localhost:8080`. If you need to set it up, run:

```bash
# Initialize Flexo MMS (if not already running)
flexo init
```

---

## Phase 0: Setup Dedicated SysML v2 Organization

Before using the SysML v2 plugin, create a dedicated organization in Flexo MMS to avoid conflicts with existing data:

```bash
# Create sysmlv2 organization in Flexo MMS
flexo init --org sysmlv2 --repo default

# This creates:
# - Organization: sysmlv2
# - Default repository: default
```

Expected output:
```
Initializing Flexo MMS...
Creating organization: sysmlv2
Creating repository: default
Initialization complete!
```

**Note**: The SysML v2 service will use this organization to store projects as UUID-based repositories.

---

## Phase 1: Initialization and Local Setup

### 1.1 Initialize Local SysML v2 Service

```bash
# Initialize with Docker (starts SysML v2 API on port 9000)
flexo sysml init

# With custom remote name
flexo sysml init --remote-name local

# Skip Docker if service already running
flexo sysml init --skip-docker --url http://existing-service:9000

# Use custom URL
flexo sysml init --url http://custom-host:9000
```

Expected output:
```
Initializing SysML v2 API service...
This will:
  1. Start Docker service (SysML v2 API on port 9000)
  2. Create remote 'origin' with URL: http://localhost:9000
Starting SysML v2 API service...
  Using docker-compose file: /tmp/sysmlv2-docker-compose-...yml
  Fuseki started
  Waiting for service on port 9000...
  sysmlv2-service is ready
  sysmlv2-service is healthy
  Remote 'origin' configured
Initialization complete!

Remote 'origin' configured with URL: http://localhost:9000
Set as default remote

You can now use the SysML v2 commands:
  flexo sysml project list
  flexo sysml project create --name "My Project"
```

### 1.2 Verify Service is Running

```bash
# Check Docker containers
docker ps | grep sysmlv2
```

Expected output:
```
CONTAINER ID   IMAGE                           ...   PORTS                    NAMES
abc123...      openmbee/flexo-sysmlv2:v0.1.0   ...   0.0.0.0:9000->9000/tcp   sysmlv2-service
```

**Note**: The `flexo sysml project list` command may return errors if the backend organization contains non-UUID repository IDs. This is expected if using an existing Flexo MMS instance. Individual project operations (create, get, update) will work correctly with UUID-based projects created through the SysML v2 API.

---

## Phase 2: Remote Management

### 2.1 List Remotes

```bash
flexo sysml remote list
# Or
flexo sysml remote ls
```

Expected output:
```
SysML v2 Remotes:
  origin * - http://localhost:9000
```

### 2.2 Add Multiple Remotes

```bash
# Add staging remote
flexo sysml remote add staging https://sysml-staging.example.com

# Add production remote and set as default
flexo sysml remote add production https://sysml.example.com --set-default

# List remotes again
flexo sysml remote list
```

Expected:
```
SysML v2 Remotes:
  origin - http://localhost:9000
  staging - https://sysml-staging.example.com
  production * - https://sysml.example.com
```

### 2.3 Remote Operations

```bash
# Show remote details
flexo sysml remote show production

# Update remote URL
flexo sysml remote set-url staging https://new-staging.example.com

# Rename remote
flexo sysml remote rename staging dev

# Remove remote
flexo sysml remote remove dev
```

---

## Phase 3: Project Management

### 3.1 Create Projects

```bash
# Create project on local instance
flexo sysml project create --name "Demo Project" --description "Project for demonstration"

# Create on specific remote
flexo --remote staging project create --name "Staging Project"
```

Expected output:
```
Using SysML v2 API at: http://localhost:9000
Creating project: Demo Project
Created project: c7906e60-ff9f-47da-b876-1968f35671c4
```

Save the project ID for later use: `c7906e60-ff9f-47da-b876-1968f35671c4`

**Note**: Project IDs are UUIDs generated by the SysML v2 API.

### 3.2 List Projects

**Important Limitation**: The `flexo sysml project list` command may fail with HTTP 500 error if the backend organization contains repositories with non-UUID IDs (e.g., "default", "localrepo"). This happens because:

1. The SysML v2 API standard requires all project IDs to be UUIDs
2. The backend may contain repos created with string IDs via `flexo init`
3. The list endpoint attempts to convert all backend repos to SysML v2 projects and fails on non-UUID IDs

**Workaround**: Use individual project operations (`get`, `update`, `delete`) which work correctly with UUID-based projects created through the SysML v2 API. Alternatively, use a dedicated backend organization that contains only SysML v2-created projects.

```bash
# List projects (may fail if backend has non-UUID repos)
# flexo sysml project list

# Instead, get individual projects by ID
flexo sysml project get --project c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected:
```
Project Details:
  ID: c7906e60-ff9f-47da-b876-1968f35671c4
  Name: Demo Project
  Description: Project for demonstration
```

### 3.3 Get Project Details

```bash
# Get project details
flexo sysml project get --project c7906e60-ff9f-47da-b876-1968f35671c4

# Verbose output (shows full JSON)
flexo sysml -v project get --project c7906e60-ff9f-47da-b876-1968f35671c4
```

### 3.4 Update and Delete Projects

```bash
# Update project
flexo sysml project update --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --name "Demo Project Updated" \
    --description "Updated description"

# Delete project (requires confirmation)
flexo sysml project delete --project <project-id> --confirm
```

---

## Phase 4: Project Cloning and Mapping

### 4.1 Clone Project from Remote

Scenario: You have a project on production that you want to work with locally.

```bash
# First, create a project on production (simulated)
flexo --remote production project create \
    --name "Production Model" \
    --description "Production SysML model"
# Assume this returns: proj-prod-abc123

# Clone the production project to local
flexo --remote production clone proj-prod-abc123 \
    --name "Dev Copy of Production Model"
```

Expected output:
```
Cloning project from remote...
  Remote project ID: proj-prod-abc123
  Remote: production

Step 1: Creating new local project...
  Created local project: c7906e60-ff9f-47da-b876-1968f35671c4
  Project name: Dev Copy of Production Model
  Local default branch ID: local-branch-uuid-123

Step 2: Fetching remote project details...
  Remote default branch ID: remote-branch-uuid-456

Step 3: Creating project mapping...
  Created project mapping: c7906e60-ff9f-47da-b876-1968f35671c4 <-> production/proj-prod-abc123

Step 4: Creating default branch mapping...
  Created branch mapping: local-branch-uuid-123 <-> remote-branch-uuid-456

Step 5: Pulling project data...
[Output from flexo pull command - actual data transfer]

Clone complete!

Local project ID: c7906e60-ff9f-47da-b876-1968f35671c4
Remote project ID: proj-prod-abc123
Remote: production

You can now work with this project using REMOTE IDs:
  flexo sysml pull proj-prod-abc123
  flexo sysml push proj-prod-abc123 -m "commit message"
```

### 4.2 Clone Specific Branch

```bash
# Clone only a specific branch
flexo --remote production clone proj-prod-abc123 \
    --branch feature-branch-id \
    --name "Feature Branch Copy"
```

**Note**: The `--branch` parameter expects a branch ID (UUID), not a branch name.

---

## Phase 5: Project and Branch Mapping

**Note**: Mappings are created automatically when you clone a project. You typically don't need to manage them manually unless you're working with projects created outside the clone workflow.

### 5.1 View Mappings

```bash
# List all project mappings
flexo sysml map list

# Show specific mapping details (use local project ID)
flexo sysml map show c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected output:
```
Project Mappings:

Local Project ID                      Remote               Remote Project ID
---                                   ---                  ---
c7906e60-ff9f-47da-b876-1968f35671c4  production           proj-prod-abc123
```

### 5.2 Manual Mapping Creation

```bash
# Create manual mapping (if you created a project outside of clone)
# Syntax: flexo sysml map add <local-project-id> <remote-name> <remote-project-id>
flexo sysml map add c7906e60-ff9f-47da-b876-1968f35671c4 production proj-prod-abc123

# Add branch mapping
# Syntax: flexo sysml map add-branch <local-project-id> <local-branch-id> <remote-branch-id>
flexo sysml map add-branch c7906e60-ff9f-47da-b876-1968f35671c4 \
    local-branch-guid \
    remote-branch-guid
```

### 5.3 Lookup Operations

```bash
# Find local project ID from remote project ID
flexo sysml map lookup proj-prod-abc123

# Lookup with specific remote
flexo sysml map lookup proj-prod-abc123 --remote-name production
```

Expected output:
```
Found mapping:
  Remote Project ID: proj-prod-abc123
  Remote Name:       production
  Local Project ID:  c7906e60-ff9f-47da-b876-1968f35671c4

Note: Use the REMOTE project ID (proj-prod-abc123) in your commands:
  flexo sysml pull proj-prod-abc123
  flexo sysml push proj-prod-abc123 -m "commit message"
```

### 5.4 List Branch Mappings

```bash
# List branch mappings for a project (use local project ID)
flexo sysml map list-branches c7906e60-ff9f-47da-b876-1968f35671c4
```

### 5.5 Remove Mappings

```bash
# Remove branch mapping (use local project ID and local branch ID)
flexo sysml map remove-branch c7906e60-ff9f-47da-b876-1968f35671c4 local-branch-guid

# Remove project mapping (use local project ID)
flexo sysml map remove c7906e60-ff9f-47da-b876-1968f35671c4
```

---

## Phase 6: Pull and Push Operations

**Important**: Use REMOTE project and branch IDs in pull/push commands. The plugin automatically handles the mapping to local flexo storage.

### 6.1 Pull Model from Remote

After cloning, you can pull updates from the remote:

```bash
# Pull from mapped project using REMOTE project ID (uses default branch)
flexo sysml pull proj-prod-abc123 --output current-model.ttl

# Pull specific branch using REMOTE branch ID
flexo sysml pull proj-prod-abc123 \
    --branch remote-branch-guid \
    --output model.ttl

# Pull in different format
flexo sysml pull proj-prod-abc123 \
    --format jsonld \
    --output model.jsonld

# Pull to stdout
flexo sysml pull proj-prod-abc123
```

Expected output:
```
Pulling from remote...
Remote project ID: proj-prod-abc123
  Project mapping:
    Remote: production/proj-prod-abc123
    Local:  c7906e60-ff9f-47da-b876-1968f35671c4
  Remote URL: https://sysml.example.com

Executing: flexo pull...
Fetched model with 142 statements
Saved to: current-model.ttl
Pull completed successfully
```

**Local-Only Projects**: If no mapping exists, the plugin treats the provided ID as a local-only project:
```bash
# Pull from local-only project (no remote mapping)
flexo sysml pull local-project-uuid
# Output: "No project mapping found - treating as local-only project"
```

### 6.2 Push Model to Remote

Make changes locally and push them back using REMOTE project and branch IDs:

```bash
# Create or edit a model file
cat > updated-model.ttl << 'EOF'
@prefix sysml: <http://www.omg.org/spec/SysML/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

sysml:NewElement a sysml:Part ;
    sysml:name "New Component" ;
    sysml:description "Added in local development" .
EOF

# Push to remote using REMOTE project ID
flexo sysml push proj-prod-abc123 \
    --message "Add new component" \
    --input updated-model.ttl

# Push to specific branch using REMOTE branch ID
flexo sysml push proj-prod-abc123 \
    --branch remote-branch-guid \
    --message "Update main branch" \
    --input updated-model.ttl

# Push from stdin
cat updated-model.ttl | flexo sysml push proj-prod-abc123 \
    --message "Update from pipeline"
```

Expected output:
```
Pushing to remote...
Remote project ID: proj-prod-abc123
  Project mapping:
    Remote: production/proj-prod-abc123
    Local:  c7906e60-ff9f-47da-b876-1968f35671c4
  Remote URL: https://sysml.example.com

Reading model from: updated-model.ttl
Parsed model with 3 statements
Executing: flexo push...
Model pushed successfully
Commit: commit-abc-xyz-123
Push completed successfully
```

### 6.3 Pull/Push Workflow

Complete development workflow using REMOTE IDs:

```bash
# 1. Clone project
flexo --remote production clone proj-prod-abc123 --name "Dev Copy"
# This creates mapping: local-uuid <-> production/proj-prod-abc123

# 2. Pull latest using REMOTE ID
flexo sysml pull proj-prod-abc123 --output current.ttl

# 3. Make changes (edit current.ttl)

# 4. Push changes using REMOTE ID
flexo sysml push proj-prod-abc123 \
    --message "Updated components" \
    --input current.ttl

# 5. Pull again to verify
flexo sysml pull proj-prod-abc123 --output verified.ttl
```

Expected output:
```
Pulling from remote...
Local project ID: proj-local-xyz789
  Project mapping:
    Local:  proj-local-xyz789
    Remote: production/proj-prod-abc123
  Remote URL: https://sysml.example.com

Executing: flexo pull...
Fetched model with 142 statements
Saved to: current-model.ttl
Pull completed successfully
```

### 6.2 Push Model to Remote

Make changes locally and push them back:

```bash
# Create or edit a model file
cat > updated-model.ttl << 'EOF'
@prefix sysml: <http://www.omg.org/spec/SysML/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

sysml:NewElement a sysml:Part ;
    sysml:name "New Component" ;
    sysml:description "Added in local development" .
EOF

# Push to remote
flexo sysml push proj-local-xyz789 \
    --message "Add new component" \
    --input updated-model.ttl

# Push to specific branch
flexo sysml push proj-local-xyz789 \
    --branch local-main-guid \
    --message "Update main branch" \
    --input updated-model.ttl

# Push from stdin
cat updated-model.ttl | flexo sysml push proj-local-xyz789 \
    --message "Update from pipeline"
```

Expected output:
```
Pushing to remote...
Local project ID: proj-local-xyz789
  Project mapping:
    Local:  proj-local-xyz789
    Remote: production/proj-prod-abc123
  Remote URL: https://sysml.example.com

Reading model from: updated-model.ttl
Parsed model with 3 statements
Executing: flexo push...
Model pushed successfully
Commit: commit-abc-xyz-123
Push completed successfully
```

### 6.3 Pull/Push Workflow

Complete development workflow:

```bash
# 1. Clone project
flexo --remote production clone proj-prod-abc123 --name "Dev Copy"
# Returns: proj-local-xyz789

# 2. Pull latest
flexo sysml pull proj-local-xyz789 --output current.ttl

# 3. Make changes (edit current.ttl)

# 4. Push changes
flexo sysml push proj-local-xyz789 \
    --message "Updated components" \
    --input current.ttl

# 5. Pull again to verify
flexo sysml pull proj-local-xyz789 --output verified.ttl
```

---

## Phase 7: Element Operations

### 7.1 List Elements

```bash
# List elements in a project/commit
flexo sysml element list \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123

# With pagination
flexo sysml element list \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    --page 0 \
    --size 20
```

### 7.2 Get Element Details

```bash
# Get specific element
flexo sysml element get \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    elem-xyz-789

# Verbose output
flexo sysml -v element get \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    elem-xyz-789
```

### 7.3 Get Root Elements

```bash
# Get root elements in project
flexo sysml element roots \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123
```

---

## Phase 8: Branch Operations

### 8.1 List Branches

```bash
# List branches in project (using project ID from Phase 3)
flexo sysml branch list --project 3a912c13-a609-4954-909d-b773be7edb0f

# On specific remote
flexo --remote production branch list \
    --project proj-prod-abc123

# With verbose output to see authentication details
flexo sysml -v branch list --project 3a912c13-a609-4954-909d-b773be7edb0f
```

Expected output:
```
Branches:
  Initial (ID: 88299563-581f-45e0-978a-99b5a70b5d2b)
```

### 8.2 Get Branch Details

```bash
# Get details for a specific branch
flexo sysml branch get \
    --project 3a912c13-a609-4954-909d-b773be7edb0f \
    --branch 88299563-581f-45e0-978a-99b5a70b5d2b
```

Expected output:
```
Branch Details:
  ID: 88299563-581f-45e0-978a-99b5a70b5d2b
  Name: Initial
  Description: Default branch
```

### 8.3 Create a New Branch

```bash
# Create a feature branch
flexo sysml branch create \
    --project 3a912c13-a609-4954-909d-b773be7edb0f \
    --name "feature-xyz" \
    --description "Development branch for feature XYZ"
```

Expected output:
```
Created branch: 12345678-abcd-efgh-ijkl-9876543210ab
  Name: feature-xyz
```

### 8.4 Branch Usage in Pull/Push Operations

Branches are primarily used when pulling and pushing model data:

```bash
# Pull from specific branch (will be covered in Phase 6)
flexo sysml pull proj-local-xyz789 \
    --branch local-main-guid \
    --output model.ttl

# Push to specific branch
flexo sysml push proj-local-xyz789 \
    --branch local-main-guid \
    --message "Update main branch" \
    --input updated-model.ttl
```

### 8.5 Delete a Branch

```bash
# Delete a feature branch when no longer needed
flexo sysml branch delete \
    --project 3a912c13-a609-4954-909d-b773be7edb0f \
    --branch 12345678-abcd-efgh-ijkl-9876543210ab
```

Expected output:
```
Branch deleted: 12345678-abcd-efgh-ijkl-9876543210ab
```

### 8.6 Branch Mapping for Multi-Environment Workflows

When cloning projects, branch mappings are automatically created to track which local branch corresponds to which remote branch:

```bash
# View branch mappings for a project
flexo sysml map list-branches proj-local-xyz789

# Manually add branch mapping (if needed)
flexo sysml map add-branch proj-local-xyz789 \
    local-branch-id \
    remote-branch-id
```

Expected output:
```
Branch mappings for project proj-local-xyz789:

Local Branch ID           Remote Branch ID
---                       ---
local-branch-abc          remote-branch-xyz
```

---

## Phase 9: Commit Operations

### 9.1 List Commits

```bash
# List commits in project
flexo sysml commit list --project proj-12345-abc-67890
```

**Note**: The `--branch` flag exists for backward compatibility but is ignored. The SysML v2 API does not support filtering commits by branch name.

---

## Phase 10: Query Operations

### 10.1 List Queries

```bash
# List queries in project
flexo sysml query list --project proj-12345-abc-67890

# On specific remote
flexo --remote production query list \
    --project proj-prod-abc123
```

### 10.2 Get Query Details

```bash
# Get details for a specific query
flexo sysml query get \
    --project proj-12345-abc-67890 \
    --query query-uuid-123
```

Expected output:
```
Query Details:
  ID: query-uuid-123
  Type: Query
```

### 10.3 Execute a Query

```bash
# Execute query at HEAD of default branch
flexo sysml query execute \
    --project proj-12345-abc-67890 \
    --query query-uuid-123

# Execute query at specific commit (point-in-time query)
flexo sysml query execute \
    --project proj-12345-abc-67890 \
    --query query-uuid-123 \
    --commit commit-abc-123
```

Expected output:
```
Query Results (15 elements):
  elem-123 (PartDefinition) - Spacecraft
  elem-456 (AttributeUsage) - mass
  elem-789 (ConnectionDefinition) - PowerConnection
  ...
```

**Use Case - Historical Analysis**:
```bash
# Execute query at current state
flexo sysml query execute \
    --project proj-12345-abc-67890 \
    --query safety-critical-elements

# Execute same query at previous release to compare
flexo sysml query execute \
    --project proj-12345-abc-67890 \
    --query safety-critical-elements \
    --commit v1.0-release-commit

# This allows you to see what changed between versions
```

---

## Phase 11: Tag Operations

### 11.1 List Tags

```bash
# List tags in project
flexo sysml tag list --project proj-12345-abc-67890

# On specific remote
flexo --remote production tag list \
    --project proj-prod-abc123
```

---

## Phase 12: Relationship Operations

### 12.1 Query Element Relationships

```bash
# List relationships for an element
flexo sysml relationship list \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    elem-xyz-789

# Filter by relationship direction
flexo sysml relationship list \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    --direction in \
    elem-xyz-789

# Exclude elements from ProjectUsages
flexo sysml relationship list \
    --project proj-12345-abc-67890 \
    --commit commit-abc-123 \
    --exclude-used \
    elem-xyz-789
```

Expected output:
```
Relationships:
  Ownership -> owner-elem-123
  ConnectionUsage -> connection-elem-456
  FeatureMembership -> feature-elem-789
```

---

## Phase 13: Local-Only vs Mapped Projects

### 13.1 Understanding Project Types

The plugin supports two types of projects:

**Mapped Projects** (have remote mappings):
```bash
# Created via clone - automatically mapped
flexo --remote production clone proj-prod-abc123

# Use REMOTE IDs in commands
flexo sysml pull proj-prod-abc123
flexo sysml push proj-prod-abc123 -m "update"
```

**Local-Only Projects** (no remote mappings):
```bash
# Created directly on local SysMLv2 API
flexo sysml project create --name "Local Project"
# Returns: local-proj-uuid

# Use project UUID directly - plugin treats as local-only
flexo sysml pull local-proj-uuid
# Output: "No project mapping found - treating as local-only project"
```

### 13.2 Working with Local-Only Projects

Local-only projects work without any mapping configuration:

```bash
# Pull from local project
flexo sysml pull local-proj-uuid --output local-model.ttl

# Push to local project
flexo sysml push local-proj-uuid \
    --message "Local changes" \
    --input local-model.ttl

# The plugin automatically detects no mapping exists and uses
# the provided ID as both the remote and local ID
```

### 13.3 Converting Local-Only to Mapped

If you later want to sync a local project with a remote:

```bash
# 1. Create corresponding project on remote
flexo --remote production project create --name "Synced Project"
# Returns: prod-proj-uuid

# 2. Create mapping
flexo sysml map add local-proj-uuid production prod-proj-uuid

# 3. Now you can use the remote ID
flexo sysml pull prod-proj-uuid
flexo sysml push prod-proj-uuid -m "sync to production"
```

---

## Phase 14: Complete Multi-Environment Workflow

### 14.1 Setup

```bash
# 1. Initialize local service
flexo sysml init

# 2. Add staging and production remotes
flexo sysml remote add staging https://sysml-staging.example.com
flexo sysml remote add production https://sysml.example.com
```

### 14.2 Development Flow

```bash
# 1. Clone from production using REMOTE ID
flexo --remote production clone proj-prod-model \
    --name "Local Dev Copy"
# This creates mapping: local-uuid <-> production/proj-prod-model

# 2. Work locally using REMOTE ID
flexo sysml pull proj-prod-model --output dev-model.ttl
# Edit dev-model.ttl

# 3. Push back to production using REMOTE ID
flexo sysml push proj-prod-model \
    --message "Added new subsystem" \
    --input dev-model.ttl

# 4. Verify on production
flexo --remote production project get --project proj-prod-model
```

### 14.3 Staging Workflow

```bash
# 1. Clone production to local
flexo --remote production clone proj-prod-model \
    --name "Staging Copy"
# Creates mapping: local-uuid-2 <-> production/proj-prod-model

# 2. Pull from production using REMOTE ID
flexo sysml pull proj-prod-model --output staging-model.ttl

# 3. Create project on staging
flexo --remote staging project create \
    --name "Staging Model" \
    --description "Staging environment"
# Returns: proj-staging-abc

# 4. Create second mapping for staging
# (This allows same local copy to sync with both production and staging)
flexo sysml map add local-uuid-2 staging proj-staging-abc

# 5. Push to staging using REMOTE ID
flexo sysml push proj-staging-abc \
    --message "Deploy to staging" \
    --input staging-model.ttl
```

**Note**: A single local project can be mapped to multiple remotes, enabling promotion workflows from dev → staging → production.

---

## Phase 15: Configuration Management

### 15.1 View Configuration

```bash
# View SysML v2 configuration
cat ~/.flexo/config | grep sysmlv2
```

Expected:
```
sysmlv2.remote.origin.url=http://localhost:9000
sysmlv2.remote.production.url=https://sysml.example.com
sysmlv2.default.remote=origin
sysmlv2.mapping.proj-local-xyz789.remote=production
sysmlv2.mapping.proj-local-xyz789.remoteProjectId=proj-prod-abc123
```

### 15.2 Configuration Structure

The plugin stores configuration in `~/.flexo/config` with the following keys:

- `sysmlv2.remote.<name>.url` - Remote URLs
- `sysmlv2.default.remote` - Default remote name
- `sysmlv2.mapping.<local-id>.remote` - Project mapping remote
- `sysmlv2.mapping.<local-id>.remoteProjectId` - Remote project ID
- `sysmlv2.mapping.<local-id>.branch.<local-branch>.remoteBranchId` - Branch mappings

---

## Phase 16: Verbose and Debug Output

### 16.1 Verbose Mode

```bash
# Enable verbose output
flexo sysml -v project list

# Verbose clone
flexo sysml -v --remote production clone proj-prod-abc123
```

### 16.2 Debug Output

```bash
# Debug shows internal operations
flexo sysml -v pull proj-local-xyz789
```

Expected (additional debug lines):
```
Using SysML v2 API at: http://localhost:9000
Local project ID: proj-local-xyz789
Mapped remote project ID 'proj-prod-abc123' to local ID 'proj-local-xyz789'
Command: flexo pull --org proj-prod-abc123 --repo default --remote production
...
```

---

## Phase 17: Error Handling

### 17.1 No Mapping Found (Treated as Local-Only)

```bash
# Try to pull from project without mapping
flexo sysml pull proj-unmapped-123
```

Expected output:
```
Pulling from remote...
Remote project ID: proj-unmapped-123
  No project mapping found - treating as local-only project
  Remote URL: http://localhost:9000

Executing: flexo pull...
[Proceeds with local-only project]
```

**Note**: The plugin no longer errors when mappings don't exist - it assumes local-only mode.

### 17.2 Remote Not Found

```bash
# Try to use non-existent remote
flexo --remote nonexistent project list
```

Expected error:
```
Remote 'nonexistent' not found in configuration
Add it with: flexo sysml remote add nonexistent <url>
```

### 17.3 Project Not Found

```bash
# Get non-existent project
flexo sysml project get --project proj-nonexistent
```

Expected error:
```
Failed to get project: HTTP 404: Project not found
```

---

## Phase 18: Advanced Features

### 18.1 Clone with Custom Name

```bash
# Clone with custom project name
flexo --remote production clone proj-prod-abc \
    --name "Custom Local Copy" \
    --description "Development copy"
```

### 18.2 Multi-Branch Development

```bash
# Clone project
flexo --remote production clone proj-prod-abc
# Creates mapping with default branch

# Create local branch mappings for additional branches
flexo sysml map add-branch local-proj-uuid \
    local-feature-branch-uuid \
    remote-feature-branch-uuid

# Pull specific branch using REMOTE branch ID
flexo sysml pull proj-prod-abc \
    --branch remote-feature-branch-uuid \
    --output feature.ttl

# Push to specific branch using REMOTE branch ID
flexo sysml push proj-prod-abc \
    --branch remote-feature-branch-uuid \
    --message "Feature update" \
    --input feature.ttl
```

---

## Cleanup

### Stop Docker Services

```bash
# Stop SysML v2 service
docker stop sysmlv2-service

# Or using docker-compose
docker-compose -f sysmlv2-docker-compose.yml down
```

### Clear Configuration

```bash
# Remove SysML v2 configuration
rm ~/.flexo/config

# Or manually edit to remove sysmlv2.* entries
nano ~/.flexo/config
```

---

## Summary of Commands

| Command | Description |
|---------|-------------|
| `flexo sysml init` | Initialize local SysML v2 API service |
| `flexo sysml clone` | Clone project from remote to local |
| `flexo sysml pull` | Pull model from remote (mapped) |
| `flexo sysml push` | Push model to remote (mapped) |
| `flexo sysml remote` | Manage SysML v2 remotes |
| `flexo sysml map` | Manage project/branch mappings |
| `flexo sysml map lookup` | Find local ID from remote ID |
| `flexo sysml project` | Manage projects (create, list, get, update, delete) |
| `flexo sysml element` | Query elements (list, get, roots) |
| `flexo sysml branch` | Manage branches (list, get, create, delete) |
| `flexo sysml commit` | List commits |
| `flexo sysml query` | Manage and execute queries (list, get, execute) |
| `flexo sysml tag` | List tags |
| `flexo sysml relationship` | Query relationships |

---

## Quick Reference

### Clone and Work Flow

```bash
# Setup
flexo sysml init
flexo sysml remote add prod https://sysml.example.com

# Clone (creates mapping automatically)
flexo --remote prod clone proj-remote-123 --name "Dev Copy"
# Creates local project and mapping

# Work using REMOTE project ID
flexo sysml pull proj-remote-123 --output model.ttl
# Edit model.ttl
flexo sysml push proj-remote-123 --message "Update" --input model.ttl
```

### Mapping Management

```bash
# View mappings
flexo sysml map list

# Lookup (use REMOTE ID)
flexo sysml map lookup proj-remote-123

# Manual mapping (use LOCAL ID, remote name, REMOTE ID)
flexo sysml map add local-proj-uuid prod proj-remote-123
```

---

## Next Steps

- Explore the [Flexo CLI Client](../flexo-cli-client/README.md)
- Read the [Plugin Architecture Guide](../flexo-cli-client/README-PLUGINS.md)
- Set up [authentication for production](../flexo-cli-client/README.md#authentication)
- Review the [full README](./README.md) for advanced features
