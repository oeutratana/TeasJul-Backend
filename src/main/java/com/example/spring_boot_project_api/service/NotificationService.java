package com.example.spring_boot_project_api.service;

import org.springframework.data.domain.Pageable;

import com.example.spring_boot_project_api.dto.response.NotificationResponse;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UnreadCountResponse;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Listing;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;

public interface NotificationService {

    PageResponse<NotificationResponse> getMyNotifications(Pageable pageable);

    UnreadCountResponse getUnreadCount();

    NotificationResponse markAsRead(Long id);

    void markAllAsRead();

    // ---- Business-event hooks (called by existing services) -------------

    void notifyPropertySubmitted(Room room);

    void notifyPropertySubmitted(Listing listing);

    void notifyInvoiceCreated(Invoice invoice);

    void notifyPaymentCreated(Payment payment);

    void notifyPaymentSuccess(Payment payment);

    void notifyPaymentFailed(Payment payment);

    void notifyMaintenanceRequestCreated(MaintenanceRequest request);

    void notifyMaintenanceStatusChanged(MaintenanceRequest request);

    void notifyContractExpiring(RentalContract contract);

    void notifyRoomAssigned(RentalContract contract, Tenant tenant);
}