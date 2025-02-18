package com.taesan.tikkle.domain.appointment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taesan.tikkle.domain.account.dto.response.TradeLogFindAllResponse;
import com.taesan.tikkle.domain.account.entity.Account;
import com.taesan.tikkle.domain.account.repository.AccountRepository;
import com.taesan.tikkle.domain.appointment.dto.request.CreateAppointmentRequest;
import com.taesan.tikkle.domain.appointment.dto.response.BriefAppointmentResponse;
import com.taesan.tikkle.domain.appointment.dto.response.DetailAppointmentResponse;
import com.taesan.tikkle.domain.appointment.dto.response.TodoAppointmentResponse;
import com.taesan.tikkle.domain.appointment.entity.Appointment;
import com.taesan.tikkle.domain.appointment.repository.AppointmentRepository;
import com.taesan.tikkle.domain.board.entity.Board;
import com.taesan.tikkle.domain.board.repository.BoardRepository;
import com.taesan.tikkle.domain.chat.entity.Chatroom;
import com.taesan.tikkle.domain.chat.repository.ChatroomRepository;
import com.taesan.tikkle.domain.file.service.FileService;
import com.taesan.tikkle.domain.member.entity.Member;
import com.taesan.tikkle.domain.member.repository.MemberRepository;
import com.taesan.tikkle.global.errors.ErrorCode;
import com.taesan.tikkle.global.exceptions.CustomException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;

	private final ChatroomRepository chatroomRepository;

	private final BoardRepository boardRepository;

	private final AccountRepository accountRepository;

	private final MemberRepository memberRepository;

	private final FileService fileService;

	@Transactional(readOnly = true)
	public List<TodoAppointmentResponse> getTodoAppointments(UUID memberId) {
		// 🎯 현재 로그인한 사용자가 performer 또는 writer로 속한 채팅방 조회
		List<Chatroom> chatrooms = chatroomRepository.findByMemberId(memberId);

		// 🎯 각 채팅방에서 최신 약속(삭제되지 않은 것 중 가장 최근 것)만 추출
		List<Appointment> appointments = extractLatestAppointments(chatrooms);

		// 🎯 약속과 연결된 Board 엔티티들을 한 번에 조회하여 캐싱 (N+1 문제 방지)
		Map<UUID, Board> boardCache = extractBoardCache(appointments);

		// 🎯 Appointment 데이터를 TodoAppointmentResponse 형식으로 변환하여 반환
		return convertToTodoAppointmentResponses(appointments, boardCache, memberId);
	}

	@Deprecated
	private void extractTodoAppointments(List<Chatroom> chatrooms, List<Appointment> appointments) {
		chatrooms.stream().map(Chatroom::getAppointments) // chatrooms 각각의 appointments에 대하여
			.filter(appts -> !appts.isEmpty()) // 비어 있지 않은 appointments 중에서
			.map(appts -> appts.get(appts.size() - 1)) // 가장 최신 appointment을 골라서
			.filter(appt -> !appt.isDeleted()) // 삭제 되지 않은 것만
			.forEach(appointments::add); // 리스트에 추가
	}

	private List<Appointment> extractLatestAppointments(List<Chatroom> chatrooms) {
		return chatrooms.stream()
			.flatMap(chatroom -> chatroom.getAppointments().stream()) // chatroom의 appointments 스트림 변환
			.filter(appt -> !appt.isDeleted()) // 삭제되지 않은 약속만
			.sorted(Comparator.comparing(Appointment::getApptTime).reversed()) // 최신 약속 우선 정렬
			.limit(1) // 각 chatroom에서 가장 최신 약속만 선택
			.collect(Collectors.toList()); // 리스트로 반환
	}

	private Map<UUID, Board> extractBoardCache(List<Appointment> appointments) {
		return boardRepository.findAllById(
			appointments.stream().map(appt -> appt.getRoom().getBoard().getId()) // 각 Appointment가 속한 Board ID 추출
				.collect(Collectors.toSet()) // 중복 제거를 위해 Set으로 변환
		).stream().collect(Collectors.toMap(Board::getId, board -> board)); // Board ID를 Key로, Board 객체를 Value로 저장
	}

	private List<TodoAppointmentResponse> convertToTodoAppointmentResponses(List<Appointment> appointments,
		Map<UUID, Board> boardCache, UUID memberId) {
		return appointments.stream().map(appointment -> {
			Chatroom chatroom = appointment.getRoom();
			Board board = boardCache.get(
				chatroom.getBoard().getId()); // boardCache에서 현재 chatroom의 board ID로 Board 객체 가져오기

			if (board == null) {
				throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
			}

			// 접속한 memberId와 performerId를 비교하여 상대방 정보 설정
			String partner = chatroom.getPerformer().getId().equals(memberId) ? chatroom.getWriter().getName() :
				chatroom.getPerformer().getName();

			return new TodoAppointmentResponse(appointment.getId(), // Appointment ID
				board.getStatus(), // Board 상태
				partner, // 대화 상대 이름
				appointment.getApptTime(), // 약속 시간
				board.getTitle(), // Board 제목
				chatroom.getId() // Chatroom ID
			);
		}).collect(Collectors.toList()); // 변환된 TodoAppointmentResponse 리스트 반환
	}

	@Transactional
	public UUID createAppointment(CreateAppointmentRequest request, UUID memberId) {
		Chatroom chatroom = chatroomRepository.findById(request.getRoomId())
			.orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));
		Appointment appointment = new Appointment(chatroom, request.getAppTime(), request.getTimeQnt());
		appointmentRepository.save(appointment);
		// TODO : Board Status 무슨 String으로 설정할지 / 약속 삭제하지 않고 생성하기 호출할 땐 어떻게 해야하는가 / 삭제된 Board에 접근 시 예외 처리?
		boardRepository.findById(chatroom.getBoard().getId())
			.orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND))
			.changeStatus("진행중");
		List<Appointment> appointments = chatroom.getAppointments();

		if (!appointments.isEmpty()) {
			appointments.get(appointments.size() - 1).softDelete();
		}

		appointments.add(appointment);

		// 보증금 받기
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
		Account account = accountRepository.findByMemberIdAndDeletedAtIsNull(member.getId())
			.orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
		if (account.getTimeQnt() < appointment.getTimeQnt())
			throw new CustomException(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
		account.setBalance(account.getTimeQnt() - appointment.getTimeQnt());
		return appointment.getId();
	}

	@Transactional
	public void deleteAppointment(UUID appointmentId, UUID memberId) {
		Appointment appointment = appointmentRepository.findById(appointmentId)
			.orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));
		if (memberId.equals(appointment.getRoom().getWriter().getId())) {
			appointment.softDelete();
			Chatroom chatroom = chatroomRepository.findById(appointment.getRoom().getId())
				.orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));
			Board board = boardRepository.findById(chatroom.getBoard().getId())
				.orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
			board.changeStatus("진행전");
			// 보증금 받기
			Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
			Account account = accountRepository.findByMemberIdAndDeletedAtIsNull(member.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
			account.setBalance(account.getTimeQnt() + appointment.getTimeQnt());
			// TODO : 존재하는 약속이지만 이미 삭제된 약속이라면?
		} else {
			throw new CustomException(ErrorCode.APPOINTMENT_NOT_AUTHORIZED);
		}
	}

	@Transactional(readOnly = true)
	public List<DetailAppointmentResponse> getAppointments(UUID memberId) {
		List<DetailAppointmentResponse> responses = new ArrayList<>();
		extractAppointmentFromChatroom(chatroomRepository.findByPerformerId(memberId), responses);
		extractAppointmentFromChatroom(chatroomRepository.findByWriterId(memberId), responses);
		responses.sort(Comparator.comparing(DetailAppointmentResponse::getCreatedAt).reversed());
		return responses;
	}

	private void extractAppointmentFromChatroom(List<Chatroom> chatrooms, List<DetailAppointmentResponse> responses) {
		for (Chatroom chatroom : chatrooms) {
			if (!chatroom.getAppointments().isEmpty()) {
				Appointment appointment = chatroom.getAppointments().get(chatroom.getAppointments().size() - 1);
				if (!appointment.isDeleted()) {
					responses.add(new DetailAppointmentResponse(appointment.getId(), appointment.getApptTime(),
						appointment.getTimeQnt(), appointment.getCreatedAt(), chatroom.getBoard().getTitle()));
				}
			}
		}
	}

	public BriefAppointmentResponse getAppointment(UUID roomId, UUID memberId) {
		Optional<Appointment> appointment = appointmentRepository.findByRoomIdAndDeletedAtIsNull(roomId);
		if (appointment.isEmpty())
			return new BriefAppointmentResponse();
		if (!memberId.equals(appointment.get().getRoom().getWriter().getId()) && !memberId.equals(
			appointment.get().getRoom().getPerformer().getId())) {
			throw new CustomException(ErrorCode.APPOINTMENT_NOT_AUTHORIZED);
		} else {
			return new BriefAppointmentResponse(appointment.get().getId(), appointment.get().getApptTime(),
				appointment.get().getTimeQnt());
		}
	}

	@Transactional(readOnly = true)
	public List<TradeLogFindAllResponse> getAppointedBoardsByMemberId(UUID username) {

		List<Appointment> appointments = appointmentRepository.findAppointmentsWithBoardByMemberId(username);

		return appointments.stream().map(appointment -> {
			UUID memberId = username.equals(appointment.getRoom().getWriter().getId()) ?
				appointment.getRoom().getPerformer().getId() : appointment.getRoom().getWriter().getId();
			Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId).get();
			Board board = appointment.getRoom().getBoard();
			byte[] partnerImage = fileService.getProfileImage(memberId);  // partnerImage 조회 로직 추가
			return TradeLogFindAllResponse.from(board, member, partnerImage);
		}).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<TradeLogFindAllResponse> searchBoardsByMemberIdAndKeyword(UUID memberId, String keyword) {
		// 1. Board 목록을 검색
		List<Board> boards = appointmentRepository.searchBoardsByMemberIdAndKeyword(memberId, keyword);

		// 2. 검색된 Board를 TradeLogFindAllResponse로 변환
		return boards.stream().map(TradeLogFindAllResponse::from)  // Board -> TradeLogFindAllResponse 변환
			.collect(Collectors.toList());
	}

}
