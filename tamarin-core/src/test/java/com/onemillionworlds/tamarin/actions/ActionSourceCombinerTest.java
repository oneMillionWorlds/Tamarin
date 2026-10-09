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
    private static final ActionSourceState INACTIVE = ActionSourceState.INACTIVE;

    @Test
    void automaticAndRuntimeOnlyUseAnActiveRuntime(){
        for(HandGestureSourcePreference preference : List.of(AUTOMATIC, RUNTIME_ONLY)){
            assertFalse(ActionSourceCombiner.combineBoolean(OFF, ON, preference).getState(), preference.name());
            assertTrue(ActionSourceCombiner.combineBoolean(ON, OFF, preference).getState(), preference.name());
        }
    }

    @Test
    void automaticAndRuntimeOnlyUseTheGestureIfTheRuntimeIsInactive(){
        for(HandGestureSourcePreference preference : List.of(AUTOMATIC, RUNTIME_ONLY)){
            BooleanActionState state = ActionSourceCombiner.combineBoolean(INACTIVE, JUST_ON, preference);
            assertTrue(state.getState(), preference.name());
            assertTrue(state.hasChanged(), preference.name());
        }
    }

    @Test
    void aSlightlyDifferentGestureDoesNotAffectAnActiveRuntime(){
        // the runtime and Tamarin disagree slightly about when the pinch started, only the runtime's edge is reported
        BooleanActionState state = ActionSourceCombiner.combineBoolean(ON, JUST_ON, AUTOMATIC);
        assertTrue(state.getState());
        assertFalse(state.hasChanged());
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
    void floatPrecedenceUsesOneSource(){
        ActionSourceState runtime = new ActionSourceState(0.2f, false, true);
        ActionSourceState gesture = new ActionSourceState(0.6f, true, true);
        assertEquals(0.2f, ActionSourceCombiner.combineFloat(runtime, gesture, AUTOMATIC).getState());
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
