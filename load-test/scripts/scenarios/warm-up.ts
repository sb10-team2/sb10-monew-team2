import {sleep} from 'k6';
import {signin, signup} from '@/api/auth.api';
import {createUserDto} from "@/dto/user.dto";
import {generateSignupRequest} from "@/utils/data-factory";
import createComments from "@/api/comment.api";

export default function (articleIds: string[]): void {
  // sign-up
  const requestBody = generateSignupRequest();
  signup(requestBody);
  sleep(0.5);

  // sign-in
  const res = signin(requestBody);
  sleep(0.5);

  // post comments
  const userDto = createUserDto(res);
  createComments(userDto, articleIds);
  sleep(0.5);
}
