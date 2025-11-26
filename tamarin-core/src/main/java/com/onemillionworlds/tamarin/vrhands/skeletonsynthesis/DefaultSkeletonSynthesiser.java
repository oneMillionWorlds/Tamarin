package com.onemillionworlds.tamarin.vrhands.skeletonsynthesis;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.vrhands.BoundHand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"ClassCanBeRecord", "unused"})
public class DefaultSkeletonSynthesiser implements SkeletonSynthesiser {
    private static final Logger LOGGER = Logger.getLogger(DefaultSkeletonSynthesiser.class.getName());


    public static Map<HandSide,Map<HandJoint, BonePose>> OPEN_HAND_FALLBACK = new HashMap<>();
    
    static{
        OPEN_HAND_FALLBACK.put(HandSide.LEFT, new HashMap<>());
        OPEN_HAND_FALLBACK.put(HandSide.RIGHT, new HashMap<>());
        
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.LITTLE_INTERMEDIATE_EXT,new BonePose(new Vector3f(-0.041532f, -0.097547f, 0.069675f), new Quaternion(0.275551f, -0.090107f, -0.776972f, -0.558813f), 0.011540f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.LITTLE_PROXIMAL_EXT,new BonePose(new Vector3f(-0.044486f, -0.081800f, 0.095299f), new Quaternion(0.205780f, -0.183584f, -0.761181f, -0.586993f), 0.020110f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.PALM_EXT,new BonePose(new Vector3f(-0.042735f, -0.035095f, 0.139039f), new Quaternion(-0.020554f, -0.068168f, -0.625195f, -0.777217f), 0.023547f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.RING_METACARPAL_EXT,new BonePose(new Vector3f(-0.042078f, -0.048135f, 0.152133f), new Quaternion(0.041297f, -0.096190f, -0.639800f, -0.761382f), 0.026868f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.THUMB_PROXIMAL_EXT,new BonePose(new Vector3f(-0.016789f, 0.016339f, 0.118151f), new Quaternion(-0.392974f, 0.090845f, 0.025631f, -0.914695f), 0.016903f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.RING_TIP_EXT,new BonePose(new Vector3f(-0.039147f, -0.089206f, 0.013785f), new Quaternion(0.230170f, -0.044044f, -0.710221f, -0.663832f), 0.011801f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.THUMB_METACARPAL_EXT,new BonePose(new Vector3f(-0.022271f, -0.016465f, 0.141097f), new Quaternion(-0.452649f, 0.105695f, -0.056455f, -0.883604f), 0.029145f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.MIDDLE_PROXIMAL_EXT,new BonePose(new Vector3f(-0.050507f, -0.038240f, 0.080609f), new Quaternion(0.070130f, -0.064673f, -0.754206f, -0.649675f), 0.017228f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.RING_PROXIMAL_EXT,new BonePose(new Vector3f(-0.048249f, -0.060391f, 0.087673f), new Quaternion(0.145598f, -0.114283f, -0.702309f, -0.687392f), 0.018243f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.RING_DISTAL_EXT,new BonePose(new Vector3f(-0.042001f, -0.085293f, 0.023247f), new Quaternion(0.230170f, -0.044044f, -0.710221f, -0.663832f), 0.011801f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.MIDDLE_DISTAL_EXT,new BonePose(new Vector3f(-0.043826f, -0.051025f, 0.005850f), new Quaternion(0.105834f, -0.039957f, -0.744533f, -0.657936f), 0.012969f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.RING_INTERMEDIATE_EXT,new BonePose(new Vector3f(-0.046320f, -0.075069f, 0.049765f), new Quaternion(0.180718f, -0.078160f, -0.708725f, -0.677455f), 0.013501f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.MIDDLE_TIP_EXT,new BonePose(new Vector3f(-0.042469f, -0.053594f, -0.006743f), new Quaternion(0.104145f, -0.044172f, -0.770334f, -0.627531f), 0.012969f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.THUMB_TIP_EXT,new BonePose(new Vector3f(-0.007390f, 0.055050f, 0.090631f), new Quaternion(-0.566516f, 0.113406f, 0.012146f, -0.816123f), 0.013666f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.LITTLE_DISTAL_EXT,new BonePose(new Vector3f(-0.035576f, -0.105694f, 0.054546f), new Quaternion(0.291033f, -0.152579f, -0.756790f, -0.565061f), 0.010302f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.INDEX_METACARPAL_EXT,new BonePose(new Vector3f(-0.036020f, -0.015290f, 0.150507f), new Quaternion(-0.089984f, -0.033397f, -0.758342f, -0.644755f), 0.029847f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.INDEX_PROXIMAL_EXT,new BonePose(new Vector3f(-0.049373f, -0.010427f, 0.077504f), new Quaternion(0.029890f, 0.031525f, -0.731832f, -0.680102f), 0.018792f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.WRIST_EXT,new BonePose(new Vector3f(-0.041038f, -0.040223f, 0.167839f), new Quaternion(-0.051120f, 0.015170f, 0.637587f, 0.768536f), 0.032754f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.LITTLE_METACARPAL_EXT,new BonePose(new Vector3f(-0.041509f, -0.060736f, 0.154538f), new Quaternion(0.100191f, -0.139235f, -0.711281f, -0.681660f), 0.028244f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.INDEX_TIP_EXT,new BonePose(new Vector3f(-0.044032f, -0.010262f, -0.006063f), new Quaternion(0.023401f, -0.000777f, -0.698606f, -0.715126f), 0.011676f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.INDEX_INTERMEDIATE_EXT,new BonePose(new Vector3f(-0.045567f, -0.010186f, 0.033740f), new Quaternion(0.010602f, 0.018493f, -0.700410f, -0.713425f), 0.012606f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.INDEX_DISTAL_EXT,new BonePose(new Vector3f(-0.044384f, -0.009877f, 0.005070f), new Quaternion(0.023401f, -0.000777f, -0.698606f, -0.715126f), 0.011676f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.THUMB_DISTAL_EXT,new BonePose(new Vector3f(-0.010730f, 0.039564f, 0.096214f), new Quaternion(-0.566516f, 0.113406f, 0.012146f, -0.816123f), 0.013666f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.LITTLE_TIP_EXT,new BonePose(new Vector3f(-0.033508f, -0.110014f, 0.048496f), new Quaternion(0.291033f, -0.152579f, -0.756790f, -0.565061f), 0.010302f));
        OPEN_HAND_FALLBACK.get(HandSide.LEFT).put(HandJoint.MIDDLE_INTERMEDIATE_EXT,new BonePose(new Vector3f(-0.049570f, -0.046374f, 0.038285f), new Quaternion(0.111693f, 0.004583f, -0.745942f, -0.656567f), 0.013162f));

        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.LITTLE_INTERMEDIATE_EXT,new BonePose(new Vector3f(0.037855f, -0.096057f, 0.068881f), new Quaternion(-0.274258f, -0.051364f, -0.777027f, 0.564248f), 0.011540f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.LITTLE_PROXIMAL_EXT,new BonePose(new Vector3f(0.042122f, -0.082050f, 0.095316f), new Quaternion(-0.204162f, -0.144678f, -0.765707f, 0.592521f), 0.020110f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.PALM_EXT,new BonePose(new Vector3f(0.042709f, -0.035102f, 0.139036f), new Quaternion(0.020386f, -0.067282f, -0.633749f, 0.770339f), 0.023547f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.RING_METACARPAL_EXT,new BonePose(new Vector3f(0.042078f, -0.048135f, 0.152133f), new Quaternion(-0.039404f, -0.096228f, -0.657980f, 0.745824f), 0.026868f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.THUMB_PROXIMAL_EXT,new BonePose(new Vector3f(0.014889f, 0.016341f, 0.119113f), new Quaternion(0.382317f, 0.127061f, -0.045040f, 0.914146f), 0.016903f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.RING_TIP_EXT,new BonePose(new Vector3f(0.033145f, -0.085912f, 0.013649f), new Quaternion(-0.255460f, 0.014038f, -0.711556f, 0.654395f), 0.011801f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.THUMB_METACARPAL_EXT,new BonePose(new Vector3f(0.021550f, -0.016881f, 0.141126f), new Quaternion(0.461113f, 0.122386f, -0.054061f, 0.877198f), 0.029145f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.MIDDLE_PROXIMAL_EXT,new BonePose(new Vector3f(0.050354f, -0.038282f, 0.080591f), new Quaternion(-0.066583f, -0.045071f, -0.748442f, 0.658311f), 0.017228f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.RING_PROXIMAL_EXT,new BonePose(new Vector3f(0.048120f, -0.060353f, 0.087654f), new Quaternion(-0.154683f, -0.078511f, -0.707288f, 0.685314f), 0.018243f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.RING_DISTAL_EXT,new BonePose(new Vector3f(0.037204f, -0.082571f, 0.022887f), new Quaternion(-0.255460f, 0.014038f, -0.711556f, 0.654395f), 0.011801f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.MIDDLE_DISTAL_EXT,new BonePose(new Vector3f(0.042533f, -0.049020f, 0.005586f), new Quaternion(-0.131249f, 0.012059f, -0.736908f, 0.663022f), 0.012969f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.RING_INTERMEDIATE_EXT,new BonePose(new Vector3f(0.043595f, -0.073501f, 0.049406f), new Quaternion(-0.192942f, -0.038891f, -0.711984f, 0.674049f), 0.013501f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.MIDDLE_TIP_EXT,new BonePose(new Vector3f(0.039826f, -0.051039f, -0.006889f), new Quaternion(-0.130660f, 0.017316f, -0.709710f, 0.692057f), 0.012969f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.THUMB_TIP_EXT,new BonePose(new Vector3f(0.005855f, 0.054644f, 0.090495f), new Quaternion(0.533170f, 0.132646f, -0.062189f, 0.833229f), 0.013666f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.LITTLE_DISTAL_EXT,new BonePose(new Vector3f(0.031158f, -0.103137f, 0.053526f), new Quaternion(-0.326179f, -0.062257f, -0.759791f, 0.558973f), 0.010302f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.INDEX_METACARPAL_EXT,new BonePose(new Vector3f(0.036020f, -0.015290f, 0.150507f), new Quaternion(0.088055f, -0.039606f, -0.713267f, 0.694212f), 0.029847f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.INDEX_PROXIMAL_EXT,new BonePose(new Vector3f(0.049452f, -0.010399f, 0.077520f), new Quaternion(-0.024736f, 0.028831f, -0.724215f, 0.688529f), 0.018792f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.WRIST_EXT,new BonePose(new Vector3f(0.041038f, -0.040223f, 0.167839f), new Quaternion(0.051120f, 0.015170f, 0.637587f, -0.768536f), 0.032754f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.LITTLE_METACARPAL_EXT,new BonePose(new Vector3f(0.041509f, -0.060736f, 0.154538f), new Quaternion(-0.109428f, -0.132630f, -0.741505f, 0.648542f), 0.028244f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.INDEX_TIP_EXT,new BonePose(new Vector3f(0.044044f, -0.010104f, -0.006062f), new Quaternion(-0.033836f, 0.015674f, -0.698871f, 0.714277f), 0.011676f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.INDEX_INTERMEDIATE_EXT,new BonePose(new Vector3f(0.046134f, -0.010061f, 0.033717f), new Quaternion(-0.012911f, 0.019394f, -0.700563f, 0.713212f), 0.012606f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.INDEX_DISTAL_EXT,new BonePose(new Vector3f(0.044821f, -0.009809f, 0.005052f), new Quaternion(-0.033836f, 0.015674f, -0.698871f, 0.714277f), 0.011676f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.THUMB_DISTAL_EXT,new BonePose(new Vector3f(0.008455f, 0.039442f, 0.097152f), new Quaternion(0.533170f, 0.132646f, -0.062189f, 0.833229f), 0.013666f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.LITTLE_TIP_EXT,new BonePose(new Vector3f(0.027870f, -0.106681f, 0.047512f), new Quaternion(-0.326179f, -0.062257f, -0.759791f, 0.558973f), 0.010302f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.MIDDLE_INTERMEDIATE_EXT,new BonePose(new Vector3f(0.048615f, -0.044969f, 0.038040f), new Quaternion(-0.109334f, 0.016150f, -0.737855f, 0.665852f), 0.013162f));
        OPEN_HAND_FALLBACK.get(HandSide.RIGHT).put(HandJoint.MIDDLE_METACARPAL_EXT,new BonePose(new Vector3f(0.041164f, -0.034459f, 0.150854f), new Quaternion(0.020386f, -0.067282f, -0.633749f, 0.770339f), 0.029865f));

    }

    // CSV resource path
    private static final String RESOURCE_PATH = "Tamarin/Data/simulatedJointValues.csv";

    // Async loading control
    private static boolean loadingStarted = false;
    private static boolean dataLoaded = false;
    private static ExecutorService loaderExecutor;

    // Per-hand grid data loaded from CSV
    private static final Map<HandSide, GridData> HAND_GRIDS = new HashMap<>();

    private final ActionHandle triggerAction;
    private final ActionHandle grabAction;

    private final SynthesiseMode synthesiseMode;

    public DefaultSkeletonSynthesiser(ActionHandle triggerAction, ActionHandle grabAction, SynthesiseMode synthesiseMode){
        this.triggerAction = triggerAction;
        this.grabAction = grabAction;
        this.synthesiseMode = synthesiseMode;
    }

    private void triggerDataLoad(){
        if(dataLoaded || loadingStarted){
            return;
        }
        loadingStarted = true;

        loaderExecutor = Executors.newSingleThreadExecutor();
        loaderExecutor.submit(() -> {
            try{
                loadCsvData();
                dataLoaded = true;
            } catch(Exception e){
                // If loading fails, leave dataLoaded=false so fallback is used
                LOGGER.log(Level.SEVERE, "Failed to load skeleton synthesis data", e);
            } finally{
                // Only ever needed once per process
                loaderExecutor.shutdown();
            }
        });
    }

    @Override
    public void initialise() {
        if(synthesiseMode == SynthesiseMode.ALWAYS_SYNTHESISE){
            triggerDataLoad();
        }
    }

    @Override
    public SynthesiseMode getSynthesiseMode() {
        return synthesiseMode;
    }

    /**
     * Given the current grip and trigger pressures, synthesise the positions of the bones in the hand skeleton.
     */
    @Override
    public Map<HandJoint, BonePose> synthesiseBonePositions(BoundHand boundHand){
        triggerDataLoad();

        HandSide handSide = boundHand.getHandSide();
        float triggerPressure = boundHand.getFloatActionState(triggerAction).getState();
        float gripPressure = boundHand.getFloatActionState(grabAction).getState();

        // If data not yet loaded, return the simple open-hand fallback
        if(!dataLoaded){
            return OPEN_HAND_FALLBACK.get(handSide);
        }

        GridData grid = HAND_GRIDS.get(handSide);
        if(grid == null || grid.gripValues.length == 0 || grid.triggerValues.length == 0){
            return OPEN_HAND_FALLBACK.get(handSide);
        }

        // Clamp inputs to [0,1]
        float gp = Math.max(0f, Math.min(1f, gripPressure));
        float tp = Math.max(0f, Math.min(1f, triggerPressure));

        // Find surrounding indices in non-uniform grids
        int gLowIdx = lowerIndex(grid.gripValues, gp);
        int gHighIdx = upperIndex(grid.gripValues, gp);
        int tLowIdx = lowerIndex(grid.triggerValues, tp);
        int tHighIdx = upperIndex(grid.triggerValues, tp);

        float g0 = grid.gripValues[gLowIdx];
        float g1 = grid.gripValues[gHighIdx];
        float t0 = grid.triggerValues[tLowIdx];
        float t1 = grid.triggerValues[tHighIdx];

        Map<Float, Map<Float, Map<HandJoint, BonePose>>> gMap0 = grid.data;

        Map<HandJoint, BonePose> q00 = safeGet(gMap0, g0, t0);
        Map<HandJoint, BonePose> q10 = safeGet(gMap0, g1, t0);
        Map<HandJoint, BonePose> q01 = safeGet(gMap0, g0, t1);
        Map<HandJoint, BonePose> q11 = safeGet(gMap0, g1, t1);

        // If any corner is missing, fallback
        if(q00 == null || q10 == null || q01 == null || q11 == null){
            return OPEN_HAND_FALLBACK.get(handSide);
        }

        // Exact match
        if(g0 == g1 && t0 == t1){
            return q00;
        }

        float tg = (g1 == g0) ? 0f : (gp - g0) / (g1 - g0);
        float tt = (t1 == t0) ? 0f : (tp - t0) / (t1 - t0);

        // 1D interpolation cases
        if(g0 == g1){
            return lerpPoseMap(q00, q01, tt);
        }
        if(t0 == t1){
            return lerpPoseMap(q00, q10, tg);
        }

        // Full bilinear: first along trigger for each grip, then along grip
        Map<HandJoint, BonePose> a = lerpPoseMap(q00, q01, tt);
        Map<HandJoint, BonePose> b = lerpPoseMap(q10, q11, tt);
        return lerpPoseMap(a, b, tg);
    }

    private static Map<HandJoint, BonePose> safeGet(Map<Float, Map<Float, Map<HandJoint, BonePose>>> data,
                                                    float grip, float trigger){
        Map<Float, Map<HandJoint, BonePose>> byTrigger = data.get(grip);
        if(byTrigger == null){
            return null;
        }
        return byTrigger.get(trigger);
    }

    private static Map<HandJoint, BonePose> lerpPoseMap(Map<HandJoint, BonePose> a,
                                                        Map<HandJoint, BonePose> b,
                                                        float t){
        Map<HandJoint, BonePose> out = new HashMap<>();
        for(HandJoint joint : HandJoint.values()){
            BonePose pa = a.get(joint);
            BonePose pb = b.get(joint);
            if(pa != null && pb != null){
                out.put(joint, lerpBonePose(pa, pb, t));
            } else if(pa != null){
                out.put(joint, pa);
            } else if(pb != null){
                out.put(joint, pb);
            }
        }
        return out;
    }

    private static BonePose lerpBonePose(BonePose a, BonePose b, float t){
        Vector3f pos = a.position().clone().interpolateLocal(b.position(), t);
        Quaternion ori = new Quaternion();
        ori.slerp(a.orientation(), b.orientation(), t);
        float radius = a.radius() + t * (b.radius() - a.radius());
        return new BonePose(pos, ori, radius);
    }

    private static int lowerIndex(float[] arr, float value){
        int idx = Arrays.binarySearch(arr, value);
        if(idx >= 0){
            return idx;
        }
        int insertion = -idx - 1; // index of first element greater than value
        int lower = insertion - 1;
        return Math.max(lower, 0);
    }

    private static int upperIndex(float[] arr, float value){
        int idx = Arrays.binarySearch(arr, value);
        if(idx >= 0){
            return idx;
        }
        int insertion = -idx - 1; // first element greater than value
        if(insertion >= arr.length){
            return arr.length - 1;
        }
        return insertion;
    }

    private static void loadCsvData() throws IOException{
        LOGGER.info("Loading skeleton synthesis data from " + RESOURCE_PATH);
        InputStream is = DefaultSkeletonSynthesiser.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if(is == null){
            throw new IOException("Resource not found: " + RESOURCE_PATH);
        }
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
            // Builders for each hand
            CsvGridBuilder leftBuilder = new CsvGridBuilder();
            CsvGridBuilder rightBuilder = new CsvGridBuilder();

            @SuppressWarnings("UnusedAssignment")
            String line = br.readLine(); // header
            // Expected header: handSide,targetGrip,targetTrigger,joint,positionX,positionY,positionZ,rotationX,rotationY,rotationZ,rotationW[,radius]
            while((line = br.readLine()) != null){
                if(line.isBlank()){
                    continue;
                }
                String[] cols = line.split(",");
                String sideStr = cols[0].trim();
                float grip = parseFloatSafe(cols[1]);
                float trigger = parseFloatSafe(cols[2]);
                String jointStr = cols[3].trim();
                float px = parseFloatSafe(cols[4]);
                float py = parseFloatSafe(cols[5]);
                float pz = parseFloatSafe(cols[6]);
                float qx = parseFloatSafe(cols[7]);
                float qy = parseFloatSafe(cols[8]);
                float qz = parseFloatSafe(cols[9]);
                float qw = parseFloatSafe(cols[10]);
                float radius = parseFloatSafe(cols[11]);

                HandSide side;
                try{
                    side = HandSide.valueOf(sideStr);
                } catch(IllegalArgumentException ex){
                    throw new IOException("Invalid hand side in CSV: " + sideStr);
                }
                HandJoint joint;
                try{
                    joint = HandJoint.valueOf(jointStr);
                } catch(IllegalArgumentException ex){
                    throw new IOException("Invalid joint in CSV: " + jointStr);
                }

                BonePose pose = new BonePose(new Vector3f(px, py, pz), new Quaternion(qx, qy, qz, qw), radius);

                if(side == HandSide.LEFT){
                    leftBuilder.add(grip, trigger, joint, pose);
                } else if(side == HandSide.RIGHT){
                    rightBuilder.add(grip, trigger, joint, pose);
                }
            }

            GridData leftGrid = leftBuilder.build();
            GridData rightGrid = rightBuilder.build();
            if(leftGrid != null){
                HAND_GRIDS.put(HandSide.LEFT, leftGrid);
            }
            if(rightGrid != null){
                HAND_GRIDS.put(HandSide.RIGHT, rightGrid);
            }
        }
    }

    private static float parseFloatSafe(String s){
        try{
            return Float.parseFloat(s.trim());
        } catch(Exception e){
            return 0f;
        }
    }

    private static class CsvGridBuilder{
        final TreeSet<Float> grips = new TreeSet<>();
        final TreeSet<Float> triggers = new TreeSet<>();
        final Map<Float, Map<Float, Map<HandJoint, BonePose>>> data = new HashMap<>();

        void add(float grip, float trigger, HandJoint joint, BonePose pose){
            grips.add(grip);
            triggers.add(trigger);
            data.computeIfAbsent(grip, g -> new HashMap<>())
                .computeIfAbsent(trigger, t -> new HashMap<>())
                .put(joint, pose);
        }

        GridData build(){
            if(grips.isEmpty() || triggers.isEmpty() || data.isEmpty()){
                return null;
            }
            List<Float> gList = new ArrayList<>(grips);
            List<Float> tList = new ArrayList<>(triggers);
            float[] gArr = new float[gList.size()];
            float[] tArr = new float[tList.size()];
            for(int i=0;i<gArr.length;i++) gArr[i] = gList.get(i);
            for(int i=0;i<tArr.length;i++) tArr[i] = tList.get(i);
            return new GridData(gArr, tArr, data);
        }
    }

    /**
     * Holds a non-uniform grid of poses for efficient corner lookup.
     */
    private static class GridData{
        final float[] gripValues;     // sorted ascending
        final float[] triggerValues;  // sorted ascending
        final Map<Float, Map<Float, Map<HandJoint, BonePose>>> data; // grip -> trigger -> joints

        GridData(float[] gripValues, float[] triggerValues,
                 Map<Float, Map<Float, Map<HandJoint, BonePose>>> data){
            this.gripValues = gripValues;
            this.triggerValues = triggerValues;
            this.data = data;
        }
    }

}
