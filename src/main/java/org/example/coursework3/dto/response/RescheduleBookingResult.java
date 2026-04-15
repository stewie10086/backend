package org.example.coursework3.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.coursework3.entity.BookingStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleBookingResult {
    private String id;
    private BookingStatus status = BookingStatus.Rescheduled;
    private String slotId;
}
