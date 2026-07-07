package cuponmaker.smtc.domain.user.dto;

public record UserInfoResponse(
        Long userId,
        String email,
        String nickname
) {
}
