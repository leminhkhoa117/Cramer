package com.cramer.service.unit;

import com.cramer.dto.ProfileDTO;
import com.cramer.entity.Profile;
import com.cramer.repository.ProfileRepository;
import com.cramer.service.implement.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileServiceImpl.
 * Tests user profile CRUD operations.
 * 
 * @author Cramer Test Team
 * @since 2026-01-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl Unit Tests")
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private UUID testUserId;
    private Profile testProfile;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testProfile = new Profile();
        testProfile.setId(testUserId);
        testProfile.setUsername("testuser");
        testProfile.setFullName("Test User");
        testProfile.setPhoneNumber("0901234567");
        testProfile.setAddress("123 Test Street");
        testProfile.setAvatarUrl("https://example.com/avatar.jpg");
        testProfile.setIsAdmin(false);
        testProfile.setCreatedAt(OffsetDateTime.now().minusDays(30));
    }

    // =========================================================================
    // GET PROFILE BY ID TESTS
    // =========================================================================
    @Nested
    @DisplayName("getProfileById() Tests")
    class GetProfileByIdTests {

        @Test
        @DisplayName("Should return profile when found")
        void getProfileById_exists_returnsProfile() {
            // Arrange
            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));

            // Act
            ProfileDTO result = profileService.getProfileById(testUserId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testUserId);
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getFullName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void getProfileById_notFound_throwsException() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(profileRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> profileService.getProfileById(unknownId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Profile not found");
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void getProfileById_nullId_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> profileService.getProfileById(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should not return actual API key in DTO")
        void getProfileById_withApiKey_hidesKey() {
            // Arrange
            testProfile.setLlmApiKey("sk-secret-api-key-12345");
            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));

            // Act
            ProfileDTO result = profileService.getProfileById(testUserId);

            // Assert - Should indicate key exists but not return actual value
            assertThat(result.isHasLlmApiKey()).isTrue();
            assertThat(result.getLlmApiKey()).isNull(); // Actual key not exposed
        }

        @Test
        @DisplayName("Should indicate no API key when not set")
        void getProfileById_noApiKey_indicatesNone() {
            // Arrange
            testProfile.setLlmApiKey(null);
            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));

            // Act
            ProfileDTO result = profileService.getProfileById(testUserId);

            // Assert
            assertThat(result.isHasLlmApiKey()).isFalse();
        }
    }

    // =========================================================================
    // UPDATE PROFILE TESTS
    // =========================================================================
    @Nested
    @DisplayName("updateProfile() Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update basic profile fields")
        void updateProfile_basicFields_updatesSuccessfully() {
            // Arrange
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setFullName("Updated Name");
            updateDto.setPhoneNumber("0909999999");
            updateDto.setAddress("New Address 456");

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProfileDTO result = profileService.updateProfile(testUserId, updateDto);

            // Assert
            assertThat(result.getFullName()).isEqualTo("Updated Name");
            assertThat(result.getPhoneNumber()).isEqualTo("0909999999");
            assertThat(result.getAddress()).isEqualTo("New Address 456");

            verify(profileRepository).save(argThat(profile ->
                    "Updated Name".equals(profile.getFullName())));
        }

        @Test
        @DisplayName("Should update avatar URL")
        void updateProfile_avatarUrl_updatesSuccessfully() {
            // Arrange
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setAvatarUrl("https://new-avatar.com/image.png");

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProfileDTO result = profileService.updateProfile(testUserId, updateDto);

            // Assert
            assertThat(result.getAvatarUrl()).isEqualTo("https://new-avatar.com/image.png");
        }

        @Test
        @DisplayName("Should set LLM API key")
        void updateProfile_setLlmApiKey_savesKey() {
            // Arrange
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setLlmApiKey("new-deepseek-api-key");

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            profileService.updateProfile(testUserId, updateDto);

            // Assert
            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getLlmApiKey()).isEqualTo("new-deepseek-api-key");
        }

        @Test
        @DisplayName("Should clear LLM API key when empty string provided")
        void updateProfile_clearLlmApiKey_clearsKey() {
            // Arrange
            testProfile.setLlmApiKey("existing-api-key");
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setLlmApiKey(""); // Empty string to clear

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            profileService.updateProfile(testUserId, updateDto);

            // Assert
            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getLlmApiKey()).isNull();
        }

        @Test
        @DisplayName("Should update LLM model")
        void updateProfile_llmModel_updatesModel() {
            // Arrange
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setLlmModel("deepseek-reasoner");

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProfileDTO result = profileService.updateProfile(testUserId, updateDto);

            // Assert
            assertThat(result.getLlmModel()).isEqualTo("deepseek-reasoner");
        }

        @Test
        @DisplayName("Should update LLM provider")
        void updateProfile_llmProvider_updatesProvider() {
            // Arrange
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setLlmProvider("openrouter");

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProfileDTO result = profileService.updateProfile(testUserId, updateDto);

            // Assert
            assertThat(result.getLlmProvider()).isEqualTo("openrouter");
        }

        @Test
        @DisplayName("Should throw exception when profile not found")
        void updateProfile_notFound_throwsException() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            ProfileDTO updateDto = new ProfileDTO();

            when(profileRepository.findById(unknownId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> profileService.updateProfile(unknownId, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Profile not found");
        }

        @Test
        @DisplayName("Should not update null fields in DTO")
        void updateProfile_nullFields_preservesExisting() {
            // Arrange - Original has avatar URL
            testProfile.setAvatarUrl("https://original-avatar.com");
            
            ProfileDTO updateDto = new ProfileDTO();
            updateDto.setFullName("New Name");
            updateDto.setAvatarUrl(null); // Don't update

            when(profileRepository.findById(testUserId))
                    .thenReturn(Optional.of(testProfile));
            when(profileRepository.save(any(Profile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ProfileDTO result = profileService.updateProfile(testUserId, updateDto);

            // Assert - Avatar should be preserved
            assertThat(result.getAvatarUrl()).isEqualTo("https://original-avatar.com");
        }
    }
}
