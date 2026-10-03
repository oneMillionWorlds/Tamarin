package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.observable.ObservableDataEventSubscription;
import com.onemillionworlds.tamarin.observable.ObservableEventSubscription;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InteractionProfileTrackerTest{

    private static final String TOUCH = "/interaction_profiles/oculus/touch_controller";
    private static final String HANDS = "/interaction_profiles/ext/hand_interaction_ext";

    private static final Map<HandSide, String> BOTH = Map.of(HandSide.LEFT, TOUCH, HandSide.RIGHT, TOUCH);
    private static final Map<HandSide, String> LEFT_ONLY = Map.of(HandSide.LEFT, TOUCH);
    private static final Map<HandSide, String> RIGHT_ONLY = Map.of(HandSide.RIGHT, TOUCH);
    private static final Map<HandSide, String> NONE = Map.of();

    @Test
    void startingWithControllersIsNotARegain(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();
        ObservableDataEventSubscription<HandSide> regained = tracker.controllerRegained.subscribe();
        ObservableEventSubscription changed = tracker.profileChanged.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.tick(false, 10);

        assertTrue(changed.checkHasChanged());
        assertEquals(List.of(), lost.pollEvents());
        assertEquals(List.of(), regained.pollEvents());
        assertEquals(TOUCH, tracker.getCurrentProfile(HandSide.LEFT).orElseThrow());
    }

    @Test
    void controllerIsOnlyLostAfterTheGracePeriod(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        double gracePeriod = InteractionProfileTracker.DEFAULT_LOSS_GRACE_PERIOD;
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(RIGHT_ONLY, false, 10);
        assertTrue(tracker.getCurrentProfile(HandSide.LEFT).isEmpty(), "The profile itself should update immediately");

        tracker.tick(false, 10 + gracePeriod - 0.01);
        assertEquals(List.of(), lost.pollEvents());

        tracker.tick(false, 10 + gracePeriod);
        assertEquals(List.of(HandSide.LEFT), lost.pollEvents());

        tracker.tick(false, 20);
        assertEquals(List.of(), lost.pollEvents(), "Events should only be reported once");
    }

    @Test
    void lostControllerReturningIsRegained(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> regained = tracker.controllerRegained.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(RIGHT_ONLY, false, 10);
        tracker.tick(false, 13);
        tracker.update(BOTH, false, 20);

        assertEquals(List.of(HandSide.LEFT), regained.pollEvents());
    }

    /**
     * Both controllers vanish, then the left one returns shortly afterwards (ticking every frame, as in real use)
     */
    @Test
    void controllerReturningWithinTheGracePeriodIsNotReported(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        tracker.setLossGracePeriod(1);
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();
        ObservableDataEventSubscription<HandSide> regained = tracker.controllerRegained.subscribe();

        tracker.update(BOTH, false, 0);
        double time = 10;
        tracker.update(NONE, false, time);
        for(; time < 10.9; time += 0.011){
            tracker.tick(false, time);
        }
        tracker.update(LEFT_ONLY, false, time);
        for(; time < 12; time += 0.011){
            tracker.tick(false, time);
        }

        assertEquals(List.of(HandSide.RIGHT), lost.pollEvents());
        assertEquals(List.of(), regained.pollEvents(), "Returning within the grace period isn't a regain");
    }

    @Test
    void bothControllersLostAtOnceGivesTwoEvents(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(NONE, false, 1);
        tracker.tick(false, 5);

        assertEquals(List.of(HandSide.LEFT, HandSide.RIGHT), lost.pollEvents());
    }

    @Test
    void handThatNeverHadAControllerIsNeverLost(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();
        ObservableDataEventSubscription<HandSide> regained = tracker.controllerRegained.subscribe();

        tracker.update(RIGHT_ONLY, false, 0);
        tracker.update(BOTH, false, 1);
        tracker.tick(false, 10);

        assertEquals(List.of(), lost.pollEvents());
        assertEquals(List.of(), regained.pollEvents(), "First appearance of a controller isn't a regain");
    }

    @Test
    void switchingToHandTrackingIsNotALoss(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();
        ObservableEventSubscription changed = tracker.profileChanged.subscribe();

        tracker.update(BOTH, false, 0);
        changed.checkHasChanged();
        tracker.update(Map.of(HandSide.LEFT, HANDS, HandSide.RIGHT, HANDS), false, 1);
        tracker.tick(false, 10);

        assertTrue(changed.checkHasChanged());
        assertEquals(List.of(), lost.pollEvents());
    }

    @Test
    void lossWhileAlreadyPausedIsSuppressedAndSoIsTheReturn(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();
        ObservableDataEventSubscription<HandSide> regained = tracker.controllerRegained.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(NONE, true, 1); // headset taken off, controllers sleep
        tracker.tick(true, 10);
        tracker.update(BOTH, false, 20); // headset back on, controllers wake
        tracker.tick(false, 30);

        assertEquals(List.of(), lost.pollEvents());
        assertEquals(List.of(), regained.pollEvents());
    }

    @Test
    void pausingDuringTheGracePeriodAbandonsTheLoss(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(NONE, false, 1); // controllers sleep slightly before the headset reports being taken off
        tracker.tick(true, 1.5);
        tracker.tick(false, 10);

        assertEquals(List.of(), lost.pollEvents());
    }

    @Test
    void gracePeriodIsConfigurable(){
        InteractionProfileTracker tracker = new InteractionProfileTracker();
        tracker.setLossGracePeriod(0.5);
        ObservableDataEventSubscription<HandSide> lost = tracker.controllerLost.subscribe();

        tracker.update(BOTH, false, 0);
        tracker.update(RIGHT_ONLY, false, 1);
        tracker.tick(false, 1.5);

        assertEquals(List.of(HandSide.LEFT), lost.pollEvents());
    }
}
