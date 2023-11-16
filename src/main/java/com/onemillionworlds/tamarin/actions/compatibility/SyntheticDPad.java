package com.onemillionworlds.tamarin.actions.compatibility;

import com.jme3.math.Vector2f;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;

import java.util.Optional;

/**
 * <p>
 * It is possible to use real DPad functionality but that requires the XR_EXT_dpad_binding extension which
 * may not be available on older runtimes (including at least the rift and early HTC vive).
 * </p>
 * <p>
 * Real DPad functionality is certainly better than this synthetic version allowing users to redefine action by action
 * but the synthetic dpad is a good fallback for older runtimes (but only the joystick as a whole can be redefined).
 * </p>
 * <p>
 * Instantiate a SyntheticDPad once then keep reusing it to correctly get hasChanged values
 */
public class SyntheticDPad{
    Optional<Direction> lastUpdateDirection = Optional.empty();
    Optional<Direction> thisUpdateDirection = Optional.empty();

    double minToActivate = 0.5;
    double minToDeactivate = 0.4;

    public SyntheticDPad(){
    }

    public void updateRawAction(Vector2fActionState rawState){
        lastUpdateDirection = thisUpdateDirection;

        boolean shouldActivate = rawState.getState().lengthSquared()>minToActivate*minToActivate;
        boolean shouldDeactivate = rawState.getState().lengthSquared()<minToDeactivate*minToDeactivate;
        if (shouldDeactivate){
            thisUpdateDirection = Optional.empty();
        }else if (shouldActivate){
            thisUpdateDirection = Optional.of(getPrimaryDirection(rawState.getState()));
        }
    }

    public BooleanActionState north(){
        return dPadValue(Direction.NORTH);
    }

    public BooleanActionState south(){
        return dPadValue(Direction.SOUTH);
    }

    public BooleanActionState east(){
        return dPadValue(Direction.EAST);
    }

    public BooleanActionState west(){
        return dPadValue(Direction.WEST);
    }

    public BooleanActionState dPadValue(Direction direction){
        boolean eitherConcernsThisDirection = lastUpdateDirection.map(d->d==direction).orElse(false) || thisUpdateDirection.map(d->d==direction).orElse(false);
        boolean changed = eitherConcernsThisDirection && !lastUpdateDirection.equals(thisUpdateDirection);

        return new BooleanActionState(thisUpdateDirection.map(d->d==direction).orElse(false), changed);
    }

    private static Direction getPrimaryDirection(Vector2f rawDirection) {
        float x = rawDirection.getX();
        float y = rawDirection.getY();

        // Compare the absolute values to find the primary direction
        if (Math.abs(x) > Math.abs(y)) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return y > 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }

    public enum Direction{
        NORTH,SOUTH,EAST,WEST
    }
}
