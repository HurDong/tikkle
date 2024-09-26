"use client";

import { useFetchChatroomById } from "@/hooks/chat/usefetchChatroomById";
import Loading from "@/components/loading/Loading";
import { useState, useEffect, useRef } from "react";
import { usePathname } from "next/navigation";
import Image from "next/image";
import Button from "@/components/button/Button";
import Badge from "@/components/badge/Badge";
import Link from "next/link";
import ChatList from "@/components/chat/ChatList";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { Chat } from "@/types/chat/index.j";

export default function ChatId() {
  const pathname = usePathname();

  // URL에서 roomId 추출 (예: '/chat/31000000-0000-0000-0000-000000000000')
  const roomId = pathname.split("/").pop(); // 경로의 마지막 부분이 roomId

  // 특정 유저 ID 설정
  const memberId = "74657374-3200-0000-0000-000000000000";
  const { data, error, isLoading } = useFetchChatroomById(roomId!);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(event.target.value);
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Enter") {
      handleSendMessage();
    }
  };

  const [inputValue, setInputValue] = useState(""); // 메시지 입력 값
  const [messages, setMessages] = useState<Chat[]>([]); // 메시지를 Chat[] 형식으로 관리
  const stompClientRef = useRef<Client | null>(null);

  useEffect(() => {
    const socket = new SockJS("http://localhost:8080/ws");
    const stompClient = new Client({
      webSocketFactory: () => socket,
    });

    // WebSocket 연결이 성공했을 때
    stompClient.onConnect = () => {
      console.log("WebSocket 연결됨");

      stompClient.subscribe(`/topic/chatroom.${roomId}`, (message) => {
        const receivedMessage = JSON.parse(message.body);
        const newChat = {
          content: receivedMessage.content,
          timestamp: receivedMessage.timestamp,
          senderId: receivedMessage.senderId,
        };

        setMessages((prevMessages) => [...prevMessages, newChat]);
        console.log("수신된 메시지:", newChat); // 수신된 메시지 로그
      });
    };

    // WebSocket 연결이 종료되었을 때
    stompClient.onDisconnect = () => {
      console.log("WebSocket 연결이 종료됨");
    };

    stompClient.activate();
    stompClientRef.current = stompClient;

    return () => {
      stompClient.deactivate();
    };
  }, [roomId]);

  const handleSendMessage = () => {
    if (stompClientRef.current && inputValue.trim() !== "") {
      const chatMessage = {
        chatroomId: roomId,
        senderId: memberId,
        content: inputValue,
      };

      const sendMessage = {
        destination: "/app/sendMessage",
        body: JSON.stringify(chatMessage),
      };

      stompClientRef.current.publish(sendMessage);
      console.log("메시지 전송:", sendMessage); // 전송한 메시지 로그
      setInputValue("");
    }
  };

  const combinedMessages = [...(data?.chats || []), ...messages];

  if (isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  if (error) {
    return <p>Error: {error.message}</p>;
  }

  return (
    <>
      {/* 채팅 헤더 */}
      <div className="item flex items-start justify-between self-stretch px-10 pb-0 pt-10">
        <div className="flex items-center gap-10">
          <Image
            src="/profile.png"
            alt={`${data?.partnerName} profile`}
            width={41}
            height={41}
            className="rounded-round"
          />
          <div className="flex py-10 text-28 font-bold text-teal-900">
            {data?.partnerName}님과의 대화
          </div>
        </div>
        <div>
          <Button size="m" variant="primary" design="fill" main="약속잡기" />
        </div>
      </div>
      <div className="flex items-center gap-6 self-stretch border-b border-b-coolGray300 p-10">
        <Badge size="l" color="yellow">
          {data?.status}
        </Badge>
        <Link href={`/board/${data?.boardId}`}>
          <div className="text-15">{data?.boardTitle}</div>
        </Link>
      </div>

      {/* 채팅 내용 */}
      <div className="scrollbar-custom flex flex-1 flex-col self-stretch overflow-y-auto">
        {combinedMessages.length > 0 ? (
          combinedMessages.map((chat, index) => (
            <ChatList
              key={index}
              content={chat.content}
              createdAt={chat.timestamp}
              senderId={chat.senderId}
              isMine={chat.senderId === memberId}
            />
          ))
        ) : (
          <div className="flex h-full items-center justify-center">
            <p className="text-center text-warmGray500">
              아직 메시지가 없습니다.
            </p>
          </div>
        )}
      </div>

      {/* 채팅 인풋 */}
      <div className="h-42 flex items-center justify-center self-stretch rounded-10 border border-coolGray400 p-10">
        <input
          type="text"
          placeholder="내용을 입력하세요."
          value={inputValue}
          onChange={handleInputChange}
          onKeyDown={handleKeyDown}
          className="flex-1 appearance-none bg-coolGray100 text-17 placeholder-warmGray300 focus:outline-none"
        />
      </div>
    </>
  );
}
