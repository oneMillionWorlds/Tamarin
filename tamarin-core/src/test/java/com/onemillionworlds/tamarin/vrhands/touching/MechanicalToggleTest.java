package com.onemillionworlds.tamarin.vrhands.touching;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.onemillionworlds.tamarin.observable.ObservableValueSubscription;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MechanicalToggleTest{

    private static final float MAXIMUM_TRAVEL = 0.1f;
    private static final float TOGGLE_IN_TRAVEL = 0.05f;
    private static final float RESET_TIME = 0.5f;

    @Test
    void silentTransitionToOffCompletesSilently(){
        Geometry geometry = new Geometry("button", new Box(0.1f, 0.1f, 0.1f));
        MechanicalToggle toggle = new MechanicalToggle(geometry, ButtonMovementAxis.NEGATIVE_Z, MAXIMUM_TRAVEL, TOGGLE_IN_TRAVEL, RESET_TIME);
        toggle.setState(MechanicalToggle.ToggleState.TOGGLED_ON);

        List<MechanicalToggle.ToggleState> pressEvents = new ArrayList<>();
        List<Boolean> onOffEvents = new ArrayList<>();
        toggle.addPressListener(pressEvents::add);
        toggle.addOnOffListener(onOffEvents::add);

        toggle.setState(MechanicalToggle.ToggleState.TRANSITIONING_OFF, false);
        relaxFully(geometry);

        assertEquals(MechanicalToggle.ToggleState.FULLY_OFF, toggle.getCurrentState());
        assertEquals(0, toggle.getCurrentTravel());
        assertEquals(List.of(), pressEvents);
        assertEquals(List.of(), onOffEvents);
    }

    @Test
    void notifiedTransitionToOffNotifiesOnCompletion(){
        Geometry geometry = new Geometry("button", new Box(0.1f, 0.1f, 0.1f));
        MechanicalToggle toggle = new MechanicalToggle(geometry, ButtonMovementAxis.NEGATIVE_Z, MAXIMUM_TRAVEL, TOGGLE_IN_TRAVEL, RESET_TIME);
        toggle.setState(MechanicalToggle.ToggleState.TOGGLED_ON);

        List<MechanicalToggle.ToggleState> pressEvents = new ArrayList<>();
        List<Boolean> onOffEvents = new ArrayList<>();
        toggle.addPressListener(pressEvents::add);
        toggle.addOnOffListener(onOffEvents::add);

        toggle.setState(MechanicalToggle.ToggleState.TRANSITIONING_OFF);
        relaxFully(geometry);

        assertEquals(List.of(MechanicalToggle.ToggleState.TRANSITIONING_OFF, MechanicalToggle.ToggleState.FULLY_OFF), pressEvents);
        assertEquals(List.of(false), onOffEvents, "Relaxing to fully off is not a second on/off change");
    }

    @Test
    void silentSetKeepsSubscriptionValuesCurrentWithoutReportingAChange(){
        Geometry geometry = new Geometry("button", new Box(0.1f, 0.1f, 0.1f));
        MechanicalToggle toggle = new MechanicalToggle(geometry, ButtonMovementAxis.NEGATIVE_Z, MAXIMUM_TRAVEL, TOGGLE_IN_TRAVEL, RESET_TIME);
        ObservableValueSubscription<MechanicalToggle.ToggleState> pressSubscription = toggle.subscribeToPressEvents();
        ObservableValueSubscription<Boolean> onOffSubscription = toggle.subscribeToOnOffEvents();

        toggle.setState(MechanicalToggle.ToggleState.TOGGLED_ON, false);

        assertFalse(pressSubscription.checkHasChanged());
        assertFalse(onOffSubscription.checkHasChanged());
        assertEquals(MechanicalToggle.ToggleState.TOGGLED_ON, pressSubscription.get());
        assertTrue(onOffSubscription.get());
    }

    @Test
    void physicalPressAfterSilentOffNotifies(){
        Geometry geometry = new Geometry("button", new Box(0.1f, 0.1f, 0.1f));
        MechanicalToggle toggle = new MechanicalToggle(geometry, ButtonMovementAxis.NEGATIVE_Z, MAXIMUM_TRAVEL, TOGGLE_IN_TRAVEL, RESET_TIME);
        toggle.setState(MechanicalToggle.ToggleState.TOGGLED_ON);
        toggle.setState(MechanicalToggle.ToggleState.TRANSITIONING_OFF, false);
        relaxFully(geometry);

        List<MechanicalToggle.ToggleState> pressEvents = new ArrayList<>();
        List<Boolean> onOffEvents = new ArrayList<>();
        toggle.addPressListener(pressEvents::add);
        toggle.addOnOffListener(onOffEvents::add);

        pressWithFinger(geometry);
        relaxFully(geometry);

        assertEquals(MechanicalToggle.ToggleState.TOGGLED_ON, toggle.getCurrentState());
        assertEquals(List.of(MechanicalToggle.ToggleState.TRANSITIONING_ON, MechanicalToggle.ToggleState.TOGGLED_ON), pressEvents);
        assertEquals(List.of(true), onOffEvents);
    }

    @Test
    void physicalPressAfterSilentOnNotifies(){
        Geometry geometry = new Geometry("button", new Box(0.1f, 0.1f, 0.1f));
        MechanicalToggle toggle = new MechanicalToggle(geometry, ButtonMovementAxis.NEGATIVE_Z, MAXIMUM_TRAVEL, TOGGLE_IN_TRAVEL, RESET_TIME);
        toggle.setState(MechanicalToggle.ToggleState.TOGGLED_ON, false);

        List<MechanicalToggle.ToggleState> pressEvents = new ArrayList<>();
        List<Boolean> onOffEvents = new ArrayList<>();
        toggle.addPressListener(pressEvents::add);
        toggle.addOnOffListener(onOffEvents::add);

        pressWithFinger(geometry);
        relaxFully(geometry);

        assertEquals(MechanicalToggle.ToggleState.FULLY_OFF, toggle.getCurrentState());
        assertEquals(List.of(MechanicalToggle.ToggleState.TRANSITIONING_OFF, MechanicalToggle.ToggleState.FULLY_OFF), pressEvents);
        assertEquals(List.of(false), onOffEvents);
    }

    /** Pushes the button to its maximum travel with a (mock) fingertip, then withdraws the finger. */
    private static void pressWithFinger(Geometry geometry){
        AbstractTouchControl touchControl = geometry.getControl(AbstractTouchControl.class);

        Node fingerTip = new Node("fingerTip");
        fingerTip.setLocalTranslation(0, 0, -10);
        fingerTip.updateGeometricState();
        BoundHand hand = mock(BoundHand.class);
        when(hand.getIndexFingerTip_xPointing()).thenReturn(fingerTip);

        touchControl.onTouch(hand);
        touchControl.update(1 / 60f);
        touchControl.onStopTouch(hand);
    }

    private static void relaxFully(Geometry geometry){
        AbstractTouchControl touchControl = geometry.getControl(AbstractTouchControl.class);
        for(int i = 0; i < 120; i++){
            touchControl.update(1 / 60f);
        }
    }
}
