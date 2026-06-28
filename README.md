## 9주차 정리

- 추가한 클래스 정리
1. BoardRepositoryTest 
검증 대상 : 데이터베이스 영속성 계층 BoardRepository
특징 : @DataJpaTest 를 사용하여 JPA 관련 설정과 기본 가상 DB(H2)만 메모리에 올려 테스트를 진행했습니다.
JPA Query Method나 Fetch join쿼리가 실제 데이터베이스 위에서 SQL로 변환되어 의도
한 레코드를 가져오는지를 검증했습니다.

2. BoardJpaServiceTest 
검증 대상 : 핵심 비즈니스 로직 및 예외 제어 계층 BoardJpaService
특징 : 스프링 컨텍스트를 전혀 띄우지 않고, 순수한 Mockito 기법을 활용하여 1초 만에 실행되는 순수 단위 테스트를 구현했습니다.

3. BoardControllerWebMvcTest
검증 대상 : 클라이언트 요청 처리 및 API 규격 계층 BoardController
특징 : @WebMvcTest(BoardController.class)를 활용하여 Controller, Spring MVC 매핑, JSON 변환, Validation, ExceptionHandler를 얇게 테스트한다

- Jacoco
<img width="901" height="301" alt="스크린샷 2026-06-26 041900" src="https://github.com/user-attachments/assets/5a55f485-7392-4b4a-ad20-d7e915aa7d49" />

JaCoCo 보고서

1. delete(Long id) 메서드 누락

안 덮힌이유 :
BoardControllerWebMvcTest에서 게시글 삭제 API를 검증하는 테스트 케이스를 누락하여 해당 라인이 전혀 실행되지 않음.

어떻게 덮을지 :
mockMvc.perform(delete("/api/boards/1")) 검증 코드를 추가하여 204 No Content 응답이 오는지 확인하는 테스트 케이스를 추가하겠다.

2. list 메서드의 if 조건문 분기 누락

안 덮힌이유 : fetch=join에 따른 동적 분기 테스트 시나리오가 없습니다.

어떻게 덮을지:
파리미터 없이 요청했을 때와 ?fetch=join을 넘겼을때 가가 다른 서비스 메서드가 호출되는지 검증하는 멀티 시나리오 테스트를 구축 하겠습니다.

3. BoardJpaService.renameTitle 메서드 영역  
renameTitle 성공 케이스는 추가했고, 없는 게시글일 때의 예외(실패) 케이스를 추가하면 더 좋다

- 커버리지가 높은 것 VS 테스트 품질이 좋은 것 차이
커버리지가 높다는 것은 테스트 코드가 소스 코드를 얼마나 많이 실행해 봤는가에 대한 양적인 지표일 뿐입니다.
반면, 테스트 품질이 좋다는 것은 단순한 라인 실행을 넘어 다양한 예외상황에서 의도한대로 에러가 터지는지 그리고 Mock 객채의 행위 검증이 정확하게 이루어졌는지에 대한 질적 완성도를 의미합니다.
한줄 정리: 퍼센트 성적표만 높은것과 진짜 내 코드는 안전한가의 차이

- 이번 프로젝트에서 가장 중요한 테스트는 무엇인가?
인기 게시글 캐시 무효화 서비스 단위 테스트하고 생각합니다.
BoardJpaServiceTest에서 구현한 "게시글 생성 성공 시 evictPopularTop10() 호출 검증"과 
"생성 실패 혹은 삭제할 데이터가 없을 때 never()를 통한 미호출 검증"은 서비스의 데이터 정합성을 보장하는 안전장치입니다.
단순한 기능 통과를 넘어 이러한 비즈니스 제어 흐름을 Mockito 행위 검증으로 인해 
이 프로젝트에서 무결성을 유지하기 위해 가장 중요한 테스트라고 판단합니다.

- 면접 답변 카드 :
Test Pyramid가 뭔가요?
답변 : 테스트 피라미드는 단위 테스트, 슬라이스/통합테스트의 이상적인 비율을 시각화한 모델입니다.
@MockitoBean을 왜 쓰나요?
답변 : 실제 스프링 컨텍스트(ApplicationContext)에 등록된 특정 빈(Bean)을 가짜 Mock 객체로 대체하여 주입하기 위해 사용합니다.
JaCoCo 커버리지 목표를 묻는다면?
답변 : 무조건적인 100% 달성보다는 핵심 비즈니스 로직이 집중된 서비스 계층의 라인 및 브랜치(조건문) 커버리지 80% 이상을 목표로 잡습니다.
Git feature branch ➔ PR ➔ merge 흐름을 설명해주세요
답변 : 
하나의 기능을 개발할 때 메인 브랜치에서 분기한 feature/기능명 브랜치를 생성해 독립적으로 코드를 작성합니다. 
작업 및 로컬 테스트가 완료되면 원격 저장소에 푸시한 후, 팀원들에게 코드 리뷰를 요청하는 Pull Request(PR)를 생성합니다. 
동료들의 리뷰와 검증을 거쳐 안전성이 확보되면 최종적으로 메인 브랜치에 Merge(병합)하는 협업 흐름입니다.

