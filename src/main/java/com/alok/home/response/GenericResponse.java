package com.alok.home.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenericResponse {

    private Status status;
    private String message;

    public enum Status {
        SUCCESS,
        FAILED
    }
}
