package com.example.demo.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Commit;
import com.example.demo.model.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class GithubService {
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    @Autowired
    public GithubService() {
        this.client = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<Repository> fetchRepositories(String userOrOrg, String token) throws IOException {
        List<Repository> repositories = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = String.format("https://api.github.com/users/%s/repos?per_page=100&page=%d", userOrOrg, page);
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + token)
                .build();

            try (Response response = retryRequest(request, 3)) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch repositories: " + response);
                }

                List<Map<String, Object>> repoData = mapper.readValue(response.body().string(), List.class);
                if (repoData.isEmpty()) break;

                for (Map<String, Object> repoJson : repoData) {
                    String repoName = (String) repoJson.get("name");
                    String fullName = (String) repoJson.get("full_name");

                    List<Commit> commits = fetchCommits(fullName, token);
                    Repository repo = new Repository();
                    repo.setName(repoName);
                    repo.setFullName(fullName);
                    repo.setUrl((String) repoJson.get("html_url"));
                    repo.setCommits(commits);

                    repositories.add(repo);
                }
                page++;
            }
        }

        return repositories;
    }

    private List<Commit> fetchCommits(String fullRepoName, String token) throws IOException {
        List<Commit> commits = new ArrayList<>();

        String url = String.format("https://api.github.com/repos/%s/commits?per_page=20", fullRepoName);
        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "token " + token)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return Collections.emptyList(); // skip repo on error
            }

            List<Map<String, Object>> commitData = mapper.readValue(response.body().string(), List.class);
            for (Map<String, Object> commitJson : commitData) {
                Map<String, Object> commitInfo = (Map<String, Object>) commitJson.get("commit");
                Map<String, Object> authorInfo = (Map<String, Object>) commitInfo.get("author");

                Commit commit = new Commit();
                commit.setMessage((String) commitInfo.get("message"));
                commit.setAuthor((String) authorInfo.get("name"));
                commit.setTimestamp((String) authorInfo.get("date"));
                commits.add(commit);
            }
        }

        return commits;
    }
    
    private Response makeRequestWithRateLimitHandling(Request request) throws IOException {
        Response response = client.newCall(request).execute();

        if (response.code() == 403 && response.header("X-RateLimit-Remaining", "1").equals("0")) {
            long resetTime = Long.parseLong(response.header("X-RateLimit-Reset", "0"));
            long waitTimeSeconds = resetTime - Instant.now().getEpochSecond();
            response.close();

            throw new IOException("GitHub rate limit exceeded. Try again in " + waitTimeSeconds + " seconds.");
        }

        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "No body";
            response.close();

            throw new IOException("Request failed with status " + response.code() + ": " + errorBody);
        }

        return response;
    }
    
    private Response retryRequest(Request request, int maxRetries) throws IOException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return makeRequestWithRateLimitHandling(request);
            } catch (IOException e) {
                attempt++;
                if (attempt == maxRetries) {
                    throw new IOException("Request failed after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(1000L * attempt); // Exponential backoff
                } catch (InterruptedException ignored) {}
            }
        }
        throw new IOException("Unexpected error in retryRequest");
    }
}

