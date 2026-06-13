package com.cramer.catalog.service;

import com.cramer.catalog.domain.SectionStatus;
import com.cramer.catalog.repository.SectionRepository;
import com.cramer.catalog.repository.TestHashtagRepository;
import com.cramer.catalog.repository.TestRepository;
import com.cramer.catalog.repository.TestSetRepository;
import com.cramer.catalog.web.dto.CreateTestRequest;
import com.cramer.catalog.web.dto.TestView;
import com.cramer.platform.error.OperationNotAllowedException;
import com.cramer.platform.error.ResourceAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestAdminServiceTest {

    @Mock TestRepository tests;
    @Mock SectionRepository sections;
    @Mock TestHashtagRepository testHashtags;
    @Mock HashtagService hashtagService;
    @Mock TestSetRepository testSets;
    @Mock ObjectProvider<TestDependencyGuard> guardProvider;
    @Mock TestDependencyGuard guard;

    private TestAdminService service() {
        return new TestAdminService(tests, sections, testHashtags, hashtagService, testSets, guardProvider);
    }

    private com.cramer.catalog.domain.Test test(long id, long setId, int number) {
        com.cramer.catalog.domain.Test t = new com.cramer.catalog.domain.Test();
        t.setId(id);
        t.setSetId(setId);
        t.setTestNumber(number);
        return t;
    }

    @Test
    @DisplayName("create assigns test_number = max+1 when omitted")
    void createAutoNumber() {
        when(testSets.existsById(1L)).thenReturn(true);
        when(tests.maxTestNumber(1L)).thenReturn(3);
        when(tests.findBySetIdAndTestNumber(1L, 4)).thenReturn(Optional.empty());
        when(tests.save(any(com.cramer.catalog.domain.Test.class))).thenAnswer(inv -> {
            com.cramer.catalog.domain.Test t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        TestView v = service().create(1L, new CreateTestRequest(null, "T", null, null, null, null));

        assertThat(v.testNumber()).isEqualTo(4);
        assertThat(v.isPublished()).isFalse();
    }

    @Test
    @DisplayName("create rejects a duplicate test_number with 409")
    void createDuplicateNumber() {
        when(testSets.existsById(1L)).thenReturn(true);
        when(tests.findBySetIdAndTestNumber(1L, 2)).thenReturn(Optional.of(test(5L, 1L, 2)));

        assertThatThrownBy(() -> service().create(1L, new CreateTestRequest(2, "T", null, null, null, null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    @DisplayName("publish cascades section status to PUBLISHED via the FK path")
    void publishCascade() {
        when(tests.findById(10L)).thenReturn(Optional.of(test(10L, 1L, 1)));
        when(tests.save(any(com.cramer.catalog.domain.Test.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testHashtags.findByIdTestId(10L)).thenReturn(List.of());

        TestView v = service().setPublished(10L, true);

        assertThat(v.isPublished()).isTrue();
        verify(sections).updateStatusByTestId(10L, SectionStatus.PUBLISHED);
    }

    @Test
    @DisplayName("delete is blocked with 403 when the test has user data and force=false")
    void deleteBlockedByGuard() {
        when(tests.findById(10L)).thenReturn(Optional.of(test(10L, 1L, 1)));
        when(guardProvider.getIfAvailable()).thenReturn(guard);
        when(guard.hasUserData(10L)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(10L, false))
                .isInstanceOf(OperationNotAllowedException.class);
        verify(tests, never()).delete(any());
    }

    @Test
    @DisplayName("delete with force=true proceeds even when the guard reports user data")
    void deleteForced() {
        when(tests.findById(10L)).thenReturn(Optional.of(test(10L, 1L, 1)));
        when(sections.findByTestId(10L)).thenReturn(List.of());
        lenient().when(guardProvider.getIfAvailable()).thenReturn(guard);
        lenient().when(guard.hasUserData(anyLong())).thenReturn(true);

        service().delete(10L, true);

        verify(testHashtags).deleteByIdTestId(10L);
        verify(tests).delete(any(com.cramer.catalog.domain.Test.class));
    }
}
