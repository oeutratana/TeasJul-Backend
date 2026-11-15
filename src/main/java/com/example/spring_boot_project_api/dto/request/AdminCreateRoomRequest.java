package com.example.spring_boot_project_api.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminCreateRoomRequest extends RoomRepuest {

    /** When true the room is created and immediately published as approved by the admin. */
    private Boolean publishImmediately = false;
}