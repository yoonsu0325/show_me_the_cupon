package cuponmaker.smtc.domain.user.controller;

import cuponmaker.smtc.domain.user.dto.UserInfoResponse;
import cuponmaker.smtc.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public UserInfoResponse me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return new UserInfoResponse(
                userDetails.getUserId(),
                userDetails.getEmail(),
                userDetails.getNickname()
        );
    }
}
