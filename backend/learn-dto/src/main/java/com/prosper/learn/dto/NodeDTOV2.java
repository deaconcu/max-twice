package com.prosper.learn.dto;

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