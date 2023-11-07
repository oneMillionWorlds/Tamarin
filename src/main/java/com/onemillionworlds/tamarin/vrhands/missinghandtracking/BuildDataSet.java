package com.onemillionworlds.tamarin.vrhands.missinghandtracking;

import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.GrabPickingFunction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This is a utility to record actual hand tracking data from a working system to then hard code for later usage
 * by a system that doesn't have hand tracking.
 */
public class BuildDataSet{

    /**
     * this non-linear set of points is to get more data at the start of the grab where the curl is more subtle
     */
    static float[] pointsToRecord = new float[]{0, 0.05f, 0.1f, 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.7f,1};
    static Map<HandSide, Map<Float, Map<HandJoint, BonePose>>> skeletonData = new LinkedHashMap<>();

    static boolean complete = false;

    static float buildUp = 0;


    public static void fillInData(HandSide handSide, BoundHand hand, OpenXrActionState actionState, Map<HandJoint, BonePose> bonePoses){
        Map<Float, Map<HandJoint, BonePose>> handSideData = skeletonData.computeIfAbsent(handSide, hs -> new LinkedHashMap<>());
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

        // Round the grabStrength to the nearest interval value
        float roundedGrabStrength = findClosest(grabStrength);

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

    public static float findClosest(float target) {
        float closest = pointsToRecord[0];
        float minDifference = Math.abs(target - closest);

        for (float point : pointsToRecord) {
            float difference = Math.abs(target - point);

            if (difference < minDifference) {
                minDifference = difference;
                closest = point;
            }
        }

        return closest;
    }

    private static boolean isDataSetComplete() {
        for (HandSide handSide : HandSide.values()) {
            Map<Float, Map<HandJoint, BonePose>> handData = skeletonData.get(handSide);
            if (handData == null || handData.size() != pointsToRecord.length) {
                return false;
            }
        }
        return true;
    }

    private static void printJavaMapCode() {
        StringBuilder sb = new StringBuilder();
        for (HandSide handSide : HandSide.values()) {

            skeletonData.get(handSide).forEach((i, bonePoses) -> {
                for (Map.Entry<HandJoint, BonePose> entry : bonePoses.entrySet()) {
                    BonePose bonePose = entry.getValue();

                    sb.append(String.format("skeletonData.computeIfAbsent(HandSide.%s, hs -> new HashMap<>()).computeIfAbsent(%ff, hs -> new HashMap<>()).put(HandJoint.%s,new BonePose(new Vector3f(%ff, %ff, %ff), new Quaternion(%ff, %ff, %ff, %ff), %ff));\n",
                            handSide.name(), i, entry.getKey().name(),
                            bonePose.position().x, bonePose.position().y, bonePose.position().z,
                            bonePose.orientation().getX(), bonePose.orientation().getY(), bonePose.orientation().getZ(), bonePose.orientation().getW(),
                            bonePose.radius()));
                }
            });

        }
        System.out.println(sb);
        complete = true;
    }
}
