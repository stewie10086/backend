package org.example.coursework3.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateBookingRequest {
    @NotBlank(message = "专家ID不能为空")
    private String specialistId;
    @NotBlank(message = "时段ID不能为空")
    private String slotId;
    @NotBlank(message = "支付单ID不能为空")
    private String paymentId;
    private String note;
}
