package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.enums.RoomTypeStatus;

import lombok.Data;

@Data
public class RoomTypeResponse {
    
    private Long id;
    private Long ownerId;
    private String name;
    private String code;
    private String description;
    private Integer capacity;
    private BigDecimal defaultRent;
    private BigDecimal defaultDeposit;
    private RoomTypeStatus status;
    private String primaryImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; 

}
