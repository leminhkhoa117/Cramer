package com.cramer.catalog.service;

import com.cramer.catalog.domain.Hashtag;
import com.cramer.catalog.repository.HashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    HashtagRepository hashtags;

    private HashtagService service() {
        return new HashtagService(hashtags);
    }

    private Hashtag tag(long id, String code) {
        Hashtag h = new Hashtag();
        h.setId(id);
        h.setCode(code);
        h.setCategory("topic");
        return h;
    }

    @Test
    @DisplayName("findOrCreateByCodes returns existing and creates missing as 'topic', preserving order")
    void findOrCreate() {
        when(hashtags.findByCodeIn(anyList())).thenReturn(List.of(tag(1L, "science")));
        when(hashtags.save(any(Hashtag.class))).thenAnswer(inv -> {
            Hashtag h = inv.getArgument(0);
            h.setId(2L);
            return h;
        });

        List<Hashtag> result = service().findOrCreateByCodes(List.of("science", "technology", "science"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("science");
        assertThat(result.get(1).getCode()).isEqualTo("technology");
        assertThat(result.get(1).getCategory()).isEqualTo("topic");
        assertThat(result.get(1).getName()).isEqualTo("Technology");
    }

    @Test
    @DisplayName("findOrCreateByCodes rejects more than 20 distinct codes")
    void max20() {
        List<String> codes = IntStream.range(0, 21).mapToObj(i -> "t" + i).toList();
        assertThatThrownBy(() -> service().findOrCreateByCodes(codes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("empty/null codes resolve to an empty list without touching the repo")
    void emptyCodes() {
        assertThat(service().findOrCreateByCodes(List.of())).isEmpty();
        assertThat(service().findOrCreateByCodes(null)).isEmpty();
    }
}
