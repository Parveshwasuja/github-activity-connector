package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.common.FirebaseConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GithubService {
	private final HttpClient httpClient;
    private final ObjectMapper mapper;

    @Autowired
    public GithubService() {
    	this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<JsonNode> fetchIssues(String owner, String repo) throws IOException {
    	String url = String.format("https://api.github.com/repos/%s/%s/issues?state=all&sort=created&direction=desc&per_page=5", owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github+json")
            .build();

        HttpResponse<String> response = null;
		try {
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch issues from GitHub: " + response.body());
        }
        return mapper.readValue(response.body(), mapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
    }
    
    public List<Map<String, Object>> fetchTopNIssues(String owner, String repo, int n) throws Exception {
        List<Map<String, Object>> issues = new ArrayList<>();
        int page = 1;

        while (issues.size() < n) {
            String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/issues?state=all&sort=created&direction=desc&per_page=%d&page=%d",
                owner, repo, FirebaseConstants.perPage, page
            );

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github+json");

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("GitHub API error: " + status);
            }

            InputStream is = conn.getInputStream();
            JsonNode root = mapper.readTree(is);

            if (root.isEmpty()) break; // No more issues

            for (JsonNode node : root) {
                // Skip pull requests
                if (node.has("pull_request")) continue;

                Map<String, Object> issue = new HashMap<>();
                issue.put("id", node.get("id").asText());
                issue.put("title", node.get("title").asText());
                issue.put("created_at", node.get("created_at").asText());
                issue.put("state", node.get("state").asText());
                issue.put("html_url", node.get("html_url").asText());

                issues.add(issue);

                if (issues.size() >= n) break; // Stop if we reached the top N
            }

            page++; // Move to next page
        }

        return issues;
    }

    public List<Map<String, Object>> fetchTopNIssuesWithRetry(String owner, String repo, int n) throws Exception {
        final int MAX_RETRIES = 3;
        final long INITIAL_BACKOFF_MS = 1000;
        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRIES) {
            try {
                return fetchTopNIssues(owner, repo, n);  // The original method
            } catch (IOException e) {
                attempt++;
                System.err.println("Network error on attempt " + attempt + ": " + e.getMessage());
            } catch (RuntimeException e) {
                if (isRetryableApiException(e)) {
                    attempt++;
                    System.err.println("GitHub API transient error on attempt " + attempt + ": " + e.getMessage());
                } else {
                    // Not retryable
                    throw e;
                }
            }

            // Wait before retrying
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException ignored) {}
            backoff *= 2;  // exponential backoff
        }

        throw new RuntimeException("Failed to fetch issues after " + MAX_RETRIES + " retries.");
    }
    
    private boolean isRetryableApiException(RuntimeException e) {
        String msg = e.getMessage();
        return msg.contains("502") || msg.contains("503") || msg.contains("504");
    }


}

