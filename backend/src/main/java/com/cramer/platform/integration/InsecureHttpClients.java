package com.cramer.platform.integration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Factory for {@link HttpClient} instances. Includes a <strong>dev-only</strong> trust-all
 * variant for connecting to self-hosted Supabase with self-signed certificates. The trust-all
 * client disables certificate validation and must never be enabled in production
 * (gated by {@code supabase.insecure-tls}, SPEC-18 §4).
 */
public final class InsecureHttpClients {

    private InsecureHttpClients() {
    }

    /** A standard, certificate-validating client. */
    public static HttpClient secure(Duration connectTimeout) {
        return HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    /** DEV ONLY: a client that trusts all TLS certificates. Disables cert validation. */
    public static HttpClient trustAll(Duration connectTimeout) {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            SSLParameters params = sslContext.getDefaultSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .sslParameters(params)
                    .connectTimeout(connectTimeout)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create insecure HTTP client", e);
        }
    }
}
