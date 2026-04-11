# Flexo SysML v2 Plugin Demo Procedure

This document provides a complete walkthrough of all flexo-cli-sysmlv2-plugin features.

## Architecture Overview

**Unified IDs**: The SysML v2 API now supports user-defined project and branch IDs. Projects and branches use consistent UUIDs across all environments, eliminating the need for ID mapping.

**Direct Operations**: Users work directly with project and branch UUIDs. When creating projects through the SysML v2 API, you can specify custom IDs or let the system auto-generate them.

**Simplified Workflow**: No more ID mapping or clone operations needed - projects are identified by the same ID everywhere.

## Overview

The SysML v2 plugin extends Flexo CLI with commands for interacting with SysML v2 API services. It provides:
- Git-like remote management for multiple SysML v2 servers
- Direct project operations with consistent UUIDs
- Pull/push operations using project/branch UUIDs
- Local SysML v2 API deployment via Docker
- Complete SysML v2 API coverage (projects, elements, branches, commits, etc.)
- Support for user-defined or auto-generated IDs

## Key Requirements

**Element IDs Must Be UUIDs**: The SysML v2 API requires element IDs to be UUIDs. When pushing model data:
- Use `urn:uuid:<uuid>` format for element URIs in RDF data
- Example: `<urn:uuid:550e8400-e29b-41d4-a716-446655440001> a sysml:PartDefinition ;`
- Non-UUID element IDs will work with `element list` but will fail with `element get`, `element roots`, and `relationship` operations

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

Before using the SysML v2 plugin, create a dedicated organization in Flexo MMS:

```bash
# Create sysmlv2 organization in Flexo MMS
# Use a UUID for the repo name to avoid project list issues
flexo init --org sysmlv2 --repo 00000000-0000-0000-0000-000000000000

# This creates:
# - Organization: sysmlv2
# - Default repository: 00000000-0000-0000-0000-000000000000 (UUID format)
```

Expected output:
```
Initializing Flexo MMS...
Creating organization: sysmlv2
Creating repository: 00000000-0000-0000-0000-000000000000
Initialization complete!
```

**Note**: Using a UUID for the default repo name ensures the `project list` command works correctly. The SysML v2 API requires all project IDs to be UUIDs.

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
  sysmlv2-service started
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
abc123...      openmbee/flexo-sysmlv2:latest   ...   0.0.0.0:9000->9000/tcp   sysmlv2-service
```

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
Configured SysML v2 remotes:
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
Configured SysML v2 remotes:
  origin - http://localhost:9000
  staging - https://sysml-staging.example.com
  production * - https://sysml.example.com
```

### 2.3 Remote Operations

```bash
# Show remote details
flexo sysml remote show origin

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
flexo --remote origin sysml project create --name "Demo Project" --description "Project for demonstration"

# Create on specific remote
flexo --remote staging sysml project create --name "Staging Project"
```

Expected output:
```
Created project: c7906e60-ff9f-47da-b876-1968f35671c4
Default branch ID: 88299563-581f-45e0-978a-99b5a70b5d2b
```

Save the project ID for later use: `c7906e60-ff9f-47da-b876-1968f35671c4`

**Note**: Project IDs are UUIDs generated by the SysML v2 API.

### 3.2 List Projects

```bash
# List all projects
flexo --remote origin sysml project list
```

**Troubleshooting**: If `project list` returns HTTP 500 error, ensure your organization was created with UUID-based repo names (see Phase 0). If the organization has non-UUID repos, use individual project operations instead:

```bash
# Get individual project by ID
flexo --remote origin sysml project get --project c7906e60-ff9f-47da-b876-1968f35671c4
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
flexo --remote origin sysml project get --project c7906e60-ff9f-47da-b876-1968f35671c4

# Verbose output (shows full JSON)
flexo --remote origin sysml -v project get --project c7906e60-ff9f-47da-b876-1968f35671c4
```

### 3.4 Update Projects

