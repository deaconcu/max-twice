package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class UserDTOV3 {

    private Long id;

    private String name;

    private String biography;

    private Integer followed;

}
