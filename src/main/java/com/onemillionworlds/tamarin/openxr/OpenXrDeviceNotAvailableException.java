package com.onemillionworlds.tamarin.openxr;

public class OpenXrDeviceNotAvailableException extends RuntimeException{
    private final int errorCode;

    public OpenXrDeviceNotAvailableException(String message, int errorCode){
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode(){
        return errorCode;
    }
}
