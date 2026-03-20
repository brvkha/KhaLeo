# Feature Specification: Public Deck Discovery, Personal Study Workspace, and Anki-Style Review Flow

**Feature Branch**: `008-public-deck-clone`  
**Created**: 2026-03-19  
**Status**: Draft  
**Input**: User description: "tab study hiện thị những mục mình đang học, tab decks thì hiện những cái công khai thôi, muốn tải chúng về thì bên tab study sẽ copy deck công khai đó rồi tạo thành 1 cái deck private cho mình thôi. Tab card với study là chung hiện thị danh sách những gì mình đang học thôi (những deck private của mình), ở đó có nút crud search với deck của riêng mình trong đó. Khi bắt đầu học thì nó cho tab khác giống flash card có hai mặt, thẻ mới tinh thường again là 1 phút, hard 6 phút, good 10 phút, easy là 1 ngày là quay lại (quay lại ở đây là phương pháp lặp lại ngắt quảng đó, thuật toán đó nên copy của ANKI nó lặp lại rất hay). và quan trong là Kha Leo tôi còn định làm nhiều cái lắm, học flashcard này là 1 phần nhỏ thôi, tương lai sẽ phát triển sang học tiếng Anh. mình cũng làm rõ quy trình trước khi tạo spec thật kĩ nha"

## Clarifications

### Session 2026-03-19

- Q: Khi import public deck thì copy phạm vi dữ liệu nào? -> A: Copy deck metadata + toàn bộ cards + media references; không copy learning progress/history.
- Q: Guest/User được phép gì ở tab Decks public? -> A: Guest được xem deck public; chỉ user đăng nhập mới import/copy vào Study.
- Q: Nếu user import lại cùng source deck thì xử lý sao? -> A: Merge source vào bản private cũ; giữ chỉnh sửa local nếu không xung đột.
- Q: Khi merge re-import có xung đột giữa local và cloud thì ưu tiên sao? -> A: Cho người dùng chọn giữ local hoặc dùng dữ liệu cloud.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Keep Study Workspace Personal (Priority: P1)

As a learner, I can see and manage only my own private study decks in my study/cards workspace so my active learning area is focused and fully under my control.

**Why this priority**: A personal study workspace is the core behavior the learner uses daily; without clear private ownership boundaries, all other study actions are confusing and unsafe.

**Independent Test**: Can be fully tested by creating private decks for multiple users, opening study/cards lists for one user, and confirming only that user's private decks appear with private-deck CRUD and search operations.

**Acceptance Scenarios**:

1. **Given** a learner owns private decks and other users also own private decks, **When** the learner opens study/cards lists, **Then** only the learner's private decks are shown.
2. **Given** a private deck owned by the learner, **When** the learner performs create, update, delete, and search operations, **Then** operations apply only within that learner's private workspace.
3. **Given** a deck not owned by the learner, **When** the learner attempts private-workspace mutations on it, **Then** the action is denied and no content changes occur.

---

### User Story 2 - Discover Public Decks and Import as Private Copy (Priority: P1)

As a learner, I can browse only public decks in the discovery tab and import one into my study workspace as a private copy so I can personalize learning without modifying source content.

**Why this priority**: Public-to-private import is the bridge between discovery and actual studying; this creates immediate learner value while preserving original shared deck integrity.

**Independent Test**: Can be fully tested by listing public decks, importing one, and verifying a new private deck is created for the learner while the original public deck remains unchanged.

**Acceptance Scenarios**:

1. **Given** multiple decks with mixed visibility, **When** a guest or learner opens the discovery tab, **Then** only public decks are listed.
2. **Given** a selected public deck, **When** a guest attempts import/copy, **Then** the request is blocked until authentication is completed.
3. **Given** a selected public deck and an authenticated learner, **When** the learner chooses import/copy, **Then** the system creates a new private deck owned by that learner with copied card content.
4. **Given** a public deck has later edits by its original owner, **When** the learner explicitly re-imports, **Then** the system merges source updates into the existing private copy while preserving local learner edits when no conflicts exist.
5. **Given** a re-import merge has conflicts between local private edits and source cloud data, **When** the conflict step is shown, **Then** the learner can explicitly choose whether to keep local values or apply cloud values for each conflicted unit.
6. **Given** an authenticated but unverified account, **When** the user attempts to import from public discovery, **Then** the action is denied with a verification-required outcome.

