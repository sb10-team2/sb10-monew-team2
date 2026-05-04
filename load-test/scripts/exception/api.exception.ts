export class ApiException extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.status = status;
    this.name = 'ApiException';
  }
}

export class UnauthorizedException extends ApiException {
  constructor(message: string = '인증에 실패했습니다.') {
    super(401, message);
    this.name = 'UnauthorizedException';
  }
}
