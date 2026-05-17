import {UserResponse} from "@/types/api.type";
import config from "@/config";
import {getPersonaType} from "@/utils/data-factory";

export interface UserDto {
  id: string;
  maxComments: number;
  maxLikes: number;
  maxInterests: number;
  maxKeywords: number;
  minNotifications: number;
  minArticles: number;
}

export function toUserDto(response: UserResponse): UserDto {
  const type: string = getPersonaType();
  const { ratio, ...persona} = config.persona[type as keyof typeof config.persona];
  return {
    id: response.id,
    ...persona
  };
}