---

### User Story 3 - Study in Two-Sided Flashcard Mode With Anki-Style Timing (Priority: P2)

As a learner, I can start a focused study session that shows two-sided cards and rate answers with familiar spaced-repetition options so scheduling behavior feels natural and effective.

**Why this priority**: The study session is where learner outcomes happen; two-sided interaction plus predictable timing drives habit and retention.

**Independent Test**: Can be fully tested by starting study mode from a learner-owned private deck, revealing both sides of cards, rating with `Again/Hard/Good/Easy`, and confirming next-due timestamps and queue behavior.

**Acceptance Scenarios**:

1. **Given** a learner starts session mode for a private deck, **When** cards are presented, **Then** each card is shown as a two-sided flashcard flow (prompt side then answer side).
2. **Given** a brand-new card, **When** the learner rates it `Again`, **Then** the next review is scheduled for 1 minute.
3. **Given** a brand-new card, **When** the learner rates it `Hard`, **Then** the next review is scheduled for 6 minutes.
4. **Given** a brand-new card, **When** the learner rates it `Good`, **Then** the next review is scheduled for 10 minutes.
5. **Given** a brand-new card, **When** the learner rates it `Easy`, **Then** the next review is scheduled for 1 day.
6. **Given** cards that are no longer brand-new, **When** the learner rates them, **Then** scheduling follows the established Anki-style spaced-repetition behavior while preserving minimum ease and state-transition integrity.

---

### Edge Cases

- A learner tries to import the same public deck repeatedly in a short period.
- A public deck is deleted or made private during an in-progress import request.
- Import source contains cards with unsupported or malformed fields.
- A learner starts study mode on a private deck with zero eligible cards.
- A learner attempts to perform private-deck CRUD/search against a deck that was just transferred, deleted, or access-revoked.
- Two concurrent study submissions arrive for the same learner/card pair.
- Timezone boundaries cause due calculations to cross day cutoffs during active sessions.
- A learner account has very large numbers of private decks and cards, requiring stable pagination/filter behavior.
- A re-import merge encounters conflicts between updated source content and local private edits.

### Constitutional Impact *(mandatory)*

