package org.example.coursework3.service;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.entity.Slot;
import org.example.coursework3.repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class AutoSlotUpdateService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long CANCEL_BEFORE_START_MINUTES = 10;

    @Autowired
    private SlotRepository slotRepository;


    //5分钟一次检查，临场10分钟取消
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoInactiveSlot(){
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        List<Slot> slots = slotRepository.getSlotByStartTimeBeforeAndAvailableTrue(now.plusMinutes(CANCEL_BEFORE_START_MINUTES));
        if (slots.isEmpty()){
            log.info("暂无过期时段");
        }else{
            for (Slot slot : slots){
                slot.setAvailable(false);
                slotRepository.save(slot);
                log.info("Auto-cancelled slot id={}", slot.getId());
            }
        }
    }


}
