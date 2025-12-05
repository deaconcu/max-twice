package com.prosper.learn.application.dto.response.old;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTOV2 {
    private Long id;
    private String name;
    private Boolean isCompleted;
}