package com.shadowtool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDetailDTO {
    private RequestLogDTO request;
    private ResponseLogDTO v1Response;
    private ResponseLogDTO v2Response;
    private ComparisonDTO comparison;
}
