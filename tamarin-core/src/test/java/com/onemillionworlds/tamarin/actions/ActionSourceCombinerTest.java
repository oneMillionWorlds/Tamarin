package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference.AUTOMATIC;
import static com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference.RUNTIME_ONLY;
import static com.onemillionworlds.tamarin.handgestures.HandGestureSourcePreference.TAMARIN_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionSourceCombinerTest{

    private static final ActionSourceState ON = ActionSourceState.ofBoolean(true, false, true);
    private static final ActionSourceState JUST_ON = ActionSourceState.ofBoolean(true, true, true);
    private static final ActionSourceState OFF = ActionSourceState.ofBoolean(false, false, true);
    private static final ActionSourceState JUST_OFF = ActionSourceState.ofBoolean(false, true, true);
    private static final ActionSourceState INACTIVE = ActionSourceState.INACTIVE;

    @Test
    void automaticIsOnIfEitherIsOn(){
        assertTrue(ActionSourceCombiner.combineBoolean(ON, OFF, AUTOMATIC).getState());
        assertTrue(ActionSourceCombiner.combineBoolean(OFF, ON, AUTOMATIC).getState());
        assertFalse(ActionSourceCombiner.combineBoolean(OFF, OFF, AUTOMATIC).getState());
    }

    @Test
    void automaticOnlyReportsAChangeIfTheCombinedValueChanged(){
        // the runtime turns on while the gesture is already on
        BooleanActionState state = ActionSourceCombiner.combineBoolean(JUST_ON, ON, AUTOMATIC);
        assertTrue(state.getState());
        assertFalse(state.hasChanged());

        // the gesture turns off while the runtime is still on
        assertFalse(ActionSourceCombiner.combineBoolean(ON, JUST_OFF, AUTOMATIC).hasChanged());

        // the gesture turns on while the runtime is off
        assertTrue(ActionSourceCombiner.combineBoolean(OFF, JUST_ON, AUTOMATIC).hasChangedToTrue());
    }

    @Test
    void runtimeOnlyUsesAnActiveRuntime(){
        assertFalse(ActionSourceCombiner.combineBoolean(OFF, ON, RUNTIME_ONLY).getState());
    }

    @Test
    void runtimeOnlyUsesTheGestureIfTheRuntimeIsInactive(){
        assertTrue(ActionSourceCombiner.combineBoolean(INACTIVE, ON, RUNTIME_ONLY).getState());
    }

    @Test
    void tamarinOnlyUsesAnActiveGesture(){
        assertFalse(ActionSourceCombiner.combineBoolean(ON, OFF, TAMARIN_ONLY).getState());
    }

    @Test
    void tamarinOnlyUsesTheRuntimeIfTheGestureIsInactive(){
        // e.g. holding a controller
        assertTrue(ActionSourceCombiner.combineBoolean(ON, INACTIVE, TAMARIN_ONLY).getState());
    }

    @Test
    void anInactiveGestureLeavesTheRuntimeUnchangedWhateverThePreference(){
        for(HandGestureSourcePreference preference : HandGestureSourcePreference.values()){
            BooleanActionState state = ActionSourceCombiner.combineBoolean(JUST_ON, INACTIVE, preference);
            assertTrue(state.getState(), preference.name());
            assertTrue(state.hasChanged(), preference.name());
        }
    }

    @Test
    void floatAutomaticTakesTheLargerValue(){
        FloatActionState state = ActionSourceCombiner.combineFloat(new ActionSourceState(0.2f, false, true), new ActionSourceState(0.6f, true, true), AUTOMATIC);
        assertEquals(0.6f, state.getState());
        assertTrue(state.hasChanged());
    }

    @Test
    void floatPrecedenceUsesOneSource(){
        ActionSourceState runtime = new ActionSourceState(0.2f, false, true);
        ActionSourceState gesture = new ActionSourceState(0.6f, true, true);
        assertEquals(0.2f, ActionSourceCombiner.combineFloat(runtime, gesture, RUNTIME_ONLY).getState());
        assertEquals(0.6f, ActionSourceCombiner.combineFloat(runtime, gesture, TAMARIN_ONLY).getState());
    }

    @Test
    void anyOfIsOnIfAnyHandIsOnAndOnlyChangesIfTheCombinedValueDoes(){
        BooleanActionState state = ActionSourceCombiner.anyOf(List.of(new BooleanActionState(true, false), new BooleanActionState(true, true)));
        assertTrue(state.getState());
        assertFalse(state.hasChanged(), "the other hand was already on");

        assertTrue(ActionSourceCombiner.anyOf(List.of(new BooleanActionState(false, false), new BooleanActionState(true, true))).hasChangedToTrue());
        assertTrue(ActionSourceCombiner.anyOf(List.of(new BooleanActionState(false, false), new BooleanActionState(false, true))).hasChangedToFalse());
    }

    @Test
    void largestOfTakesTheLargestValue(){
        FloatActionState state = ActionSourceCombiner.largestOf(List.of(new FloatActionState(0.3f, false), new FloatActionState(0.7f, true)));
        assertEquals(0.7f, state.getState());
        assertTrue(state.hasChanged());
    }
}
