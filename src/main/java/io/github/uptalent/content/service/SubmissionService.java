package io.github.uptalent.content.service;

import io.github.uptalent.content.client.AccountClient;
import io.github.uptalent.content.exception.DuplicateSubmissionException;
import io.github.uptalent.content.exception.IllegalActionToSubmissionException;
import io.github.uptalent.content.exception.SubmissionNotFoundException;
import io.github.uptalent.content.mapper.VacancyMapper;
import io.github.uptalent.content.model.document.Submission;
import io.github.uptalent.content.model.document.Vacancy;
import io.github.uptalent.content.model.request.SubmissionRequest;
import io.github.uptalent.content.model.response.SubmissionDetailInfo;
import io.github.uptalent.content.model.response.TalentSubmission;
import io.github.uptalent.content.repository.SubmissionRepository;
import io.github.uptalent.starter.model.common.Author;
import io.github.uptalent.starter.model.common.EventNotificationMessage;
import io.github.uptalent.starter.pagination.PageWithMetadata;
import io.github.uptalent.starter.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.uptalent.content.model.enums.SubmissionStatus.SENT;
import static io.github.uptalent.starter.model.enums.EventNotificationType.POST_SUBMISSION;
import static io.github.uptalent.starter.util.Constants.SPONSOR_SUFFIX;
import static io.github.uptalent.starter.util.Constants.SUBMISSIONS_LINK;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final EventNotificationProducerService eventNotificationProducerService;
    private final SubmissionRepository submissionRepository;
    private final VacancyMapper vacancyMapper;
    private final AccountClient accountClient;
    private final VacancyService vacancyService;

    public SubmissionDetailInfo sendSubmission(Long talentId, String vacancyId,
                                               SubmissionRequest submissionRequest) {
        Vacancy vacancy = vacancyService.getVacancyById(vacancyId);
        checkTalentSubmissionForVacancy(talentId, vacancy);

        Submission submission = vacancyMapper.toSubmission(submissionRequest);
        Author authorTalent = accountClient.getAuthor();

        submission.setVacancy(vacancy);
        submission.setAuthor(authorTalent);
        submission.setSent(LocalDateTime.now());
        submission.setStatus(SENT);

        submission = submissionRepository.save(submission);

        vacancy.getSubmissions().add(submission);
        vacancyService.update(vacancy);

        sendEventNotification(authorTalent, vacancy.getAuthor().getId(), submission.getId());

        return vacancyMapper.toSubmissionDetailInfo(submission);
    }

    public PageWithMetadata<TalentSubmission> getTalentSubmissions(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Submission> submissionsPage = submissionRepository.findSubmissionsByAuthorId(pageRequest, userId);

        List<TalentSubmission> talentSubmissions = submissionsPage
                .stream()
                .map(submission -> TalentSubmission.builder()
                        .vacancy(vacancyMapper.toContentGeneralInfo(submission.getVacancy()))
                        .submission(vacancyMapper.toSubmissionGeneralInfo(submission))
                        .build())
                .toList();

        return new PageWithMetadata<>(talentSubmissions, submissionsPage.getTotalPages());
    }

    public void deleteSubmission(String submissionId, Long userId, String userRole) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(SubmissionNotFoundException::new);

        if (userRole.equals(Role.TALENT.name()) && !userId.equals(submission.getAuthor().getId())) {
            throw new AccessDeniedException("You have not access to the submission");
        }
        if (userRole.equals(Role.SPONSOR.name()) && !userId.equals(submission.getVacancy().getAuthor().getId())) {
            throw new IllegalActionToSubmissionException("Cannot delete submission from this vacancy");
        }
        if (!submission.getStatus().equals(SENT)) {
            throw new IllegalActionToSubmissionException("Cannot delete approved or denied submissions");
        }

        submissionRepository.delete(submission);
    }

    private void checkTalentSubmissionForVacancy(Long talentId, Vacancy vacancy) {
        if(submissionRepository.existsSubmissionByAuthorIdAndVacancyId(talentId, vacancy.getId()))
            throw new DuplicateSubmissionException();
    }

    private void sendEventNotification(Author talent, Long sponsorId, String submissionId) {
        String to = sponsorId + SPONSOR_SUFFIX;
        String messageBody = POST_SUBMISSION.getMessageBody();
        String contentLink =  SUBMISSIONS_LINK + submissionId;

        var eventNotificationMessage = EventNotificationMessage.builder()
                .from(talent)
                .to(to)
                .message(messageBody)
                .contentLink(contentLink)
                .build();
        eventNotificationProducerService.sendEventNotificationMsg(eventNotificationMessage);
    }
}
