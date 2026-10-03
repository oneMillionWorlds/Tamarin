package com.onemillionworlds.tamarin.actions;

import com.onemillionworlds.tamarin.observable.ObservableDataEvent;
import com.onemillionworlds.tamarin.observable.ObservableEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Tracks the interaction profile in use for each hand and derives controller lost/regained events from it.
 * <p>
 *     A controller is "lost" when a hand that had an interaction profile no longer has one (e.g. the controller's
 *     battery died or it was turned off) and doesn't get it back within the grace period. A hand that never had a
 *     profile (e.g. the user started with only one controller) is never reported as lost. A hand changing from one
 *     profile to another (e.g. from a controller to hand tracking) is not a loss.
 * </p>
 * <p>
 *     The grace period exists because runtimes briefly drop profiles. E.g. on the Quest when one controller's
 *     battery is removed the runtime briefly reports that <i>both</i> hands have no profile, the remaining hand
 *     returns about a second later.
 * </p>
 * <p>
 *     A lost controller is "regained" when that hand gets a profile again. A hand gaining a profile for the first time
 *     is not a regain.
 * </p>
 */
class InteractionProfileTracker{

    private static final Logger LOGGER = Logger.getLogger(InteractionProfileTracker.class.getName());

    public static final double DEFAULT_LOSS_GRACE_PERIOD = 1;

    private final EnumMap<HandSide, String> currentProfiles = new EnumMap<>(HandSide.class);

    /**
     * Hands that have lost their profile but are still within the grace period, and the time they lost it.
     */
    private final EnumMap<HandSide, Double> pendingLosses = new EnumMap<>(HandSide.class);

    private final Set<HandSide> lostHands = EnumSet.noneOf(HandSide.class);

    private double lossGracePeriod = DEFAULT_LOSS_GRACE_PERIOD;

    final ObservableEvent profileChanged = new ObservableEvent();

    final ObservableDataEvent<HandSide> controllerLost = new ObservableDataEvent<>();

    final ObservableDataEvent<HandSide> controllerRegained = new ObservableDataEvent<>();

    Optional<String> getCurrentProfile(HandSide handSide){
        return Optional.ofNullable(currentProfiles.get(handSide));
    }

    double getLossGracePeriod(){
        return lossGracePeriod;
    }

    void setLossGracePeriod(double lossGracePeriod){
        this.lossGracePeriod = lossGracePeriod;
    }

    /**
     * @param newProfiles the profile for each hand. Hands with no profile should be absent.
     * @param alreadyPaused if true then controllers disappearing will not be reported as lost (e.g. because the
     *                      application is already paused because the headset has been taken off, which also
     *                      causes controllers to sleep)
     * @param time the current time in seconds (any consistent time base)
     */
    void update(Map<HandSide, String> newProfiles, boolean alreadyPaused, double time){
        if (currentProfiles.equals(newProfiles)){
            return;
        }
        LOGGER.info("Interaction profiles changed from " + currentProfiles + " to " + newProfiles);

        for(HandSide handSide : HandSide.values()){
            String oldProfile = currentProfiles.get(handSide);
            String newProfile = newProfiles.get(handSide);

            if (oldProfile != null && newProfile == null){
                if (alreadyPaused){
                    LOGGER.info(handSide + " controller disappeared while already paused, not reporting as lost");
                }else{
                    LOGGER.info(handSide + " controller disappeared, will report as lost if not back within " + lossGracePeriod + "s");
                    pendingLosses.put(handSide, time);
                }
            } else if (oldProfile == null && newProfile != null){
                if (pendingLosses.remove(handSide) != null){
                    LOGGER.info(handSide + " controller returned within the grace period, not reporting as lost");
                } else if (lostHands.remove(handSide)){
                    LOGGER.info(handSide + " controller regained");
                    controllerRegained.fireEvent(handSide);
                }
            }
        }

        currentProfiles.clear();
        currentProfiles.putAll(newProfiles);
        profileChanged.fireEvent();
    }

    /**
     * Should be called every frame, reports controllers as lost once they have been missing for the grace period.
     *
     * @param alreadyPaused if true pending losses are abandoned (e.g. the headset was taken off shortly after the
     *                      controller disappeared, controllers sleeping is expected then)
     * @param time the current time in seconds (same time base as {@link #update})
     */
    void tick(boolean alreadyPaused, double time){
        Iterator<Map.Entry<HandSide, Double>> pendingIterator = pendingLosses.entrySet().iterator();
        while(pendingIterator.hasNext()){
            Map.Entry<HandSide, Double> pending = pendingIterator.next();
            // EnumMap entries can't be read after removal, so take the values first
            HandSide handSide = pending.getKey();
            double lossTime = pending.getValue();
            if (alreadyPaused){
                LOGGER.info(handSide + " controller disappeared shortly before pausing, not reporting as lost");
                pendingIterator.remove();
            } else if (time - lossTime >= lossGracePeriod){
                LOGGER.info(handSide + " controller lost");
                pendingIterator.remove();
                lostHands.add(handSide);
                controllerLost.fireEvent(handSide);
            }
        }
    }
}
