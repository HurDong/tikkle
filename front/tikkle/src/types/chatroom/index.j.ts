import { Chat } from "../chat/index.j";

export interface Chatroom {
  roomId: string;
  partner: string;
  lastSender: string;
  lastMsg: string;
}

export interface ChatroomData {
  boardId: string;
  boardTitle: string;
  chats: Chat[];
  partnerName: string;
  status: string;
}

export interface ChatroomResponse {
  chatroom: ChatroomData;
}

export type ChatroomResponses = Chatroom[];

export interface Response<T> {
  success: boolean;
  data: T;
  message: string;
}