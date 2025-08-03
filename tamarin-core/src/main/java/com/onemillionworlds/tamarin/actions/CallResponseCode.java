package com.onemillionworlds.tamarin.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class maps the error codes found within XR10 to be able to give better error messages.
 */
public class CallResponseCode{

    public static final int
            XR_SUCCESS                                   = 0,
            XR_TIMEOUT_EXPIRED                           = 1,
            XR_SESSION_LOSS_PENDING                      = 3,
            XR_EVENT_UNAVAILABLE                         = 4,
            XR_SPACE_BOUNDS_UNAVAILABLE                  = 7,
            XR_SESSION_NOT_FOCUSED                       = 8,
            XR_FRAME_DISCARDED                           = 9,
            XR_ERROR_VALIDATION_FAILURE                  = -1,
            XR_ERROR_RUNTIME_FAILURE                     = -2,
            XR_ERROR_OUT_OF_MEMORY                       = -3,
            XR_ERROR_API_VERSION_UNSUPPORTED             = -4,
            XR_ERROR_INITIALIZATION_FAILED               = -6,
            XR_ERROR_FUNCTION_UNSUPPORTED                = -7,
            XR_ERROR_FEATURE_UNSUPPORTED                 = -8,
            XR_ERROR_EXTENSION_NOT_PRESENT               = -9,
            XR_ERROR_LIMIT_REACHED                       = -10,
            XR_ERROR_SIZE_INSUFFICIENT                   = -11,
            XR_ERROR_HANDLE_INVALID                      = -12,
            XR_ERROR_INSTANCE_LOST                       = -13,
            XR_ERROR_SESSION_RUNNING                     = -14,
            XR_ERROR_SESSION_NOT_RUNNING                 = -16,
            XR_ERROR_SESSION_LOST                        = -17,
            XR_ERROR_SYSTEM_INVALID                      = -18,
            XR_ERROR_PATH_INVALID                        = -19,
            XR_ERROR_PATH_COUNT_EXCEEDED                 = -20,
            XR_ERROR_PATH_FORMAT_INVALID                 = -21,
            XR_ERROR_PATH_UNSUPPORTED                    = -22,
            XR_ERROR_LAYER_INVALID                       = -23,
            XR_ERROR_LAYER_LIMIT_EXCEEDED                = -24,
            XR_ERROR_SWAPCHAIN_RECT_INVALID              = -25,
            XR_ERROR_SWAPCHAIN_FORMAT_UNSUPPORTED        = -26,
            XR_ERROR_ACTION_TYPE_MISMATCH                = -27,
            XR_ERROR_SESSION_NOT_READY                   = -28,
            XR_ERROR_SESSION_NOT_STOPPING                = -29,
            XR_ERROR_TIME_INVALID                        = -30,
            XR_ERROR_REFERENCE_SPACE_UNSUPPORTED         = -31,
            XR_ERROR_FILE_ACCESS_ERROR                   = -32,
            XR_ERROR_FILE_CONTENTS_INVALID               = -33,
            XR_ERROR_FORM_FACTOR_UNSUPPORTED             = -34,
            XR_ERROR_FORM_FACTOR_UNAVAILABLE             = -35,
            XR_ERROR_API_LAYER_NOT_PRESENT               = -36,
            XR_ERROR_CALL_ORDER_INVALID                  = -37,
            XR_ERROR_GRAPHICS_DEVICE_INVALID             = -38,
            XR_ERROR_POSE_INVALID                        = -39,
            XR_ERROR_INDEX_OUT_OF_RANGE                  = -40,
            XR_ERROR_VIEW_CONFIGURATION_TYPE_UNSUPPORTED = -41,
            XR_ERROR_ENVIRONMENT_BLEND_MODE_UNSUPPORTED  = -42,
            XR_ERROR_NAME_DUPLICATED                     = -44,
            XR_ERROR_NAME_INVALID                        = -45,
            XR_ERROR_ACTIONSET_NOT_ATTACHED              = -46,
            XR_ERROR_ACTIONSETS_ALREADY_ATTACHED         = -47,
            XR_ERROR_LOCALIZED_NAME_DUPLICATED           = -48,
            XR_ERROR_LOCALIZED_NAME_INVALID              = -49,
            XR_ERROR_GRAPHICS_REQUIREMENTS_CALL_MISSING  = -50,
            XR_ERROR_RUNTIME_UNAVAILABLE                 = -51;
    
