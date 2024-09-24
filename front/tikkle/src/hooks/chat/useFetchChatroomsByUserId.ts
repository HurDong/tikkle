import { useQuery } from "@tanstack/react-query";
import { fetchChatroomsByUserId } from "@/libs/chat";
import { ChatroomResponses } from "@/types/chatroom/index.j";

// 특정 유저의 채팅 목록을 조회하는 훅
export const useFetchChatroomsByUserId = (memberId: string) => {
  return useQuery<ChatroomResponses, Error>({
    queryKey: ["chatrooms", memberId], // 캐시 관리를 위한 queryKey
    queryFn: () => fetchChatroomsByUserId(memberId), // 데이터를 가져오는 함수
  });
};
