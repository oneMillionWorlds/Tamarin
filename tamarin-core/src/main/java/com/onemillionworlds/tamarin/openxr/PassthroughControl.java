package com.onemillionworlds.tamarin.openxr;

import java.util.Set;

/**
 * Implemented by the (desktop and android) session managers to provide passthrough. Applications should use the
 * methods on {@link XrBaseAppState} instead.
 */
public interface PassthroughControl{

    /**
     * @return how passthrough is provided, {@link PassthroughMethod#NONE} if it isn't available
     */
    PassthroughMethod getPassthroughMethod();

    /**
     * @return a human-readable reason passthrough isn't available (or an empty string if it is)
     */
    String getPassthroughUnavailableReason();

    boolean isPassthroughEnabled();

    /**
     * @throws IllegalStateException if enabling and passthrough isn't available, or if the runtime refuses
     */
    void setPassthroughEnabled(boolean enabled);

    /**
     * @return the environment blend modes the runtime supports
     */
    Set<XrVrMode> getSupportedXrVrModes();
}
