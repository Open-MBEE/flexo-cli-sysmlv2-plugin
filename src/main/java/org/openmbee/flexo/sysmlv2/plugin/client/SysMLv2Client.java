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
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for SysML v2 API operations
 *
 * Wraps the SysML v2 API exposed by flexo-mms-sysmlv2 service.
 * Uses FlexoMmsClient for authentication headers.
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
     * Get all projects
     */
    public String getProjects(int page, int size) throws IOException {
        String url = baseUrl + "/projects?page=" + page + "&size=" + size;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
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

        // Build JSON body
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        jsonBody.append("\"name\":\"").append(escapeJson(name)).append("\"");
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
     * Update a project
     */
    public String updateProject(String projectId, String name, String description) throws IOException {
        String url = baseUrl + "/projects/" + projectId;

        // Build JSON body
        StringBuilder jsonBody = new StringBuilder();
        jsonBody.append("{");
        boolean hasField = false;
        if (name != null && !name.isEmpty()) {
            jsonBody.append("\"name\":\"").append(escapeJson(name)).append("\"");
            hasField = true;
        }
        if (description != null && !description.isEmpty()) {
            if (hasField) jsonBody.append(",");
            jsonBody.append("\"description\":\"").append(escapeJson(description)).append("\"");
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
     * Get elements in a project/commit
     */
    public String getElements(String projectId, String commitId, int page, int size) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements?page=" + page + "&size=" + size;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    /**
     * Get element by ID
     */
    public String getElement(String projectId, String commitId, String elementId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements/" + elementId;
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    /**
     * Get root elements in a project
     */
    public String getRootElements(String projectId, String commitId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements/roots";
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Branch Operations ============

    /**
     * Get branches in a project
     */
    public String getBranches(String projectId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/branches";
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Commit Operations ============

    /**
     * Get commits in a project
     */
    public String getCommits(String projectId, String branch) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits";
        if (branch != null && !branch.isEmpty()) {
            url += "?branch=" + branch;
        }
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Query Operations ============

    /**
     * Get queries in a project
     */
    public String getQueries(String projectId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/queries";
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Tag Operations ============

    /**
     * Get tags in a project
     */
    public String getTags(String projectId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/tags";
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Relationship Operations ============

    /**
     * Get relationships for an element
     */
    public String getRelationships(String projectId, String commitId, String elementId) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/commits/" + commitId + "/elements/" + elementId + "/relationships";
        HttpGet request = new HttpGet(url);
        addAuthHeader(request);
        request.setHeader("Accept", "application/json");

        return executeRequest(request);
    }

    // ============ Helper Methods ============

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
