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

    // Common

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, false, 500, "서버에서 예기치 않은 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, false, 400, "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, false, 405, "지원하지 않는 HTTP 메서드입니다."),

    // Auth

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, false, 401, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, false, 403, "접근 권한이 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, false, 1001, "이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, false, 1002, "이미 사용 중인 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, false, 1003, "사용자를 찾을 수 없습니다."),

    // Coupon Event

    COUPON_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, false, 2001, "쿠폰 이벤트를 찾을 수 없습니다."),
    INVALID_COUPON_EVENT_PERIOD(HttpStatus.BAD_REQUEST, false, 2002, "쿠폰 이벤트 기간이 올바르지 않습니다."),
    INVALID_COUPON_QUANTITY(HttpStatus.BAD_REQUEST, false, 2003, "쿠폰 수량은 1개 이상이어야 합니다."),
    COUPON_EVENT_NOT_OPEN(HttpStatus.BAD_REQUEST, false, 2004, "아직 쿠폰 발급 가능 시간이 아닙니다."),
    COUPON_EVENT_CLOSED(HttpStatus.BAD_REQUEST, false, 2005, "쿠폰 발급 기간이 종료되었습니다."),

    // Coupon Issue

    COUPON_SOLD_OUT(HttpStatus.CONFLICT, false, 3001, "쿠폰 수량이 모두 소진되었습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, false, 3002, "이미 발급받은 쿠폰입니다."),
    COUPON_ISSUE_NOT_FOUND(HttpStatus.NOT_FOUND, false, 3003, "쿠폰 발급 내역을 찾을 수 없습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, false, 3004, "이미 사용된 쿠폰입니다."),
    COUPON_EXPIRED(HttpStatus.CONFLICT, false, 3005, "만료된 쿠폰입니다."),

    // Batch

    BATCH_JOB_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, false, 4001, "배치 작업 실행 중 오류가 발생했습니다.");


    private final HttpStatusCode httpStatusCode;
    private final boolean isSuccess;
    private final int code;
    private final String message;

}
