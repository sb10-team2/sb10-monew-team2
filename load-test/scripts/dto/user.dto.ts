import {UserResponse} from "@/types/api.type";
import config from "@/config";
import {getPersonaType} from "@/utils/data-factory";

export interface UserDto {
  id: string;
  maxComments: number;
  maxLikes: number;
  maxSubscriptions: number;
}

export function createUserDto(response: UserResponse): UserDto {
  const type: string = getPersonaType();
  const { ratio, ...persona} = config.persona[type];
  return {
    id: response.id,
    ...persona
  };
}
