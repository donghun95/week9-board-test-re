package com.dsa.week5board.board.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dsa.week5board.board.dto.BoardCreateRequest;
import com.dsa.week5board.board.dto.BoardResponse;
import com.dsa.week5board.board.exception.BoardNotFoundException;
import com.dsa.week5board.board.service.BoardCacheService;
import com.dsa.week5board.board.service.BoardJpaService;
import com.dsa.week5board.board.service.BoardViewCountService;

@WebMvcTest(BoardController.class)
@DisplayName("BoardController 슬라이스 테스트")
class BoardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardJpaService boardJpaService;

    @MockitoBean
    private BoardCacheService boardCacheService;

    @MockitoBean
    private BoardViewCountService boardViewCountService;

    @Test
    @DisplayName("POST /api/boards - 201 Created + Location 헤더")
    void create_returns201WithLocation() throws Exception {
        BoardResponse response = BoardResponse.builder()
                .id(99L)
                .title("title")
                .content("content")
                .writer("donghun")
                .views(0)
                .build();
        given(boardJpaService.create(any(BoardCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "title",
                                  "content": "content",
                                  "writer": "donghun"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/boards/99"))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.title").value("title"));
    }

    @Test
    @DisplayName("POST /api/boards - title 누락 시 400 + fieldErrors")
    void create_returns400_whenTitleMissing() throws Exception {
        String invalid = """
                { "content": "c", "writer": "w" }
                """;

        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E_400_001"))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("title")));
    }

    @Test
    @DisplayName("GET /api/boards/{id} - 없으면 404")
    void get_returns404_whenAbsent() throws Exception {
        given(boardJpaService.findById(9999L))
                .willThrow(new BoardNotFoundException(9999L));

        mockMvc.perform(get("/api/boards/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E_404_001"))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다: 9999"));
    }

    @Test
    @DisplayName("PATCH /api/boards/{id}/title - 정상 수정 시 200")
    void renameTitle_returns200() throws Exception {
        BoardResponse response = BoardResponse.builder()
                .id(1L)
                .title("new title")
                .content("content")
                .writer("donghun")
                .views(0)
                .build();
        given(boardJpaService.renameTitle(1L, "new title")).willReturn(response);

        mockMvc.perform(patch("/api/boards/1/title")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "new title" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("new title"));
    }

}
