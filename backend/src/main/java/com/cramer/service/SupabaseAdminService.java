package com.cramer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class SupabaseAdminService {

    private final String baseUrl;
    private final String serviceRoleKey;
    private final HttpClient httpClient;

    public SupabaseAdminService(@Value("${supabase.url:}") String supabaseUrl,
                               @Value("${supabase.service-role-key:}") String serviceRoleKey) {
        this.baseUrl = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey.trim();
        String insecure = System.getenv("SUPABASE_INSECURE_TLS");
        if (insecure != null && insecure.equalsIgnoreCase("true")) {
            this.httpClient = createInsecureHttpClient();
        } else {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
    }

    private HttpClient createInsecureHttpClient() {
        try {
            javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            javax.net.ssl.SSLParameters params = sslContext.getDefaultSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(params)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure HTTP client", e);
        }
    }

    public String getTable(String table, String queryString) {
        try {
            String path = baseUrl + "/" + table + (queryString == null ? "" : queryString);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(path))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return resp.body();
            } else {
                System.err.println("Supabase Admin GET request failed with status " + resp.statusCode() + ": " + resp.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            // We should log the exception before re-throwing or wrapping it
            System.err.println("Exception during Supabase Admin GET request: " + e.getMessage());
            // Wrapping in a RuntimeException is one option, but returning null might be safer
            // depending on the desired contract of this method. Let's stick to returning null
            // to indicate failure, which is consistent with the status code check.
            return null;
        }
    }

    public String postTable(String table, String json) {
        try {
            String path = baseUrl + "/" + table;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(path))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", serviceRoleKey)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Supabase Admin POST failed: " + e.getMessage(), e);
        }
    }

    private String callAuthAdminApi(String method, String path, String body) {
        try {
            String fullPath = this.baseUrl + "/auth/v1/" + path;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullPath))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", this.serviceRoleKey)
                    .header("Authorization", "Bearer " + this.serviceRoleKey);

            if ("GET".equalsIgnoreCase(method)) {
                requestBuilder.GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                requestBuilder.header("Content-Type", "application/json");
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            }

            HttpResponse<String> resp = this.httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return resp.body();
            } else {
                System.err.println("Supabase Auth Admin API request failed with status " + resp.statusCode() + ": " + resp.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Exception during Supabase Auth Admin API request: " + e.getMessage());
            return null;
        }
    }

    public boolean checkEmailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // WARNING: This is not a scalable solution!
        // The Supabase Admin API does not support server-side filtering of users by email.
        // This code fetches all users (up to the default page limit of 50) and checks them in memory.
        // This will be slow and will fail for user bases larger than 50.
        // The recommended solution is to create a PostgreSQL RPC function to perform this check in the database.
        String responseBody = callAuthAdminApi("GET", "admin/users", null);

        if (responseBody == null) {
            return false; // Error occurred during the HTTP request
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            // The response is an object like {"users": [...]}
            JsonNode root = mapper.readTree(responseBody);
            JsonNode usersNode = root.path("users");
            
            if (usersNode.isMissingNode() || !usersNode.isArray()) {
                return false;
            }

            for (JsonNode userNode : usersNode) {
                JsonNode emailNode = userNode.path("email");
                if (!emailNode.isMissingNode() && email.equalsIgnoreCase(emailNode.asText())) {
                    return true; // Found a matching user
                }
            }

            // TODO: Handle pagination if total users > page limit.
            // The response includes "total", "page", "per_page" fields which can be used to fetch all pages.

            return false; // No user found on the first page
        } catch (Exception e) {
            System.err.println("Failed to parse Supabase admin users response: " + responseBody);
            return false;
        }
    }
}
