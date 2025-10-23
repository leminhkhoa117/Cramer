package com.cramer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class SupabaseClient {

    private final String baseUrl;
    private final String anonKey;
    private final HttpClient httpClient;

    public SupabaseClient(@Value("${supabase.url:}") String supabaseUrl,
                          @Value("${supabase.anon-key:}") String anonKey) {
        this.baseUrl = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
        this.anonKey = anonKey == null ? "" : anonKey.trim();
        // Allow opt-in insecure TLS (trust-all) for debugging only via SUPABASE_INSECURE_TLS=true
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
            // disable endpoint identification (hostname verification) for debug
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
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer " + anonKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Supabase GET failed: " + e.getMessage(), e);
        }
    }

    public String insertIntoTable(String table, String json) {
        try {
            String path = baseUrl + "/" + table;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(path))
                    .timeout(Duration.ofSeconds(10))
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer " + anonKey)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Supabase POST failed: " + e.getMessage(), e);
        }
    }
}

