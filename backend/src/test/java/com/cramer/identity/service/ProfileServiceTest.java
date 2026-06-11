package com.cramer.identity.service;

import com.cramer.identity.domain.AccountStatus;
import com.cramer.identity.domain.Profile;
import com.cramer.identity.repository.ProfileRepository;
import com.cramer.identity.web.dto.ProfileResponse;
import com.cramer.identity.web.dto.UpdateProfileRequest;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    ProfileRepository profiles;

    @InjectMocks
    ProfileService service;

    private static UpdateProfileRequest withLlmKey(String key) {
        return new UpdateProfileRequest(null, null, null, null, null, null, key, null, null);
    }

    private Profile sample(UUID id) {
        Profile p = new Profile();
        p.setId(id);
        p.setUsername("jacob");
        p.setLlmApiKey("secret-key");
        p.setAccountStatus(AccountStatus.ACTIVE);
        return p;
    }

    @Test
    @DisplayName("read own profile returns a view and never exposes the raw LLM key")
    void getOwnProfile() {
        UUID id = UUID.randomUUID();
        when(profiles.findById(id)).thenReturn(Optional.of(sample(id)));

        ProfileResponse res = service.getProfile(id, id);

        assertThat(res.id()).isEqualTo(id);
        assertThat(res.hasLlmApiKey()).isTrue();
        // ProfileResponse exposes no raw-key field — a compile-time guarantee against leakage.
    }

    @Test
    @DisplayName("reading another user's profile is forbidden (IDOR guard) before any DB hit")
    void getOtherProfileForbidden() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertThatThrownBy(() -> service.getProfile(me, other))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("missing profile maps to 404 (not 500)")
    void getMissingProfileIs404() {
        UUID id = UUID.randomUUID();
        when(profiles.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(id, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("llmApiKey: empty clears, null leaves unchanged, non-empty stores (SPEC-10 §2.3)")
    void llmApiKeyUpdateSemantics() {
        UUID id = UUID.randomUUID();
        Profile p = sample(id);
        when(profiles.findById(id)).thenReturn(Optional.of(p));
        when(profiles.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateProfile(id, id, withLlmKey(""));
        assertThat(p.getLlmApiKey()).isNull();

        p.setLlmApiKey("kept");
        service.updateProfile(id, id, withLlmKey(null));
        assertThat(p.getLlmApiKey()).isEqualTo("kept");

        service.updateProfile(id, id, withLlmKey("new-key"));
        assertThat(p.getLlmApiKey()).isEqualTo("new-key");
    }

    @Test
    @DisplayName("updating another user's profile is forbidden (IDOR guard)")
    void updateOtherProfileForbidden() {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertThatThrownBy(() -> service.updateProfile(me, other, withLlmKey(null)))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(profiles, never()).save(any());
    }
}
