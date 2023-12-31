package io.github.uptalent.content.controller;

import io.github.uptalent.content.model.request.PostKudosSkills;
import io.github.uptalent.content.model.request.AuthorUpdate;
import io.github.uptalent.content.model.request.ProofModify;
import io.github.uptalent.content.model.response.ContentDetailInfo;
import io.github.uptalent.content.model.response.PostKudosResult;
import io.github.uptalent.content.model.response.ProofDetailInfo;
import io.github.uptalent.content.model.response.ProofGeneralInfo;
import io.github.uptalent.content.service.ContentService;
import io.github.uptalent.content.service.ProofService;
import io.github.uptalent.content.service.ReportService;
import io.github.uptalent.starter.model.request.ReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static io.github.uptalent.starter.util.Constants.USER_ID_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/content/proofs")
public class ProofController {
    private final ContentService contentService;
    private final ProofService proofService;
    private final ReportService reportService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProofGeneralInfo> findAll() {
        return proofService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProofDetailInfo getProofDetailInfo(@PathVariable String id) {
        return proofService.getProofDetailInfo(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TALENT')")
    public URI saveProof(@Valid @RequestBody ProofModify proofModify) {
        return contentService.saveContent(proofModify);
    }

    @PatchMapping("/{contentId}")
    @PreAuthorize("hasAuthority('TALENT')")
    public ContentDetailInfo updateProof(@RequestHeader(USER_ID_KEY) Long userId,
                                         @PathVariable String contentId,
                                         @Valid @RequestBody ProofModify proofModify) {
        return contentService.updateContent(userId, contentId, proofModify);
    }

    @DeleteMapping("/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TALENT')")
    public void deleteProof(@RequestHeader(USER_ID_KEY) Long userId,
                            @PathVariable String proofId) {
        proofService.deleteProof(userId, proofId);
    }


    @PostMapping("/{proofId}/kudos")
    @PreAuthorize("hasAuthority('SPONSOR')")
    public PostKudosResult postKudos(@RequestHeader(USER_ID_KEY) Long userId,
                                     @Valid @RequestBody PostKudosSkills request,
                                     @PathVariable String proofId) {
        return proofService.postKudos(request, proofId, userId);
    }

    @PatchMapping("/author")
    @PreAuthorize("hasAuthority('TALENT')")
    public void updateProofsByAuthor(@RequestHeader(USER_ID_KEY) Long authorId,
                              @RequestBody AuthorUpdate authorUpdate) {
        proofService.updateProofsByAuthor(authorId, authorUpdate);
    }

    @DeleteMapping("/author/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProofsByAuthor(@PathVariable Long userId) {
        proofService.deleteProofsByAuthor(userId);
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public void reportProof(@PathVariable String id,
                            @RequestBody @Valid ReportRequest reportRequest){
        reportService.reportProof(id, reportRequest);
    }
}