- **Algorithm Fidelity**: High impact. The feature codifies required review timing for brand-new cards (`Again=1 minute`, `Hard=6 minutes`, `Good=10 minutes`, `Easy=1 day`) and requires established Anki-style scheduling behavior for subsequent reviews while preserving existing state transitions and daily-limit integrity.
- **Security Impact**: High impact. Discovery must expose only public decks; private study/cards workspace and all deck/card mutations must be restricted to owner-authorized content.
- **Observability Impact**: High impact. System must emit outcomes for public discovery, import-copy execution, private CRUD/search actions, study-session starts, rating actions, schedule results, and authorization denials.
- **Infrastructure Impact**: Low to moderate impact. Requires no mandatory topology change, but may require index/query tuning and capacity adjustments for public discovery and private workspace listings at scale.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a discovery view that lists only public decks.
- **FR-016**: Public discovery read access MUST allow both guests and authenticated users, and MUST NOT expose private decks.
- **FR-017**: Import/copy action from public discovery MUST require an authenticated user session.
- **FR-020**: Import/copy and study-session actions MUST require an authenticated and email-verified account.
- **FR-002**: System MUST provide a personal study/cards workspace that lists only private decks owned by the acting learner.
- **FR-003**: System MUST allow learners to create, update, delete, and search only within their own private decks.
- **FR-004**: System MUST deny private-workspace CRUD/search mutations for decks not owned by the acting learner.
- **FR-005**: System MUST allow a learner to import a public deck by creating a new private copy owned by that learner.
- **FR-006**: System MUST preserve source public deck integrity during import (import operation cannot modify source deck/cards).
- **FR-007**: System MUST ensure imported private copies are independently editable by the importing learner.
- **FR-015**: Public-deck import MUST copy deck metadata, cards, and media references, and MUST NOT copy learning progress/history from source owner or other learners.
- **FR-018**: When a learner re-imports the same source public deck, system MUST merge source updates into the existing private copy and MUST preserve local learner edits where no conflict exists.
- **FR-019**: When merge conflicts occur during re-import, system MUST provide a user decision path to choose local values or cloud values for conflicted fields/items before finalizing merge.
- **FR-008**: System MUST provide a study-session mode that presents two-sided flashcard interaction (question side and answer side).
- **FR-009**: For brand-new cards, system MUST schedule next due time as: `Again=1 minute`, `Hard=6 minutes`, `Good=10 minutes`, `Easy=1 day`.
- **FR-010**: For non-brand-new cards, system MUST apply established Anki-style spaced-repetition behavior while preserving minimum ease-factor and valid card-state transitions.
- **FR-011**: Every list-producing capability in discovery and personal workspace MUST define pagination behavior, default page size, and maximum page size.
- **FR-012**: System MUST define required observability outputs for discovery, import, private CRUD/search, study start, rating actions, scheduling outcomes, and access denials.
- **FR-021**: Observability outputs for this feature MUST include structured JSON logs for import/re-import/conflict flows, New Relic service instrumentation for affected endpoints, and CloudWatch alarm coverage for backend 5xx/error-rate signals on changed runtime paths.
- **FR-013**: System MUST preserve current account-level daily learning-limit behavior unless explicitly reconfigured.
- **FR-014**: System MUST keep discovery/import/study behavior encapsulated in flashcard-scoped modules (frontend features and backend services/controllers) so future non-flashcard learning modules can be added without changing flashcard API contracts or current flashcard user journeys.

### Key Entities *(include if feature involves data)*

- **Public Deck Catalog Entry**: Represents a deck that is visible in discovery, including visibility status, source owner identity, and summary metadata.
- **Private Study Deck**: Represents a learner-owned private deck shown in study/cards workspace, including editable metadata and contained cards.
- **Deck Import Copy Job**: Represents a user-triggered operation that clones a public deck into a learner-owned private deck by copying metadata, cards, and media references while excluding learning progress/history and preserving source-reference lineage for auditability.
- **Deck Merge Conflict Decision**: Represents an explicit learner choice artifact produced during re-import merge conflicts, capturing whether local or cloud data is selected for each conflicted field/item.
- **Study Session Card Queue Item**: Represents a card in active session flow with current side state, due status, and next-action options.
- **Card Scheduling State**: Represents per-learner per-card spaced-repetition attributes such as state, interval, ease, and next due timestamp.

### Assumptions

- Discovery of public decks is accessible to both guests and authenticated users.
- Existing deck/card ownership and authorization model remains available and unchanged.
- Existing spaced-repetition core can be extended/configured to support the specified new-card timing behavior.
- Future English-learning modules are out of scope for this feature and are addressed only through modular scope boundaries.

### Dependencies

- Existing authentication and authorization capabilities.
- Existing deck/card persistence model with support for visibility and ownership.
- Existing study scheduling engine with support for stateful spaced-repetition updates.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of decks shown in discovery satisfy public visibility rules in audit sampling.
- **SC-002**: 100% of decks shown in personal study/cards workspace are private decks owned by the acting learner.
- **SC-003**: At least 95% of successful public-deck imports produce a usable private copy in under 30 seconds.
- **SC-004**: 100% of unauthorized private-deck mutation attempts are denied with no persisted data change.
- **SC-005**: 100% of new-card rating outcomes map to specified first-step timing (`Again=1 minute`, `Hard=6 minutes`, `Good=10 minutes`, `Easy=1 day`) in automated verification scenarios.
- **SC-006**: At least 95% of study-session rating actions return updated next-card/schedule response in under 1 second under normal operating load.
- **SC-007**: Operational telemetry captures 100% of failed import attempts and 100% of study-rating failures with actionable diagnostics.
- **SC-008**: 100% of import and study requests from authenticated-but-unverified accounts are denied with a verification-required outcome in automated verification scenarios.
