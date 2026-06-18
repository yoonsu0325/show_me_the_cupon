package cuponmaker.smtc.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus {
    /**
     * 2xx: Success (성공)
     */
    SUCCESS(HttpStatus.OK, true, 200, "요청에 성공하였습니다."),

    /**
     * 4xx: Client Error (클라이언트 에러)
     */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, false, 400, "올바르지 않은 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, false, 404, "요청하신 정보를 찾을 수 없습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, false, 413, "파일 업로드 용량 초과입니다. 파일 당 요청 합계 제한을 확인하세요."),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, false, 403, "접근 권한이 없습니다."),

    // 401 UNAUTHORIZED: 인증 실패
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, false, 401, "인증이 필요한 요청입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, false, 401, "인증이 실패했습니다."),

    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, false, 401, "유효하지 않은 JWT 토큰입니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, false, 401, "만료된 JWT 토큰입니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, false, 401, "지원되지 않는 형식의 JWT 토큰입니다."),
    INVALID_TOKEN_CLAIM(HttpStatus.UNAUTHORIZED, false, 401, "토큰 정보가 올바르지 않습니다."),

    /**
     * 5xx: Sever Error (서버 에러)
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, false, 500, "서버에서 예기치 않은 오류가 발생했습니다.");

    /**
     * Service Related Custom Errors
     */

    /**
     * 2000: Member Service 관련 에러
     */


    private final HttpStatusCode httpStatusCode;
    private final boolean isSuccess;
    private final int code;
    private final String message;

}
