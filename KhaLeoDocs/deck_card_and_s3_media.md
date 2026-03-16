# SPEC 003: Deck/Card Management and AWS S3 Media Integration

## 1. Context & Objective
This specification covers the core content management domain. It defines the REST APIs for creating, reading, updating, and deleting (CRUD) Decks and Flashcards. It also implements the AWS S3 Presigned URL mechanism to handle media uploads (images and audio) directly from the client to S3, reducing the load on the backend EC2 instances.

## 2. Technical Constraints
- **AWS S3 Integration:** Use AWS SDK v2 (`software.amazon.awssdk:s3`). The backend MUST NOT process multipart file uploads for media. Instead, it must generate a short-lived (e.g., 5 minutes) Presigned PUT URL.
- **Media Restrictions:** Max file size is 5MB. Allowed formats: `image/jpeg`, `image/png`, `audio/mpeg`, `audio/webm`.
- **Pagination:** All collection endpoints (e.g., listing decks, listing cards in a deck) must utilize Spring Data JPA's `Pageable` and return paginated responses.
- **Authorization:** Only the `author` (User) or an `ADMIN` can modify or delete a Deck/Card. Public decks can be read by anyone.

## 3. Deck Management (APIs)
- `POST /api/v1/decks`: Create a new deck. Requires Name, Description, Tags, Visibility (Public/Private).
- `GET /api/v1/decks`: List decks (Paginated). Support filtering by `isPublic=true` for exploring, or `authorId` for user's own decks.
- `GET /api/v1/decks/{id}`: Get deck details.
- `PUT /api/v1/decks/{id}`: Update deck metadata.
- `DELETE /api/v1/decks/{id}`: Delete deck (Cascade delete all associated cards and learning states).

## 4. Card Management & Advanced Search (APIs)
- `POST /api/v1/decks/{deckId}/cards`: Add a card to a deck.
- `PUT /api/v1/cards/{id}`: Update card content.
- `DELETE /api/v1/cards/{id}`: Delete a specific card.
- `GET /api/v1/decks/{deckId}/cards/search`: **Advanced Search Endpoint**. Must support searching within the specific deck by `frontText`, `backText`, or exact vocabulary match. Use Spring Data JPA Specifications or JPQL for efficient querying.

## 5. AWS S3 Media Upload Flow (APIs)
- `GET /api/v1/media/presigned-url?fileName={name}&contentType={type}`: 
  - Validates the extension and content type.
  - Returns a securely generated Presigned URL and the final S3 Object Key.
  - The client uses this URL to upload the file directly to S3.
  - The client then passes the S3 Object Key/URL in the `frontMediaUrl` or `backMediaUrl` payload when creating/updating a Card or Deck Cover.

## 6. Execution Instructions for AI
Generate the Controllers, Services, and Repositories for Decks and Cards. Implement the AWS S3 Service for generating presigned URLs. Ensure rigorous ownership checks (RBAC) in the Service layer before allowing updates or deletions.