```bash
# Update project
flexo --remote origin sysml project update --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --name "Demo Project Updated" \
    --description "Updated description"
```

---

## Phase 4: Branch Operations

### 4.1 List Branches

```bash
# List branches in project
flexo --remote origin sysml branch list --project c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected output:
```
Branches:
  Initial (ID: 88299563-581f-45e0-978a-99b5a70b5d2b)
```

### 4.2 Get Branch Details

```bash
# Get details for a specific branch
flexo --remote origin sysml branch get \
    --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --branch 88299563-581f-45e0-978a-99b5a70b5d2b
```

Expected output:
```
Branch Details:
  ID: 88299563-581f-45e0-978a-99b5a70b5d2b
  Name: Initial
```

### 4.3 Create a New Branch

```bash
# Create a feature branch
flexo --remote origin sysml branch create \
    --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --name "feature-xyz" \
    --description "Development branch for feature XYZ"
```

Expected output:
```
Created branch: 12345678-abcd-efgh-ijkl-9876543210ab
  Name: feature-xyz
```

### 4.4 Delete a Branch

```bash
# Delete a feature branch when no longer needed
flexo --remote origin sysml branch delete \
    --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --branch 12345678-abcd-efgh-ijkl-9876543210ab
```

Expected output:
```
Branch deleted: 12345678-abcd-efgh-ijkl-9876543210ab
```

---

## Phase 5: Pull and Push Operations

### 5.1 Pull Model from Remote

```bash
# Pull from project using project UUID (uses default branch)
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"
flexo --remote origin sysml pull $PROJECT_ID --output current-model.ttl
```

Expected output:
```
Pulling from remote...
  Remote: origin
  Remote URL: http://localhost:9000
  No branch specified, fetching remote default branch...
  Using default branch: 88299563-581f-45e0-978a-99b5a70b5d2b

Executing: flexo pull...
Warning: No model data found in branch '88299563-581f-45e0-978a-99b5a70b5d2b'
Pull completed successfully
```

### 5.2 Push Model to Remote

**Important**: Use UUID-based element IDs in your RDF data for full compatibility with the SysML v2 API:

```bash
# Create model file with UUID-based element IDs
cat > model.ttl << 'EOF'
@prefix sysml: <http://www.omg.org/spec/SysML/> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .

<urn:uuid:550e8400-e29b-41d4-a716-446655440001> a sysml:PartDefinition ;
    rdfs:label "TestComponent" ;
    rdfs:comment "A test component for demonstration" .

<urn:uuid:550e8400-e29b-41d4-a716-446655440002> a sysml:PartDefinition ;
    rdfs:label "SubComponent" ;
    rdfs:comment "A sub-component" .
EOF

# Push to remote using project UUID
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"
flexo --remote origin sysml push $PROJECT_ID \
    --message "Add test components" \
    --input model.ttl
```

Expected output:
```
Pushing to remote...
  Remote: origin
  Remote URL: http://localhost:9000
  No branch specified, fetching remote default branch...
  Using default branch: 88299563-581f-45e0-978a-99b5a70b5d2b

Executing: flexo push...
Reading model from: model.ttl
Parsed model with 6 statements
Model pushed successfully
Commit: success
Push completed successfully
```

### 5.3 Pull/Push Workflow

```bash
# Complete development workflow
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"

# 1. Pull latest
flexo --remote origin sysml pull $PROJECT_ID --output current.ttl

# 2. Make changes (edit current.ttl with UUID-based element IDs)

# 3. Push changes
flexo --remote origin sysml push $PROJECT_ID \
    --message "Updated components" \
    --input current.ttl

# 4. Pull again to verify
flexo --remote origin sysml pull $PROJECT_ID --output verified.ttl
```

---

## Phase 6: Element Operations

**Note**: Element operations require actual commit UUIDs. The SysML v2 API does not support "HEAD" as a commit ID.

### 6.1 List Commits

```bash
# Get commit IDs first
flexo --remote origin sysml commit list --project c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected output:
```
Commits:
  ce05acb9-4aa1-4126-949c-fc2a3f6d6538
```

