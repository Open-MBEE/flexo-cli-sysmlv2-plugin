package org.openmbee.flexo.sysmlv2.plugin.client;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openmbee.flexo.cli.client.FlexoMmsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for SysML v2 API operations
 *
 * Wraps the SysML v2 API exposed by flexo-mms-sysmlv2 service.
 * Uses FlexoMmsClient for authentication headers.
 * 
 * IMPORTANT: Creating/Updating Elements
 * =====================================
 * The SysML v2 API does NOT support direct POST to /projects/{id}/commits/{id}/elements.
 * Instead, elements must be created/updated through commits using the POST /projects/{id}/commits endpoint.
 * 
 * Use createCommit() method with a properly formatted CommitRequest JSON:
 * 
 * Example:
 * <pre>
 * {
 *   "@type": "Commit",
 *   "change": [
 *     {
 *       "@type": "DataVersion",
 *       "identity": {"@id": "bb1d79c2-1306-5b35-a807-93e46fc3431c"},
 *       "payload": {
 *         "@type": "PartDefinition",
 *         "name": "Spacecraft System",
 *         "connectionEnd": [
 *           {"@id": "e03dab82-7485-4aa3-a63c-3e9bed70ad82"}
 *         ]
 *       }
 *     }
 *   ],
 *   "description": "Add spacecraft system"
 * }
 * </pre>
 * 
 * Helper methods buildCommitRequest() and buildCommitRequestMultiple() are provided
 * to simplify CommitRequest JSON construction.
 */
public class SysMLv2Client {
    private static final Logger logger = LoggerFactory.getLogger(SysMLv2Client.class);

    private final String baseUrl;
    private final FlexoMmsClient flexoClient;
    private final CloseableHttpClient httpClient;

    public SysMLv2Client(String baseUrl, FlexoMmsClient flexoClient) {
        this.baseUrl = baseUrl.replaceAll("/$", ""); // Remove trailing slash
        this.flexoClient = flexoClient;
        this.httpClient = HttpClients.createDefault();
    }

    // ============ Project Operations ============

    /**
     * Get all projects with cursor-based pagination
     * 
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional, default from API)
     * @return JSON response with projects
     */
    public String getProjects(String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects";
        List<String> params = new ArrayList<>();
        
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get root elements (convenience method for backward compatibility)
     */
    public String getRootElements(String projectId, String commitId) throws IOException {
        return getRootElements(projectId, commitId, null, null, null, null);
    }


    /**
     * Get elements (convenience method for backward compatibility)
     */
    public String getElements(String projectId, String commitId, int page, int size) throws IOException {
        return getElements(projectId, commitId, null, null, null, size > 0 ? size : null);
    }

    /**
     * Get all projects (convenience method for backward compatibility)
     * Uses simple page number and size (converts to cursor-based pagination)
     */
    public String getProjects(int page, int size) throws IOException {
        // For backward compatibility, just use page size
        // Cursor-based pagination is more complex and requires previous responses
        return getProjects(null, null, size > 0 ? size : null);
    }

    /**
     * Get project by ID
     */
    public String getProject(String projectId) throws IOException {
        String url = baseUrl + "/projects/" + projectId;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    /**
     * Create a new project
     */
    public String createProject(String name, String description) throws IOException {
        String url = baseUrl + "/projects";

        // Build JSON body - SysML v2 API requires @type field
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"@type\":\"Project\"");
        jsonBody.append(",\"name\":\"").append(escapeJson(name)).append("\"");
        if (description != null && !description.isEmpty()) {
            jsonBody.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        jsonBody.append("}");

        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON));

        return executeRequest(request);
    }

