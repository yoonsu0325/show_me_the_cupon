package cuponmaker.smtc.domain.user.dto;

public record LoginResponse(
        Long userId,
        String email,
        String nickname
) {
}