### 6.2 List Elements

```bash
# List elements using actual commit ID
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"
COMMIT_ID="ce05acb9-4aa1-4126-949c-fc2a3f6d6538"

flexo --remote origin sysml element list \
    --project $PROJECT_ID \
    --commit $COMMIT_ID
```

Expected output:
```
Elements:
  550e8400-e29b-41d4-a716-446655440002 (PartDefinition)
  550e8400-e29b-41d4-a716-446655440001 (PartDefinition)
```

### 6.3 Get Element Details

```bash
# Get specific element (must use UUID element ID)
flexo --remote origin sysml element get \
    --project $PROJECT_ID \
    --commit $COMMIT_ID \
    550e8400-e29b-41d4-a716-446655440001
```

Expected output:
```
Element Details:
  ID: 550e8400-e29b-41d4-a716-446655440001
```

### 6.4 Get Root Elements

```bash
# Get root elements in project
flexo --remote origin sysml element roots \
    --project $PROJECT_ID \
    --commit $COMMIT_ID
```

Expected output:
```
Root Elements:
  550e8400-e29b-41d4-a716-446655440002 (PartDefinition)
  550e8400-e29b-41d4-a716-446655440001 (PartDefinition)
```

---

## Phase 7: Relationship Operations

**Note**: Relationship operations require UUID-based element IDs. Elements with non-UUID IDs will not work.

```bash
# List relationships for an element
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"
COMMIT_ID="ce05acb9-4aa1-4126-949c-fc2a3f6d6538"
ELEMENT_ID="550e8400-e29b-41d4-a716-446655440001"

flexo --remote origin sysml relationship list \
    --project $PROJECT_ID \
    --commit $COMMIT_ID \
    $ELEMENT_ID
```

Expected output:
```
No relationships found
```

---

## Phase 8: Query Operations

### 8.1 List Queries

```bash
# List queries in project
flexo --remote origin sysml query list --project c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected output:
```
No queries found
```

### 8.2 Execute a Query

```bash
# Execute query at specific commit
flexo --remote origin sysml query execute \
    --project c7906e60-ff9f-47da-b876-1968f35671c4 \
    --query <query-uuid> \
    --commit <commit-uuid>
```

---

## Phase 9: Tag Operations

### 9.1 List Tags

```bash
# List tags in project
flexo --remote origin sysml tag list --project c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected output:
```
No tags found
```

---

## Phase 10: Multi-Environment Workflow

### 10.1 Understanding Project IDs

Projects use consistent UUIDs across all environments. The same project ID works on:
- Local development (origin remote)
- Staging servers
- Production servers

### 10.2 Syncing Across Remotes

```bash
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"

# 1. Pull from production
flexo --remote production sysml pull $PROJECT_ID --output model.ttl

# 2. Push to staging
flexo --remote staging sysml push $PROJECT_ID \
    --message "Sync from production" \
    --input model.ttl
```

---

## Phase 11: Configuration Management

### 11.1 View Configuration

```bash
# View SysML v2 configuration
cat ~/.flexo/config | grep sysmlv2
```

Expected:
```
sysmlv2.remote.origin.url=http://localhost:9000
sysmlv2.default.remote=origin
```

### 11.2 Configuration Structure

The plugin stores configuration in `~/.flexo/config` with the following keys:

- `sysmlv2.remote.<name>.url` - Remote URLs
- `sysmlv2.remote.<name>.flexoRemote` - Corresponding Flexo MMS remote (optional)
- `sysmlv2.default.remote` - Default remote name

---

## Phase 12: Verbose and Debug Output

### 12.1 Verbose Mode

```bash
# Enable verbose output
flexo --remote origin sysml -v project list

# Verbose pull
flexo --remote origin sysml -v pull c7906e60-ff9f-47da-b876-1968f35671c4
```

### 12.2 Debug Output

