package com.onemillionworlds.tamarin.actions.state;

import com.jme3.math.Vector2f;
import lombok.Getter;

@Getter
public class Vector2fActionState{

    /**
     * The current state of the action, will be between (-1,-1) and (1, 1)
     */
    private final Vector2f state;

    /**
     * The X coordinate of the analog data (typically between -1 and 1 for joystick coordinates or 0 and 1 for
     * trigger pulls)
     */
    private final float x;

    /**
     * The Y coordinate of the analog data (typically between -1 and 1)
     * <p>
     * Will be zero if the analog action doesn't have at least 2 dimensions
     */
    private final float y;

    /**
     * If this action has changed since the last update
     */
    private final boolean changed;

    public Vector2fActionState(float x, float y,  boolean changed){
        this.x = x;
        this.y = y;
        this.state = new Vector2f(x, y);
        this.changed = changed;
    }
}
