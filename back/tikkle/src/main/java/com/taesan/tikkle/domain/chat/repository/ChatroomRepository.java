package com.taesan.tikkle.domain.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.taesan.tikkle.domain.chat.entity.Chatroom;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface ChatroomRepository extends JpaRepository<Chatroom, UUID> {
	List<Chatroom> findByWriterId(UUID memberId);

	List<Chatroom> findByPerformerId(UUID memberId);

	@Query("SELECT c FROM Chatroom c WHERE c.performer.id = :memberId OR c.writer.id = :memberId")
	List<Chatroom> findByMemberId(@Param("memberId") UUID memberId);

	Optional<Chatroom> findByBoardIdAndWriterIdAndPerformerId(UUID boardId, UUID id, UUID performerId);
}
