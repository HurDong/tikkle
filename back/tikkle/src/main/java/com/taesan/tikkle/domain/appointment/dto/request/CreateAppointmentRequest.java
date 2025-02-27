package com.taesan.tikkle.domain.appointment.dto.request;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {
	private UUID roomId;
	private Timestamp appTime;
	private Integer timeQnt; // 몇시간 약속인지
}
