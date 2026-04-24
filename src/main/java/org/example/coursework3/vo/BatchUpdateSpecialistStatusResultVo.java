package org.example.coursework3.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateSpecialistStatusResultVo {
    private int successCount;
    private int failCount;
    private List<String> successIds;
    private List<BatchFailureVo> failures;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchFailureVo {
        private String id;
        private String reason;
    }
}