    public static final CallResponseCode FULL_SUCCESS = new CallResponseCode(XR_SUCCESS, "XR_SUCCESS", "The provided XrSystemId was invalid");
    private static final Map<Integer, CallResponseCode> responseCodes = new HashMap<>();

    static{
        responseCodes.put(XR_SUCCESS, FULL_SUCCESS);
        responseCodes.put(XR_TIMEOUT_EXPIRED, new CallResponseCode(XR_TIMEOUT_EXPIRED, "XR_TIMEOUT_EXPIRED", "The specified timeout time occurred before the operation could complete"));
        responseCodes.put(XR_SESSION_LOSS_PENDING, new CallResponseCode(XR_SESSION_LOSS_PENDING, "XR_SESSION_LOSS_PENDING", "The session will be lost soon."));
        responseCodes.put(XR_EVENT_UNAVAILABLE, new CallResponseCode(XR_EVENT_UNAVAILABLE, "XR_EVENT_UNAVAILABLE", "No event was available."));
        responseCodes.put(XR_SPACE_BOUNDS_UNAVAILABLE, new CallResponseCode(XR_SPACE_BOUNDS_UNAVAILABLE, "XR_SPACE_BOUNDS_UNAVAILABLE", "The space’s bounds are not known at the moment"));
        responseCodes.put(XR_SESSION_NOT_FOCUSED, new CallResponseCode(XR_SESSION_NOT_FOCUSED, "XR_SESSION_NOT_FOCUSED", "The session is not in the focused state"));
        responseCodes.put(XR_FRAME_DISCARDED, new CallResponseCode(XR_FRAME_DISCARDED, "XR_FRAME_DISCARDED", "A frame has been discarded from composition"));
        responseCodes.put(XR_ERROR_VALIDATION_FAILURE, new CallResponseCode(XR_ERROR_VALIDATION_FAILURE, "XR_ERROR_VALIDATION_FAILURE", "The function usage was invalid in some way."));
        responseCodes.put(XR_ERROR_RUNTIME_FAILURE, new CallResponseCode(XR_ERROR_RUNTIME_FAILURE, "XR_ERROR_RUNTIME_FAILURE", "The runtime failed to handle the function in an unexpected way that is not covered by another error result."));
        responseCodes.put(XR_ERROR_OUT_OF_MEMORY, new CallResponseCode(XR_ERROR_OUT_OF_MEMORY, "XR_ERROR_OUT_OF_MEMORY", "A memory allocation has failed."));
        responseCodes.put(XR_ERROR_API_VERSION_UNSUPPORTED, new CallResponseCode(XR_ERROR_API_VERSION_UNSUPPORTED, "XR_ERROR_API_VERSION_UNSUPPORTED", "The runtime does not support the requested API version."));
        responseCodes.put(XR_ERROR_INITIALIZATION_FAILED, new CallResponseCode(XR_ERROR_INITIALIZATION_FAILED, "XR_ERROR_INITIALIZATION_FAILED", "Initialization of object could not be completed."));
        responseCodes.put(XR_ERROR_FUNCTION_UNSUPPORTED, new CallResponseCode(XR_ERROR_FUNCTION_UNSUPPORTED, "XR_ERROR_FUNCTION_UNSUPPORTED", "The requested function was not found or is otherwise unsupported."));
        responseCodes.put(XR_ERROR_FEATURE_UNSUPPORTED, new CallResponseCode(XR_ERROR_FEATURE_UNSUPPORTED, "XR_ERROR_FEATURE_UNSUPPORTED", "The requested feature is not supported."));
        responseCodes.put(XR_ERROR_EXTENSION_NOT_PRESENT, new CallResponseCode(XR_ERROR_EXTENSION_NOT_PRESENT, "XR_ERROR_EXTENSION_NOT_PRESENT", "A requested extension is not supported."));
        responseCodes.put(XR_ERROR_LIMIT_REACHED, new CallResponseCode(XR_ERROR_LIMIT_REACHED, "XR_ERROR_LIMIT_REACHED", "The runtime supports no more of the requested resource"));
        responseCodes.put(XR_ERROR_SIZE_INSUFFICIENT, new CallResponseCode(XR_ERROR_SIZE_INSUFFICIENT, "XR_ERROR_SIZE_INSUFFICIENT", "The supplied size was smaller than required."));
        responseCodes.put(XR_ERROR_HANDLE_INVALID, new CallResponseCode(XR_ERROR_HANDLE_INVALID, "XR_ERROR_HANDLE_INVALID", "A supplied object handle was invalid."));
        responseCodes.put(XR_ERROR_INSTANCE_LOST, new CallResponseCode(XR_ERROR_INSTANCE_LOST, "XR_ERROR_INSTANCE_LOST", "The XrInstance was lost or could not be found. It will need to be destroyed and optionally recreated."));
        responseCodes.put(XR_ERROR_SESSION_RUNNING, new CallResponseCode(XR_ERROR_SESSION_RUNNING, "XR_ERROR_SESSION_RUNNING", "The session is already running. See https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html#session_running"));
        responseCodes.put(XR_ERROR_SESSION_NOT_RUNNING, new CallResponseCode(XR_ERROR_SESSION_NOT_RUNNING, "XR_ERROR_SESSION_NOT_RUNNING", "The session is not yet running. See https://www.khronos.org/registry/OpenXR/specs/1.0/html/xrspec.html#session_not_running"));
        responseCodes.put(XR_ERROR_SESSION_LOST, new CallResponseCode(XR_ERROR_SESSION_LOST, "XR_ERROR_SESSION_LOST", "The XrSession was lost. It will need to be destroyed and optionally recreated."));
        responseCodes.put(XR_ERROR_SYSTEM_INVALID, new CallResponseCode(XR_ERROR_SYSTEM_INVALID, "XR_ERROR_SYSTEM_INVALID", "The provided XrSystemId was invalid"));
        responseCodes.put(XR_ERROR_PATH_INVALID, new CallResponseCode(XR_ERROR_PATH_INVALID, "XR_ERROR_PATH_INVALID", "The provided XrPath was not valid."));
        responseCodes.put(XR_ERROR_PATH_COUNT_EXCEEDED, new CallResponseCode(XR_ERROR_PATH_COUNT_EXCEEDED, "XR_ERROR_PATH_COUNT_EXCEEDED", "The maximum number of supported semantic paths has been reached."));
        responseCodes.put(XR_ERROR_PATH_FORMAT_INVALID, new CallResponseCode(XR_ERROR_PATH_FORMAT_INVALID, "XR_ERROR_PATH_FORMAT_INVALID", "The semantic path character format is invalid"));
        responseCodes.put(XR_ERROR_PATH_UNSUPPORTED, new CallResponseCode(XR_ERROR_PATH_UNSUPPORTED, "XR_ERROR_PATH_UNSUPPORTED", "The semantic path is unsupported"));
        responseCodes.put(XR_ERROR_LAYER_INVALID, new CallResponseCode(XR_ERROR_LAYER_INVALID, "XR_ERROR_LAYER_INVALID", "The layer was NULL or otherwise invalid."));
        responseCodes.put(XR_ERROR_LAYER_LIMIT_EXCEEDED, new CallResponseCode(XR_ERROR_LAYER_LIMIT_EXCEEDED, "XR_ERROR_LAYER_LIMIT_EXCEEDED", "The number of specified layers is greater than the supported number"));
        responseCodes.put(XR_ERROR_SWAPCHAIN_RECT_INVALID, new CallResponseCode(XR_ERROR_SWAPCHAIN_RECT_INVALID, "XR_ERROR_SWAPCHAIN_RECT_INVALID", "The image rect was negatively sized or otherwise invalid"));
        responseCodes.put(XR_ERROR_SWAPCHAIN_FORMAT_UNSUPPORTED, new CallResponseCode(XR_ERROR_SWAPCHAIN_FORMAT_UNSUPPORTED, "XR_ERROR_SWAPCHAIN_FORMAT_UNSUPPORTED", "The image format is not supported by the runtime or platform."));
        responseCodes.put(XR_ERROR_ACTION_TYPE_MISMATCH, new CallResponseCode(XR_ERROR_ACTION_TYPE_MISMATCH, "XR_ERROR_ACTION_TYPE_MISMATCH", "The API used to retrieve an action’s state does not match the action’s type"));
        responseCodes.put(XR_ERROR_SESSION_NOT_READY, new CallResponseCode(XR_ERROR_SESSION_NOT_READY, "XR_ERROR_SESSION_NOT_READY", "The session is not in the ready state"));
        responseCodes.put(XR_ERROR_SESSION_NOT_STOPPING, new CallResponseCode(XR_ERROR_SESSION_NOT_STOPPING, "XR_ERROR_SESSION_NOT_STOPPING", "The session is not in the stopping state"));
        responseCodes.put(XR_ERROR_TIME_INVALID, new CallResponseCode(XR_ERROR_TIME_INVALID, "XR_ERROR_TIME_INVALID", "The provided XrTime was zero, negative, or out of range."));
        responseCodes.put(XR_ERROR_REFERENCE_SPACE_UNSUPPORTED, new CallResponseCode(XR_ERROR_REFERENCE_SPACE_UNSUPPORTED, "XR_ERROR_REFERENCE_SPACE_UNSUPPORTED", "The specified reference space is not supported by the runtime or system"));
        responseCodes.put(XR_ERROR_FILE_ACCESS_ERROR, new CallResponseCode(XR_ERROR_FILE_ACCESS_ERROR, "XR_ERROR_FILE_ACCESS_ERROR", "The file could not be accessed."));
        responseCodes.put(XR_ERROR_FILE_CONTENTS_INVALID, new CallResponseCode(XR_ERROR_FILE_CONTENTS_INVALID, "XR_ERROR_FILE_CONTENTS_INVALID", "The file’s contents were invalid."));
        responseCodes.put(XR_ERROR_FORM_FACTOR_UNSUPPORTED, new CallResponseCode(XR_ERROR_FORM_FACTOR_UNSUPPORTED, "XR_ERROR_FORM_FACTOR_UNSUPPORTED", "The specified form factor is not supported by the current runtime or platform."));
        responseCodes.put(XR_ERROR_FORM_FACTOR_UNAVAILABLE, new CallResponseCode(XR_ERROR_FORM_FACTOR_UNAVAILABLE, "XR_ERROR_FORM_FACTOR_UNAVAILABLE", "The specified form factor is supported, but the device is currently not available, e.g. not plugged in or powered off."));
        responseCodes.put(XR_ERROR_API_LAYER_NOT_PRESENT, new CallResponseCode(XR_ERROR_API_LAYER_NOT_PRESENT, "XR_ERROR_API_LAYER_NOT_PRESENT", "A requested API layer is not present or could not be loaded."));
        responseCodes.put(XR_ERROR_CALL_ORDER_INVALID, new CallResponseCode(XR_ERROR_CALL_ORDER_INVALID, "XR_ERROR_CALL_ORDER_INVALID", "The call was made without having made a previously required call."));
        responseCodes.put(XR_ERROR_GRAPHICS_DEVICE_INVALID, new CallResponseCode(XR_ERROR_GRAPHICS_DEVICE_INVALID, "XR_ERROR_GRAPHICS_DEVICE_INVALID", "The given graphics device is not in a valid state. The graphics device could be lost or initialized without meeting graphics requirements"));
        responseCodes.put(XR_ERROR_POSE_INVALID, new CallResponseCode(XR_ERROR_POSE_INVALID, "XR_ERROR_POSE_INVALID", "The supplied pose was invalid with respect to the requirements."));
        responseCodes.put(XR_ERROR_INDEX_OUT_OF_RANGE, new CallResponseCode(XR_ERROR_INDEX_OUT_OF_RANGE, "XR_ERROR_INDEX_OUT_OF_RANGE", "The supplied index was outside the range of valid indices."));
        responseCodes.put(XR_ERROR_VIEW_CONFIGURATION_TYPE_UNSUPPORTED, new CallResponseCode(XR_ERROR_VIEW_CONFIGURATION_TYPE_UNSUPPORTED, "XR_ERROR_VIEW_CONFIGURATION_TYPE_UNSUPPORTED", "The specified view configuration type is not supported by the runtime or platform"));
        responseCodes.put(XR_ERROR_ENVIRONMENT_BLEND_MODE_UNSUPPORTED, new CallResponseCode(XR_ERROR_ENVIRONMENT_BLEND_MODE_UNSUPPORTED, "XR_ERROR_ENVIRONMENT_BLEND_MODE_UNSUPPORTED", "The specified environment blend mode is not supported by the runtime or platform."));
        responseCodes.put(XR_ERROR_NAME_DUPLICATED, new CallResponseCode(XR_ERROR_NAME_DUPLICATED, "XR_ERROR_NAME_DUPLICATED", "The name provided was a duplicate of an already-existing resource."));
        responseCodes.put(XR_ERROR_NAME_INVALID, new CallResponseCode(XR_ERROR_NAME_INVALID, "XR_ERROR_NAME_INVALID", "The name provided was invalid"));
        responseCodes.put(XR_ERROR_ACTIONSET_NOT_ATTACHED, new CallResponseCode(XR_ERROR_ACTIONSET_NOT_ATTACHED, "XR_ERROR_ACTIONSET_NOT_ATTACHED", "A referenced action set is not attached to the session"));
        responseCodes.put(XR_ERROR_ACTIONSETS_ALREADY_ATTACHED, new CallResponseCode(XR_ERROR_ACTIONSETS_ALREADY_ATTACHED, "XR_ERROR_ACTIONSETS_ALREADY_ATTACHED", "The session already has attached action sets."));
        responseCodes.put(XR_ERROR_LOCALIZED_NAME_DUPLICATED, new CallResponseCode(XR_ERROR_LOCALIZED_NAME_DUPLICATED, "XR_ERROR_LOCALIZED_NAME_DUPLICATED", "The localized name provided was a duplicate of an already-existing resource"));
        responseCodes.put(XR_ERROR_LOCALIZED_NAME_INVALID, new CallResponseCode(XR_ERROR_LOCALIZED_NAME_INVALID, "XR_ERROR_LOCALIZED_NAME_INVALID", "The localized name provided was invalid."));
        responseCodes.put(XR_ERROR_GRAPHICS_REQUIREMENTS_CALL_MISSING, new CallResponseCode(XR_ERROR_GRAPHICS_REQUIREMENTS_CALL_MISSING, "XR_ERROR_GRAPHICS_REQUIREMENTS_CALL_MISSING", "The xrGetGraphicsRequirements call was not made before calling xrCreateSession"));
        responseCodes.put(XR_ERROR_RUNTIME_UNAVAILABLE, new CallResponseCode(XR_ERROR_RUNTIME_UNAVAILABLE, "XR_ERROR_RUNTIME_UNAVAILABLE", "The loader was unable to find or load a runtime."));
    }

    private final int errorCode;
    private final String errorTextId;
    private final String errorMessage;
    private final boolean isAnErrorCondition;

    private final String formattedErrorMessage;

    public static Optional<CallResponseCode> getResponseCode(int errorCode){
        CallResponseCode responseCode = responseCodes.get(errorCode);
        if (responseCode!=null){
            return Optional.of(responseCode);
        }else {
            return Optional.empty();
        }
    }

    public CallResponseCode(int errorCode, String errorTextId, String errorMessage){
        this.errorCode = errorCode;
        this.errorTextId = errorTextId;
        this.errorMessage = errorMessage;
        this.isAnErrorCondition = errorCode < 0;
        this.formattedErrorMessage = errorTextId + "(" + errorCode + "): " + errorMessage;
    }

    public String getFullFormattedErrorMessage(){
        return formattedErrorMessage;
    }

    public boolean isAnErrorCondition(){
        return isAnErrorCondition;
    }

    public int getErrorCode(){
        return errorCode;
    }

    public String getErrorTextId(){
        return errorTextId;
    }

    public String getErrorMessage(){
        return errorMessage;
    }
}
