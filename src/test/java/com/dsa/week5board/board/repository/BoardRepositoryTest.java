package com.dsa.week5board.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.dsa.week5board.board.domain.Board;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.highlight_sql=false"
})
@DisplayName("BoardRepository 슬라이스 테스트")
class BoardRepositoryTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("findTop10ByOrderByViewsDesc - views 내림차순 10건")
    void findTop10_orderByViewsDesc() {
        for (int i = 1; i <= 15; i++) {
            em.persist(Board.builder()
                    .title("t" + i)
                    .content("c" + i)
                    .writer("w" + i)
                    .views(i)
                    .build());
        }
        em.flush();
        em.clear();

        List<Board> top10 = boardRepository.findTop10ByOrderByViewsDesc();

        assertThat(top10).hasSize(10);
        assertThat(top10).extracting(Board::getViews)
                .containsExactly(15, 14, 13, 12, 11, 10, 9, 8, 7, 6);
    }

    @Test
    @DisplayName("addViews - views가 delta만큼 누적된다")
    void addViews_accumulates() {
        Board board = em.persistAndFlush(Board.builder()
                .title("t")
                .content("c")
                .writer("w")
                .views(3)
                .build());
        em.clear();

        int updated = boardRepository.addViews(board.getId(), 5L);

        em.clear();
        Board reloaded = em.find(Board.class, board.getId());

        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getViews()).isEqualTo(8);
    }
}
