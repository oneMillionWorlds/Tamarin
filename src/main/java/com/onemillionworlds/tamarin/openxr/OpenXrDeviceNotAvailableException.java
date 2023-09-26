package com.onemillionworlds.tamarin.openxr;

import lombok.Getter;

public class OpenXrDeviceNotAvailableException extends RuntimeException{
    @Getter
    private final int errorCode;

    public OpenXrDeviceNotAvailableException(String message, int errorCode){
        super(message);
        this.errorCode = errorCode;
    }
}
