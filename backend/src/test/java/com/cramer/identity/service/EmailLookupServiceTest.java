package com.cramer.identity.service;

import com.cramer.platform.error.UpstreamServiceException;
import com.cramer.platform.integration.supabase.SupabaseAdminClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailLookupServiceTest {

    private static final int PER_PAGE = 50;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    SupabaseAdminClient client;

    private EmailLookupService service() {
        return new EmailLookupService(client);
    }

    private JsonNode page(String... emails) {
        StringBuilder sb = new StringBuilder("{\"users\":[");
        for (int i = 0; i < emails.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"email\":\"").append(emails[i]).append("\"}");
        }
        sb.append("]}");
        try {
            return mapper.readTree(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode fullPageOf(int count) {
        String[] emails = new String[count];
        for (int i = 0; i < count; i++) {
            emails[i] = "user" + i + "@x.com";
        }
        return page(emails);
    }

    @Test
    @DisplayName("blank email is false and never calls upstream")
    void blankEmailIsFalse() {
        assertThat(service().emailExists("  ")).isFalse();
    }

    @Test
    @DisplayName("found on first page, matched case-insensitively")
    void foundFirstPage() {
        when(client.listUsersPage(1, PER_PAGE)).thenReturn(page("a@x.com", "Jacob@Cramer.io"));
        assertThat(service().emailExists("jacob@cramer.io")).isTrue();
    }

    @Test
    @DisplayName("not found on a short (last) page is false")
    void notFoundShortPage() {
        when(client.listUsersPage(1, PER_PAGE)).thenReturn(page("a@x.com"));
        assertThat(service().emailExists("zz@none.com")).isFalse();
    }

    @Test
    @DisplayName("paginates past a full page until the email is found")
    void paginatesUntilFound() {
        when(client.listUsersPage(1, PER_PAGE)).thenReturn(fullPageOf(PER_PAGE));
        when(client.listUsersPage(2, PER_PAGE)).thenReturn(page("target@x.com"));
        assertThat(service().emailExists("target@x.com")).isTrue();
    }

    @Test
    @DisplayName("upstream failure propagates as 503 — never fabricates exists:false (SPEC-10 §2.1 fix)")
    void upstreamFailurePropagates() {
        when(client.listUsersPage(1, PER_PAGE)).thenThrow(new UpstreamServiceException("Supabase down"));
        assertThatThrownBy(() -> service().emailExists("a@x.com"))
                .isInstanceOf(UpstreamServiceException.class);
    }
}