```bash
# Debug shows internal operations
flexo --remote origin sysml -v pull c7906e60-ff9f-47da-b876-1968f35671c4
```

Expected (additional debug lines):
```
Using SysML v2 API at: http://localhost:9000
Command: flexo pull --org sysmlv2 --repo c7906e60-ff9f-47da-b876-1968f35671c4 ...
```

---

## Phase 13: Error Handling

### 13.1 Remote Not Found

```bash
# Try to use non-existent remote
flexo --remote nonexistent sysml project list
```

Expected error:
```
Error: Failed to list projects: HTTP 500:
```

### 13.2 Project Not Found

```bash
# Get non-existent project
flexo --remote origin sysml project get --project nonexistent-id
```

Expected error:
```
Error: Failed to get project: HTTP 400:
```

### 13.3 Invalid Element ID

```bash
# Try to get element with non-UUID ID
flexo --remote origin sysml element get \
    --project $PROJECT_ID \
    --commit $COMMIT_ID \
    "http://example.org/SomeElement"
```

Expected error:
```
Error: Failed to get element: HTTP 400:
```

---

## Cleanup

### Stop Docker Services

```bash
# Stop SysML v2 service
docker stop sysmlv2-service

# Stop all Flexo services
docker stop sysmlv2-service layer1-service quad-store-server
```

### Clear Configuration

```bash
# Remove SysML v2 configuration
rm ~/.flexo/config
```

---

## Summary of Commands

| Command | Description |
|---------|-------------|
| `flexo sysml init` | Initialize local SysML v2 API service |
| `flexo sysml pull <project-id>` | Pull model from remote using project UUID |
| `flexo sysml push <project-id>` | Push model to remote using project UUID |
| `flexo sysml remote` | Manage SysML v2 remotes |
| `flexo sysml project` | Manage projects (create, list, get, update, delete) |
| `flexo sysml element` | Query elements (list, get, roots) |
| `flexo sysml branch` | Manage branches (list, get, create, delete) |
| `flexo sysml commit` | List commits |
| `flexo sysml query` | Manage and execute queries |
| `flexo sysml tag` | List tags |
| `flexo sysml relationship` | Query relationships |

---

## Quick Reference

### Basic Workflow

```bash
# Setup
flexo init --org sysmlv2 --repo 00000000-0000-0000-0000-000000000000
flexo sysml init

# Create project (returns UUID)
flexo --remote origin sysml project create --name "My Project"
# Returns: c7906e60-ff9f-47da-b876-1968f35671c4

# Create model with UUID element IDs
cat > model.ttl << 'EOF'
<urn:uuid:550e8400-e29b-41d4-a716-446655440001> a <http://www.omg.org/spec/SysML/PartDefinition> ;
    <http://www.w3.org/2000/01/rdf-schema#label> "Component" .
EOF

# Work with project using UUID
PROJECT_ID="c7906e60-ff9f-47da-b876-1968f35671c4"
flexo --remote origin sysml push $PROJECT_ID --message "Initial model" --input model.ttl
flexo --remote origin sysml pull $PROJECT_ID --output current.ttl

# Get commit ID for element operations
flexo --remote origin sysml commit list --project $PROJECT_ID

# List elements
flexo --remote origin sysml element list --project $PROJECT_ID --commit <commit-uuid>
```

### Element ID Requirements

- **Must use UUID format**: `<urn:uuid:550e8400-e29b-41d4-a716-446655440001>`
- **Non-UUID IDs** (like `<http://example.org/Component>`) will work with `element list` but fail with other element operations
- **Why**: The SysML v2 API parses element IDs as UUIDs for individual element operations

---

## Next Steps

- Explore the [Flexo CLI Client](../flexo-cli-client/README.md)
- Read the [Plugin Architecture Guide](../flexo-cli-client/README-PLUGINS.md)
- Set up [authentication for production](../flexo-cli-client/README.md#authentication)
- Review the [full README](./README.md) for advanced features