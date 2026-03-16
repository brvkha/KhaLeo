package com.khaleo.flashcard.controller.media;

import com.khaleo.flashcard.controller.media.dto.MediaAuthorizationResponse;
import com.khaleo.flashcard.service.media.MediaAuthorizationService;
import com.khaleo.flashcard.service.media.S3PresignedUrlService;
import com.khaleo.flashcard.service.persistence.DeckCardAccessGuard;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaAuthorizationService mediaAuthorizationService;
    private final DeckCardAccessGuard deckCardAccessGuard;

    @GetMapping("/presigned-url")
    public MediaAuthorizationResponse authorizePresignedUpload(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam long sizeBytes) {
        UUID actorId = deckCardAccessGuard.requireAuthenticatedUserId("authorize", "media", fileName);
        S3PresignedUrlService.PresignedUpload upload = mediaAuthorizationService.authorize(actorId, fileName, contentType, sizeBytes);
        return new MediaAuthorizationResponse(upload.objectKey(), upload.uploadUrl(), upload.expiresInSeconds());
    }
}
