package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.dto.PricingQuoteRequest;
import org.example.coursework3.entity.Pricing;
import org.example.coursework3.repository.PricingRepository;
import org.example.coursework3.result.PricingQuoteResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;
    public PricingQuoteResult getQuote(PricingQuoteRequest request) {
        String specialistId = request.getSpecialistId();
        Integer duration = request.getDuration();
        String type = request.getType();

        if (specialistId == null || specialistId.isBlank()) {
            throw new MsgException("specialistId is required");
        }

        if (type != null && type.isBlank()) {
            type = null;
        }

        Pricing pricing;
        if (duration != null && type != null) {
            pricing = pricingRepository
                    .findBySpecialistIdAndDurationAndType(specialistId, duration, type)
                    .orElseThrow(() -> new MsgException("Pricing not found"));
        } else if (duration != null) {
            pricing = pricingRepository
                    .findFirstBySpecialistIdAndDurationOrderByCreatedAtDesc(specialistId, duration)
                    .orElseThrow(() -> new MsgException("Pricing not found"));
        } else if (type != null) {
            pricing = pricingRepository
                    .findFirstBySpecialistIdAndTypeOrderByCreatedAtDesc(specialistId, type)
                    .orElseThrow(() -> new MsgException("Pricing not found"));
        } else {
            pricing = pricingRepository
                    .findFirstBySpecialistIdOrderByCreatedAtDesc(specialistId)
                    .orElseThrow(() -> new MsgException("Pricing not found"));
        }


        return new PricingQuoteResult(
                pricing.getAmount(),
                pricing.getCurrency(),
                pricing.getDetail()
        );
    }

}
