package org.example.coursework3.dto.request;

import lombok.Data;
import org.example.coursework3.entity.SpecialistStatus;

import java.util.List;

@Data
public class BatchUpdateSpecialistStatusRequest {
    private List<String> ids;
    private SpecialistStatus status;
}
