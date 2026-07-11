package cuponmaker.smtc.domain.issue.dto;

public record LoadTestIssueResponse(
        boolean success,
        String code,
        String message
) {
}

