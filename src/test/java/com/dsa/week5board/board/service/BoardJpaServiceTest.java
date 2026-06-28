package com.dsa.week5board.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dsa.week5board.board.domain.Board;
import com.dsa.week5board.board.dto.BoardCreateRequest;
import com.dsa.week5board.board.dto.BoardResponse;
import com.dsa.week5board.board.exception.BoardNotFoundException;
import com.dsa.week5board.board.repository.BoardRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardJpaService 단위 테스트")
class BoardJpaServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardCacheService boardCacheService;

    @InjectMocks
    private BoardJpaService sut;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("존재하면 BoardResponse를 반환한다")
        void returnsResponse_whenFound() {
            Board board = newBoardWithId(1L, "title", "writer");
            given(boardRepository.findById(1L)).willReturn(Optional.of(board));

            BoardResponse response = sut.findById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("title");
            assertThat(response.getWriter()).isEqualTo("writer");
        }

        @Test
        @DisplayName("없으면 BoardNotFoundException을 던진다")
        void throwsNotFound_whenAbsent() {
            given(boardRepository.findById(9999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.findById(9999L))
                    .isInstanceOf(BoardNotFoundException.class)
                    .hasMessageContaining("9999");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("저장 후 인기 캐시를 evict 한다")
        void evictsCacheAfterSave() {
            BoardCreateRequest request = newCreateRequest("t", "c", "donghun");
            given(boardRepository.save(any(Board.class)))
                    .willAnswer(invocation -> {
                        Board board = invocation.getArgument(0);
                        board.setId(99L);
                        return board;
                    });

            BoardResponse response = sut.create(request);

            assertThat(response.getId()).isEqualTo(99L);
            assertThat(response.getTitle()).isEqualTo("t");
            then(boardRepository).should().save(any(Board.class));
            then(boardCacheService).should().evictPopularTop10();
        }

        @Test
        @DisplayName("Repository 예외가 나면 캐시 evict는 호출되지 않는다")
        void doesNotEvict_whenSaveFails() {
            BoardCreateRequest request = newCreateRequest("t", "c", "donghun");
            given(boardRepository.save(any(Board.class)))
                    .willThrow(new RuntimeException("DB down"));

            assertThatThrownBy(() -> sut.create(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB down");

            then(boardCacheService).should(never()).evictPopularTop10();
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("존재하면 삭제 후 인기 캐시를 evict 한다")
        void deletesAndEvicts_whenFound() {
            given(boardRepository.existsById(1L)).willReturn(true);

            sut.deleteById(1L);

            then(boardRepository).should().deleteById(1L);
            then(boardCacheService).should().evictPopularTop10();
        }

        @Test
        @DisplayName("없으면 BoardNotFoundException을 던지고 삭제하지 않는다")
        void throwsNotFound_whenAbsent() {
            given(boardRepository.existsById(9999L)).willReturn(false);

            assertThatThrownBy(() -> sut.deleteById(9999L))
                    .isInstanceOf(BoardNotFoundException.class)
                    .hasMessageContaining("9999");

            then(boardRepository).should(never()).deleteById(any());
            then(boardCacheService).should(never()).evictPopularTop10();
        }
    }

    @Nested
    @DisplayName("renameTitle")
    class RenameTitle {

        @Test
        @DisplayName("제목을 변경하고 인기 캐시를 evict 한다")
        void changesTitleAndEvicts_whenFound() {
            Board board = newBoardWithId(1L, "old", "writer");
            given(boardRepository.findById(1L)).willReturn(Optional.of(board));

            BoardResponse response = sut.renameTitle(1L, "new");

            assertThat(board.getTitle()).isEqualTo("new");
            assertThat(response.getTitle()).isEqualTo("new");
            then(boardCacheService).should().evictPopularTop10();
        }
    }

    private Board newBoardWithId(Long id, String title, String writer) {
        Board board = Board.builder()
                .title(title)
                .content("content")
                .writer(writer)
                .views(0)
                .build();
        board.setId(id);
        return board;
    }

    private BoardCreateRequest newCreateRequest(String title, String content, String writer) {
        BoardCreateRequest request = new BoardCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setWriter(writer);
        return request;
    }
}
