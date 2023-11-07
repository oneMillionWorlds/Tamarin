package com.onemillionworlds.tamarin.vrhands.missinghandtracking;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This is a utility to record actual hand tracking data from a working system to then hard code for later usage
 * by a system that doesn't have hand tracking.
 */
public class BuildDataSet{

    static int noOfHandPoints = 4;
    static Map<HandSide, Map<Float, Map<HandJoint, BonePose>>> skeletonData = new HashMap<>();

    static boolean complete = false;

    static float buildUp = 0;


    public static void fillInData(HandSide handSide, BoundHand hand, OpenXrActionState actionState, Map<HandJoint, BonePose> bonePoses){
        Map<Float, Map<HandJoint, BonePose>> handSideData = skeletonData.computeIfAbsent(handSide, hs -> new HashMap<>());
        Optional<GrabPickingFunction> grabPickingFunction = hand.getFunctionOpt(GrabPickingFunction.class);
        if (grabPickingFunction.isEmpty() || complete){
            return;
        }
        //this allows 2 seconds to get ready
        buildUp+= 1/(90f * 2);
        if (buildUp < 2){
            return;
        }

        ActionHandle grabHandle = grabPickingFunction.get().getGrabAction();
        float grabStrength = actionState.getFloatActionState(grabHandle, handSide.restrictToInputString).getState();

        // Calculate the interval for the grabStrengths we're interested in
        float interval = 1.0f / (noOfHandPoints - 1);

        // Round the grabStrength to the nearest interval value
        float roundedGrabStrength = Math.round(grabStrength / interval) * interval;

        // Check if the roundedGrabStrength is within the acceptable range
        if (Math.abs(roundedGrabStrength - grabStrength) <= 0.05) {
            // Check if this grabStrength has not been filled yet
            if (!handSideData.containsKey(roundedGrabStrength)) {
                // Fill in the bonePoses for this grabStrength
                System.out.println("Fill " + handSide.name() + " " + roundedGrabStrength);
                handSideData.put(roundedGrabStrength, new HashMap<>(bonePoses));
            }
        }
        // Check if the dataset is complete for both hands
        if (isDataSetComplete()) {
            printJavaMapCode();
        }
    }

    private static boolean isDataSetComplete() {
        for (HandSide handSide : HandSide.values()) {
            Map<Float, Map<HandJoint, BonePose>> handData = skeletonData.get(handSide);
            if (handData == null || handData.size() != noOfHandPoints) {
                return false;
            }
            for (float i = 0; i <= 1; i += 1.0f / (noOfHandPoints - 1)) {
                if (!handData.containsKey(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void printJavaMapCode() {
        StringBuilder sb = new StringBuilder();
        for (HandSide handSide : HandSide.values()) {
            for (float i = 0; i <= 1; i += 1.0f / (noOfHandPoints - 1)) {
                Map<HandJoint, BonePose> bonePoses = skeletonData.get(handSide).get(i);
                for (Map.Entry<HandJoint, BonePose> entry : bonePoses.entrySet()) {
                    BonePose bonePose = entry.getValue();

                    sb.append(String.format("skeletonData.computeIfAbsent(HandSide.%s, hs -> new HashMap<>()).computeIfAbsent(%ff, hs -> new HashMap<>()).put(HandJoint.%s,new BonePose(new Vector3f(%ff, %ff, %ff), new Quaternion(%ff, %ff, %ff, %ff), %ff));\n",
                            handSide.name(), i, entry.getKey().name(),
                            bonePose.position().x, bonePose.position().y, bonePose.position().z,
                            bonePose.orientation().getX(), bonePose.orientation().getY(), bonePose.orientation().getZ(), bonePose.orientation().getW(),
                            bonePose.radius()));
                }
            }
        }
        System.out.println(sb);
        complete = true;
    }
}
