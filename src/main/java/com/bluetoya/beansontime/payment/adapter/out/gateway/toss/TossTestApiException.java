package com.bluetoya.beansontime.payment.adapter.out.gateway.toss;

/** API 오류를 결제 거절로 단정하지 않는다. 원문 응답과 인증 정보는 예외에 담지 않는다. */
public class TossTestApiException extends RuntimeException {
  private final int statusCode;
  private final String code;

  public TossTestApiException(int statusCode, String code) {
    super("토스 테스트 API 결과 확인 필요: " + code);
    this.statusCode = statusCode;
    this.code = code;
  }

  public int statusCode() {
    return statusCode;
  }

  public String code() {
    return code;
  }
}
