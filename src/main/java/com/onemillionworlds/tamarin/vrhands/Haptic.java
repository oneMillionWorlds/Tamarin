package com.onemillionworlds.tamarin.vrhands;

import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;

/**
 * @param actionHandle the handle for the action (just an object with the set name and action name)
 * @param duration how long in seconds the
 * @param frequency in cycles per second
 * @param amplitude between 0 and 1
 */
public record Haptic(ActionHandle actionHandle, float duration, float frequency, float amplitude){
}