    /**
     * Create a project with a specific ID
     */
    public String createProjectWithId(String projectId, String name, String description, String defaultBranchId) throws IOException {
        String url = baseUrl + "/projects";

        // Build JSON body - Include @id field to specify the project ID
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"@type\":\"Project\"");
        jsonBody.append(",\"@id\":\"").append(escapeJson(projectId)).append("\"");
        jsonBody.append(",\"name\":\"").append(escapeJson(name)).append("\"");
        if (description != null && !description.isEmpty()) {
            jsonBody.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        if (defaultBranchId != null && !defaultBranchId.isEmpty()) {
            jsonBody.append(",\"defaultBranch\":{\"@id\":\"").append(escapeJson(defaultBranchId)).append("\"}");
        }
        jsonBody.append("}");

        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON));

        return executeRequest(request);
    }

    /**
     * Update a project
     */
    public String updateProject(String projectId, String name, String description) throws IOException {
        String url = baseUrl + "/projects/" + projectId;

        // Build JSON body - SysML v2 API requires @type field
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"@type\":\"Project\"");
        if (name != null && !name.isEmpty()) {
            jsonBody.append(",\"name\":\"").append(escapeJson(name)).append("\"");
        }
        if (description != null && !description.isEmpty()) {
            jsonBody.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        jsonBody.append("}");

        HttpPut request = new HttpPut(url);
        addAuthHeader(request);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON));

        return executeRequest(request);
    }

    /**
     * Delete a project
     */
    public void deleteProject(String projectId) throws IOException {
        String url = baseUrl + "/projects/" + projectId;
        HttpDelete request = new HttpDelete(url);
        addAuthHeader(request);

        executeRequest(request);
    }

    // ============ Element Operations ============

    /**
     * Get elements in a project/commit with filtering and pagination
     * 
     * @param projectId Project UUID
     * @param commitId Commit UUID
     * @param excludeUsed Exclude elements from ProjectUsages (optional)
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with elements
     */
    public String getElements(String projectId, String commitId, Boolean excludeUsed,
                             String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements";
        List<String> params = new ArrayList<>();
        
        if (excludeUsed != null) {
            params.add("excludeUsed=" + excludeUsed);
        }
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    /**
     * Get element by ID
     * 
     * @param projectId Project UUID
     * @param commitId Commit UUID
     * @param elementId Element UUID
     * @param excludeUsed Exclude elements from ProjectUsages (optional)
     * @return JSON response with element
     */
    public String getElement(String projectId, String commitId, String elementId, Boolean excludeUsed) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements/" + elementId;
        
        if (excludeUsed != null) {
            url += "?excludeUsed=" + excludeUsed;
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get element by ID (convenience method for backward compatibility)
     */
    public String getElement(String projectId, String commitId, String elementId) throws IOException {
        return getElement(projectId, commitId, elementId, null);
    }

    /**
     * Get root elements in a project with filtering and pagination
     * 
     * @param projectId Project UUID
     * @param commitId Commit UUID
     * @param excludeUsed Exclude elements from ProjectUsages (optional)
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with root elements
     */
    public String getRootElements(String projectId, String commitId, Boolean excludeUsed,
                                  String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/roots";
        List<String> params = new ArrayList<>();
        
        if (excludeUsed != null) {
            params.add("excludeUsed=" + excludeUsed);
        }
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    /**
     * Create a commit with elements
     * 
     * This is the correct way to create or update elements in SysML v2 API.
     * Elements must be wrapped in a CommitRequest structure with a "change" array
     * of DataVersionRequest objects.
     * 
     * @param projectId The project UUID
     * @param commitRequestJson JSON string containing a CommitRequest object with structure:
     *                          {
     *                            "@type": "Commit",
     *                            "change": [
     *                              {
     *                                "@type": "DataVersion",
     *                                "identity": {"@id": "uuid-string"},
     *                                "payload": {
     *                                  "@type": "ElementType",
     *                                  "@id": "uuid-string",
     *                                  // ... element properties
     *                                }
     *                              }
     *                            ],
     *                            "description": "Commit message"
     *                          }
     * @param branchId Optional branch UUID (uses default branch if null)
     * @return The created commit as JSON
     */
    public String createCommit(String projectId, String commitRequestJson, String branchId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits";
        if (branchId != null && !branchId.isEmpty()) {
            url += "?branchId=" + branchId;
        }
        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(commitRequestJson, ContentType.APPLICATION_JSON));

        return executeRequest(request);
    }

    /**
     * @deprecated Use createCommit() instead. This method calls a non-existent endpoint.
     * The SysML v2 API does not support POST to /projects/{projectId}/commits/{commitId}/elements.
     * Instead, use createCommit() with a proper CommitRequest structure.
     */
    @Deprecated
    public String createElements(String projectId, String commitId, String elementsJson) throws IOException {
        throw new UnsupportedOperationException(
            "createElements() is deprecated and calls a non-existent endpoint. " +
            "Use createCommit() instead with a proper CommitRequest structure. " +
            "See SysML v2 API documentation for CommitRequest format."
        );
    }

    // ============ Branch Operations ============

    /**
     * Get branches in a project with pagination
     * 
     * @param projectId Project UUID
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with branches
     */
    public String getBranches(String projectId, String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/branches";
        List<String> params = new ArrayList<>();
        
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get branches (convenience method for backward compatibility)
     */
    public String getBranches(String projectId) throws IOException {
        return getBranches(projectId, null, null, null);
    }
    
    /**
     * Get branch by ID
     * 
     * @param projectId Project UUID
     * @param branchId Branch UUID
     * @return JSON response with branch details
     */
    public String getBranch(String projectId, String branchId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/branches/" + branchId;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");
        
        return executeRequest(request);
    }
    
    /**
     * Create a new branch
     * 
     * @param projectId Project UUID
     * @param name Branch name
     * @param description Branch description (optional)
     * @return JSON response with created branch
     */
    public String createBranch(String projectId, String name, String description) throws IOException {
        // First, get the project to find the default branch
        String projectJson = getProject(projectId);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode projectNode = mapper.readTree(projectJson);
        
        // Extract default branch ID
        com.fasterxml.jackson.databind.JsonNode defaultBranchNode = projectNode.get("defaultBranch");
        if (defaultBranchNode == null || !defaultBranchNode.has("@id")) {
            throw new IOException("Project does not have a default branch");
        }
        String defaultBranchId = defaultBranchNode.get("@id").asText();
        
        // Get the default branch to find its HEAD commit
        String branchJson = getBranch(projectId, defaultBranchId);
        com.fasterxml.jackson.databind.JsonNode branchNode = mapper.readTree(branchJson);
        
        // Extract HEAD commit ID
        com.fasterxml.jackson.databind.JsonNode headNode = branchNode.get("head");
        if (headNode == null || !headNode.has("@id")) {
            throw new IOException("Default branch does not have a HEAD commit");
        }
        String headCommitId = headNode.get("@id").asText();
        
        String url = baseUrl + "/projects/" + projectId + "/branches";
        
        // Build JSON body with required head field
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"@type\":\"Branch\"");
        jsonBody.append(",\"name\":\"").append(escapeJson(name)).append("\"");
        if (description != null && !description.isEmpty()) {
            jsonBody.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        // Add required head field pointing to the HEAD commit of the default branch
        jsonBody.append(",\"head\":{\"@id\":\"").append(escapeJson(headCommitId)).append("\"}");
        jsonBody.append("}");
        
        HttpPost request = new HttpPost(url);
        addAuthHeader(request);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setEntity(new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON));
        
        return executeRequest(request);
    }
    
    /**
     * Delete a branch
     * 
     * @param projectId Project UUID
     * @param branchId Branch UUID
     */
    public void deleteBranch(String projectId, String branchId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/branches/" + branchId;
        HttpDelete request = new HttpDelete(url);
        addAuthHeader(request);
        
        executeRequest(request);
    }

    // ============ Commit Operations ============

    /**
     * Get commits in a project with pagination
     * 
     * Note: The API does not support filtering by branch name.
     * Commits are tied to the project structure.
     * 
     * @param projectId Project UUID
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with commits
     */
    public String getCommits(String projectId, String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits";
        List<String> params = new ArrayList<>();
        
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get commits (convenience method for backward compatibility)
     * Note: The 'branch' parameter is ignored as the API doesn't support it
     */
    public String getCommits(String projectId, String branch) throws IOException {
        return getCommits(projectId, null, null, null);
    }

    // ============ Query Operations ============

    /**
     * Get queries in a project with pagination
     * 
     * @param projectId Project UUID
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with queries
     */
    public String getQueries(String projectId, String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/queries";
        List<String> params = new ArrayList<>();
        
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get queries (convenience method for backward compatibility)
     */
    public String getQueries(String projectId) throws IOException {
        return getQueries(projectId, null, null, null);
    }
    
    /**
     * Get query by ID
     * 
     * @param projectId Project UUID
     * @param queryId Query UUID
     * @return JSON response with query details
     */
    public String getQuery(String projectId, String queryId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/queries/" + queryId;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");
        
        return executeRequest(request);
    }
    
    /**
     * Execute a query and get results at a specific commit
     * 
     * @param projectId Project UUID
     * @param queryId Query UUID
     * @param commitId Commit UUID (optional, defaults to HEAD of default branch)
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with query results (array of Data elements)
     */
    public String getQueryResults(String projectId, String queryId, String commitId,
                                  String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/queries/" + queryId + "/results";
        List<String> params = new ArrayList<>();
        
        if (commitId != null && !commitId.isEmpty()) {
            params.add("commitId=" + commitId);
        }
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");
        
        return executeRequest(request);
    }
    
    /**
     * Execute a query and get results (convenience method, uses HEAD of default branch)
     */
    public String getQueryResults(String projectId, String queryId) throws IOException {
        return getQueryResults(projectId, queryId, null, null, null, null);
    }

    // ============ Tag Operations ============

    /**
     * Get tags in a project with pagination
     * 
     * @param projectId Project UUID
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with tags
     */
    public String getTags(String projectId, String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/tags";
        List<String> params = new ArrayList<>();
        
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get tags (convenience method for backward compatibility)
     */
    public String getTags(String projectId) throws IOException {
        return getTags(projectId, null, null, null);
    }

    // ============ Relationship Operations ============

    /**
     * Get relationships for an element with filtering and pagination
     * 
     * @param projectId Project UUID
     * @param commitId Commit UUID
     * @param relatedElementId Element UUID
     * @param direction Filter by relationship direction: "in", "out", or "both" (default: "both")
     * @param excludeUsed Exclude elements from ProjectUsages (optional)
     * @param pageAfter Cursor for next page (optional)
     * @param pageBefore Cursor for previous page (optional)
     * @param pageSize Number of items per page (optional)
     * @return JSON response with relationships
     */
    public String getRelationships(String projectId, String commitId, String relatedElementId,
                                   String direction, Boolean excludeUsed,
                                   String pageAfter, String pageBefore, Integer pageSize) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId 
                   + "/elements/" + relatedElementId + "/relationships";
        List<String> params = new ArrayList<>();
        
        if (direction != null && !direction.isEmpty()) {
            // Validate direction parameter
            if (!direction.equals("in") && !direction.equals("out") && !direction.equals("both")) {
                throw new IllegalArgumentException("direction must be 'in', 'out', or 'both'");
            }
            params.add("direction=" + direction);
        }
        if (excludeUsed != null) {
            params.add("excludeUsed=" + excludeUsed);
        }
        if (pageAfter != null && !pageAfter.isEmpty()) {
            params.add("page[after]=" + URLEncoder.encode(pageAfter, StandardCharsets.UTF_8));
        }
        if (pageBefore != null && !pageBefore.isEmpty()) {
            params.add("page[before]=" + URLEncoder.encode(pageBefore, StandardCharsets.UTF_8));
        }
        if (pageSize != null && pageSize > 0) {
            params.add("page[size]=" + pageSize);
        }
        
        if (!params.isEmpty()) {
            url += "?" + String.join("&", params);
        }
        
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }
    
    /**
     * Get relationships (convenience method for backward compatibility)
     */
    public String getRelationships(String projectId, String commitId, String elementId) throws IOException {
        return getRelationships(projectId, commitId, elementId, null, null, null, null, null);
    }

    // ============ Helper Methods ============

    /**
     * Build a CommitRequest JSON string for a single element
     * 
     * @param elementJson JSON string of the element (must include @type and @id fields)
     * @param description Optional commit description
     * @return CommitRequest JSON string ready to pass to createCommit()
     */
    public String buildCommitRequest(String elementJson, String description) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"@type\":\"Commit\",");
        json.append("\"change\":[");
        json.append("{");
        json.append("\"@type\":\"DataVersion\",");
        json.append("\"payload\":").append(elementJson);
        json.append("}");
        json.append("]");
        if (description != null && !description.isEmpty()) {
            json.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Build a CommitRequest JSON string for multiple elements
     * 
     * @param elementJsonArray JSON array string of elements (each must include @type and @id fields)
     * @param description Optional commit description
     * @return CommitRequest JSON string ready to pass to createCommit()
     */
    public String buildCommitRequestMultiple(String elementJsonArray, String description) throws IOException {
        // Parse the array and wrap each element in a DataVersionRequest
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"@type\":\"Commit\",");
        json.append("\"change\":[");
        
        // Simple array parsing - assumes elementJsonArray is a valid JSON array
        String arrayContent = elementJsonArray.trim();
        if (arrayContent.startsWith("[")) {
            arrayContent = arrayContent.substring(1);
        }
        if (arrayContent.endsWith("]")) {
            arrayContent = arrayContent.substring(0, arrayContent.length() - 1);
        }
        
        // Split by objects (this is a simple approach, may need improvement for complex cases)
        // For now, assume each element is on a separate line or properly formatted
        if (!arrayContent.trim().isEmpty()) {
            // Just wrap the content - the caller should provide properly formatted element objects
            String[] elements = splitJsonArray(arrayContent);
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"@type\":\"DataVersion\",");
                json.append("\"payload\":").append(elements[i].trim());
                json.append("}");
            }
        }
        
        json.append("]");
        if (description != null && !description.isEmpty()) {
            json.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Simple JSON array splitter (handles basic cases)
     */
    private String[] splitJsonArray(String content) {
        // Very basic splitter - for production, should use proper JSON parser
        // This handles comma-separated JSON objects
        java.util.List<String> elements = new java.util.ArrayList<>();
        int braceDepth = 0;
        int bracketDepth = 0;
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braceDepth++;
            else if (c == '}') braceDepth--;
            else if (c == '[') bracketDepth++;
            else if (c == ']') bracketDepth--;
            else if (c == ',' && braceDepth == 0 && bracketDepth == 0) {
                elements.add(content.substring(start, i));
                start = i + 1;
            }
        }
        if (start < content.length()) {
            elements.add(content.substring(start));
        }
        
        return elements.toArray(new String[0]);
    }

    /**
     * Add authentication header to request using FlexoMmsClient
     */
    private void addAuthHeader(HttpUriRequestBase request) {
        try {
            // Use reflection to access FlexoMmsClient's authentication method
            java.lang.reflect.Method method = FlexoMmsClient.class.getDeclaredMethod("addAuthHeader", HttpUriRequestBase.class);
            method.setAccessible(true);
            method.invoke(flexoClient, request);
        } catch (Exception e) {
            logger.warn("Failed to add auth header: {}", e.getMessage());
        }
    }

    /**
     * Execute HTTP request and return response body
     */
    private String executeRequest(HttpUriRequestBase request) throws IOException {
        try {
            logger.debug("Executing request: {} {}", request.getMethod(), request.getUri());
        } catch (Exception e) {
            logger.debug("Executing request: {}", request.getMethod());
        }

        return httpClient.execute(request, response -> {
            int statusCode = response.getCode();
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            logger.debug("Response status: {}", statusCode);

            if (statusCode >= 200 && statusCode < 300) {
                return body;
            } else {
                throw new IOException("HTTP " + statusCode + ": " + body);
            }
        });
    }

    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Close the HTTP client
     */
    public void close() throws IOException {
        httpClient.close();
    }
}
