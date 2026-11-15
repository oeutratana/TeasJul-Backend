package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.TenantMeContract;
import com.example.spring_boot_project_api.dto.response.TenantMeHouse;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantMeRoom;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UtilityMeter;
import com.example.spring_boot_project_api.model.UtilityReading;
import com.example.spring_boot_project_api.model.UtilityType;

@Component
public class TenantPortalMapper {

    public TenantMeHouse toMeHouse(House house) {
        if (house == null) {
            return null;
        }
        return new TenantMeHouse(house.getId(), house.getName(), house.getCode(), house.getAddress());
    }

    public TenantMeRoom toMeRoom(Room room) {
        if (room == null) {
            return null;
        }
        return new TenantMeRoom(room.getId(), room.getRoomNumber(), room.getName(),
                room.getFloorNumber(), room.getMonthlyRent(), room.getSecurityDeposit());
    }

    public TenantMeContract toMeContract(RentalContract contract, BigDecimal depositPaidAmount,
            boolean depositFullyPaid) {
        if (contract == null) {
            return null;
        }
        return new TenantMeContract(contract.getId(), contract.getContractNo(), contract.getStartDate(),
                contract.getEndDate(), contract.getMonthlyRent(), contract.getSecurityDeposit(),
                contract.getDueDay(), depositPaidAmount, depositFullyPaid, contract.getStatus());
    }

    public TenantMeResponse toMeResponse(Tenant tenant, TenantMeHouse house, TenantMeRoom room,
            TenantMeContract contract) {
        User owner = tenant.getOwner();
        return new TenantMeResponse(
                tenant.getId(),
                tenant.getUser() != null ? tenant.getUser().getId() : null,
                owner != null ? owner.getId() : null,
                owner != null ? fullName(owner) : null,
                owner != null ? owner.getPhone() : null,
                owner != null ? owner.getEmail() : null,
                tenant.getTenantCode(),
                tenant.getFirstName(),
                tenant.getLastName(),
                tenant.getGender(),
                tenant.getDateOfBirth(),
                tenant.getPhone(),
                tenant.getEmail(),
                tenant.getOccupationType(),
                tenant.getOccupation(),
                tenant.getSchoolOrCompany(),
                tenant.getIdType(),
                tenant.getIdNumber(),
                tenant.getAddress(),
                tenant.getStatus(),
                house,
                room,
                contract,
                tenant.getCreatedAt(),
                tenant.getUpdatedAt());
    }

    public TenantUtilityResponse toUtilityResponse(UtilityMeter meter, UtilityType type,
            UtilityReading latest) {
        return new TenantUtilityResponse(
                meter.getId(),
                meter.getMeterNumber(),
                type != null ? type.getName() : null,
                type != null ? type.getUnit() : null,
                latest != null ? latest.getCurrentReading() : null,
                latest != null ? latest.getUsage() : null,
                latest != null ? latest.getReadingDate() : null);
    }

    private String fullName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        return (first + " " + last).trim();
    }
}