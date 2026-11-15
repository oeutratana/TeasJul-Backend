package com.example.spring_boot_project_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.service.RentalContractService;

import lombok.RequiredArgsConstructor;

/**
 * Periodically transitions ACTIVE contracts whose end date has passed into EXPIRED
 * so the persisted contract status stays accurate even when nobody opens the contract
 * pages. The same reconciliation also runs defensively on contract read endpoints.
 */
@Component
@RequiredArgsConstructor
public class ContractExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContractExpiryScheduler.class);

    private final RentalContractService rentalContractService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireContracts() {
        try {
            int expiredCount = rentalContractService.expireExpiredContracts();
            if (expiredCount > 0) {
                log.info("Marked {} rental contract(s) as EXPIRED.", expiredCount);
            }
        } catch (RuntimeException e) {
            log.error("Contract expiry job failed", e);
        }
    }
}