package com.onemillionworlds.tamarin.compatibility;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.VREnvironment;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.vr.lwjgl_openvr.LWJGLOpenVR;
import com.jme3.input.vr.lwjgl_openvr.LWJGLOpenVRInput;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import org.lwjgl.BufferUtils;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdQuaternionf;
import org.lwjgl.openvr.HmdVector3;
import org.lwjgl.openvr.HmdVector4;
import org.lwjgl.openvr.InputAnalogActionData;
import org.lwjgl.openvr.InputDigitalActionData;
import org.lwjgl.openvr.InputPoseActionData;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRActiveActionSet;
import org.lwjgl.openvr.VRBoneTransform;
import org.lwjgl.openvr.VRInput;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This app state provides action based OpenVR calls (aka semi modern VR), this is functionality that will come in
 * JMonkey 3.6 but does not yet exist. This provides that functionality but makes it available in JMonkey 3.4 and 3.5.
 *
 * This app state closely follows LWJGLOpenVRInput as it will exist in 3.6
 *
 * Before binding this app state the VR context should already have been booted (using JMonkey) and the JMonkey
 * app state {@link VRAppState} will already have been bound. The setting VRConstants.SETTING_VRAPI must have been set to
 * VRConstants.SETTING_VRAPI_OPENVR_LWJGL_VALUE (as this uses LWJGL)
 */
public class ActionBasedOpenVr extends BaseAppState{

    private static final Logger logger = Logger.getLogger(ActionBasedOpenVr.class.getName());

    /**
     * A map of the action name to the objects/data required to read states from lwjgl
     */
    private final Map<String, LWJGLOpenVRDigitalActionData> digitalActions = new HashMap<>();

    /**
     * A map of the action name to the objects/data required to read states from lwjgl
     */
    private final Map<String, LWJGLOpenVRAnalogActionData> analogActions = new HashMap<>();

    /**
     * A map of the action name to the handle of a haptic action
     */
    private final Map<String, Long> hapticActionHandles = new HashMap<>();

    /**
     * A map of the action set name to the handle that is used to refer to it when talking to LWJGL
     */
    private final Map<String, Long> actionSetHandles = new HashMap<>();

    /**
     * These are the cached skeleton data (what bones there are, what the handles are etc)
     *
     * It is a map of action name to that name (/skeleton/hand/left or /skeleton/hand/right should be bound to an
     * action of type skeleton in the action manifest).
     */
    private final Map<String, LWJGLSkeletonData> skeletonActions = new HashMap<>();

    /**
     * A map of input names (e.g. /user/hand/right) to the handle used to address it.
     *
     * Note that null is a special case that maps to VR.k_ulInvalidInputValueHandle and means "any input"
     */
    private final Map<String, Long> inputHandles = new HashMap<>();

    /**
     * A lwjgl object that contains handles to the active action sets (is used each frame to tell lwjgl which actions to
     * fetch states back for)
     */
    private VRActiveActionSet.Buffer activeActionSets;

    private VREnvironment environment;

    InputMode inputMode = InputMode.LEGACY;

    private enum InputMode{
        /**
         * Simple bitfield, no way to map new controllers (Not directly supported by Tamarin, only JMonkeyVr
         */
        LEGACY,
        /**
         * Actions manifest based. Forward ported by Tamarin
         */
        ACTION_BASED
    }

    {
        inputHandles.put(null, VR.k_ulInvalidInputValueHandle);
    }

    @Override
    public void initialize(Application app){
        VRAppState vrAppState = app.getStateManager().getState(VRAppState.class);

        if (vrAppState == null){
            throw new IllegalStateException("ActionBasedOpenVr must be attached AFTER VRAppState");
        }

        if (!(vrAppState.getVRinput() instanceof LWJGLOpenVRInput)){
            String message = "Attempting to use ActionBasedOpenVR with " + vrAppState.getVRinput().getClass().getSimpleName() + " but only LWJGLOpenVRInput is supported";
            assert false :  message;
            logger.warning(message);
        }

        environment = vrAppState.getVREnvironment();
    }

    @Override
    protected void cleanup(Application app){

    }

    @Override
    protected void onEnable(){

    }

    @Override
    protected void onDisable(){

    }



    public void registerActionManifest(String actionManifestAbsolutePath, String startingActiveActionSets){
        inputMode = InputMode.ACTION_BASED;
        withErrorCodeWarning("registering an action manifest", VRInput.VRInput_SetActionManifestPath(actionManifestAbsolutePath));

        setActiveActionSet(startingActiveActionSets);
    }

    public void setActiveActionSet(String actionSet){
        assert inputMode == InputMode.ACTION_BASED : "registerActionManifest must be called before attempting to fetch action states";

        long actionSetHandle;
        if (actionSetHandles.containsKey(actionSet)){
            actionSetHandle = actionSetHandles.get(actionSet);
        }else{
            LongBuffer longBuffer = BufferUtils.createLongBuffer(1);
            int errorCode = VRInput.VRInput_GetActionHandle(actionSet, longBuffer);
            if ( errorCode != 0 )
            {
                logger.warning( "An error code of " + errorCode + " was reported while fetching an action set handle for " + actionSet );
            }
            actionSetHandle = longBuffer.get(0);
            actionSetHandles.put(actionSet,actionSetHandle);
        }

        //Todo: this seems to imply that you could have multiple active action sets at once (Although I was not able to get that to work), allow multiple action sets
        activeActionSets = VRActiveActionSet.create(1);
        activeActionSets.ulActionSet(actionSetHandle);
        activeActionSets.ulRestrictedToDevice(VR.k_ulInvalidInputValueHandle); // both hands
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for digital style actions (a button is pressed, or not)
     *
     * This method is commonly called when it is important which hand the action is found on. For example while
     * holding a weapon a button may be bound to "eject magazine" to allow you to load a new one, but that would only
     * want to take effect on the hand that is holding the weapon
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from in
     * the action manifest default bindings.
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    public DigitalActionState getDigitalActionState(String actionName, String restrictToInput){
        assert inputMode == InputMode.ACTION_BASED : "registerActionManifest must be called before attempting to fetch action states";

        LWJGLOpenVRDigitalActionData actionDataObjects = digitalActions.get(actionName);
        if (actionDataObjects == null){
            //this is the first time the action has been used. We must obtain a handle to it to efficiently fetch it in future
            long handle = fetchActionHandle(actionName);
            actionDataObjects = new LWJGLOpenVRDigitalActionData(actionName, handle, InputDigitalActionData.create());
            digitalActions.put(actionName, actionDataObjects);
        }
        int errorCode = VRInput.VRInput_GetDigitalActionData(actionDataObjects.actionHandle, actionDataObjects.actionData, getOrFetchInputHandle(restrictToInput));

        if (errorCode == VR.EVRInputError_VRInputError_WrongType){
            throw new RuntimeException("Attempted to fetch a non-digital state as if it is digital");
        }else if (errorCode!=0){
            logger.warning( "An error code of " + errorCode + " was reported while fetching an action state for " + actionName );
        }

        return new DigitalActionState(actionDataObjects.actionData.bState(), actionDataObjects.actionData.bChanged());
    }

    /**
     * A pose is where a hand is, and what its rotation is.
     *
     * @param actionName the action name that has been bound to a pose in the action manifest
     * @return the PoseActionState
     */
    public PoseActionState getPose(String actionName){

        InputPoseActionData inputPose = InputPoseActionData.create();

        VRInput.VRInput_GetPoseActionDataForNextFrame(fetchActionHandle(actionName), VR.ETrackingUniverseOrigin_TrackingUniverseSeated, inputPose, getOrFetchInputHandle(null));

        HmdMatrix34 hmdMatrix34 = inputPose.pose().mDeviceToAbsoluteTracking();

        Matrix4f pose = LWJGLOpenVR.convertSteamVRMatrix3ToMatrix4f(hmdMatrix34, new Matrix4f() );
        HmdVector3 velocity = inputPose.pose().vVelocity();
        HmdVector3 angularVelocity =inputPose.pose().vAngularVelocity();

        Object obs = environment.getObserver();
        Vector3f position = pose.toTranslationVector();
        if (obs instanceof Camera) {
            //((Camera) obs).getRotation().mult(position, position);
            position.addLocal(((Camera) obs).getLocation());
        } else {
            //((Spatial) obs).getWorldRotation().mult(position, position);
            position.addLocal(((Spatial) obs).getWorldTranslation());
        }
        return new PoseActionState(pose, position, pose.toRotationQuat(), new Vector3f(velocity.v(0), velocity.v(1), velocity.v(2)), new Vector3f(angularVelocity.v(0), angularVelocity.v(1), angularVelocity.v(2)));
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for analog style actions (most commonly joysticks, but button pressure can also be mapped in analog).
     *
     * This method is commonly called when it's not important which hand the action is bound to (e.g. if the thumb stick
     * is controlling a third-person character in-game that could be bound to either left or right hand and that would
     * not matter).
     *
     * If the handedness matters use {@link #getAnalogActionState(String, String)}
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    public AnalogActionState getAnalogActionState( String actionName ){
        return getAnalogActionState(actionName, null);
    }

    /**
     * Gets the current state of the action (abstract version of a button press).
     *
     * This is called for analog style actions (most commonly joysticks, but button pressure can also be mapped in analog).
     *
     * This method is commonly called when it is important which hand the action is found on. For example an "in universe"
     * joystick that has a hat control might (while you are holding it) bind to the on-controller hat, but only on the hand
     * holding it
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to receive the input from in
     * the action manifest default bindings.
     *
     * {@link #registerActionManifest} must have been called before using this method.
     *
     * @param actionName The name of the action. E.g. /actions/main/in/openInventory
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right. Or null, which means "any input"
     * @return the DigitalActionState that has details on if the state has changed, what the state is etc.
     */
    public AnalogActionState getAnalogActionState(String actionName, String restrictToInput ){
        assert inputMode == InputMode.ACTION_BASED : "registerActionManifest must be called before attempting to fetch action states";

        LWJGLOpenVRAnalogActionData actionDataObjects = analogActions.get(actionName);
        if (actionDataObjects == null){
            //this is the first time the action has been used. We must obtain a handle to it to efficiently fetch it in future
            long handle = fetchActionHandle(actionName);
            actionDataObjects = new LWJGLOpenVRAnalogActionData(actionName, handle, InputAnalogActionData.create());
            analogActions.put(actionName, actionDataObjects);
        }
        int errorCode = VRInput.VRInput_GetAnalogActionData(actionDataObjects.actionHandle, actionDataObjects.actionData, getOrFetchInputHandle(restrictToInput));

        if (errorCode == VR.EVRInputError_VRInputError_WrongType){
            throw new RuntimeException("Attempted to fetch a non-analog state as if it is analog");
        }else if (errorCode!=0){
            logger.warning( "An error code of " + errorCode + " was reported while fetching an action state for " + actionName );
        }

        return new AnalogActionState(actionDataObjects.actionData.x(), actionDataObjects.actionData.y(), actionDataObjects.actionData.z(), actionDataObjects.actionData.deltaX(), actionDataObjects.actionData.deltaY(), actionDataObjects.actionData.deltaZ());
    }

    /**
     * Triggers a haptic action (aka a vibration).
     *
     * Note if you want a haptic action in only one hand that is done either by only binding the action to one hand in
     * the action manifest's standard bindings or by binding to both and using {@link #triggerHapticAction(String, float, float, float, String)}
     * to control which input it gets set to at run time
     *
     * @param actionName The name of the action. Will be something like /actions/main/out/vibrate
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     */
    public void triggerHapticAction( String actionName, float duration, float frequency, float amplitude){
        triggerHapticAction( actionName, duration, frequency, amplitude, null );
    }

    /**
     * Triggers a haptic action (aka a vibration) restricted to just one input (e.g. left or right hand).
     *
     * Note that restrictToInput only restricts, it must still be bound to the input you want to send the haptic to in
     * the action manifest default bindings.
     *
     * This method is typically used to bind the haptic to both hands then decide at run time which hand to sent to     *
     *
     * @param actionName The name of the action. Will be something like /actions/main/out/vibrate
     * @param duration how long in seconds the
     * @param frequency in cycles per second
     * @param amplitude between 0 and 1
     * @param restrictToInput the input to restrict the action to. E.g. /user/hand/right, /user/hand/left. Or null, which means "any input"
     */
    public void triggerHapticAction(String actionName, float duration, float frequency, float amplitude, String restrictToInput ){
        long hapticActionHandle;
        if (!hapticActionHandles.containsKey(actionName)){
            //this is the first time the action has been used. We must obtain a handle to it to efficiently fetch it in future
            hapticActionHandle = fetchActionHandle(actionName);
            hapticActionHandles.put(actionName, hapticActionHandle);
        }else{
            hapticActionHandle = hapticActionHandles.get(actionName);
        }

        VRInput.VRInput_TriggerHapticVibrationAction(hapticActionHandle, 0, duration, frequency, amplitude, getOrFetchInputHandle(restrictToInput));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        if (inputMode == InputMode.ACTION_BASED){
            withErrorCodeWarning("UpdateActionState", VRInput.VRInput_UpdateActionState(activeActionSets, VRActiveActionSet.SIZEOF));
        }
    }

    /**
     * Converts an action name (as it appears in the action manifest) to a handle (long) that the rest of the
     * lwjgl (and openVR) wants to talk in
     * @param actionName The name of the action. Will be something like /actions/main/in/openInventory
     * @return a long that is the handle that can be used to refer to the action
     */
    private long fetchActionHandle( String actionName ){
        LongBuffer longBuffer = BufferUtils.createLongBuffer(1);
        int errorCode = VRInput.VRInput_GetActionHandle(actionName, longBuffer);
        if (errorCode !=0 ){
            logger.warning( "An error code of " + errorCode + " was reported while registering an action manifest" );
        }
        return longBuffer.get(0);
    }

    /**
     * Given an input name returns the handle to address it.
     *
     * If a cached handle is available it is returned, if not it is fetched from openVr
     *
     * @param inputName the input name, e.g. /user/hand/right. Or null, which means "any input"
     * @return the handle
     */
    public long getOrFetchInputHandle( String inputName ){
        if(!inputHandles.containsKey(inputName)){
            LongBuffer longBuffer = BufferUtils.createLongBuffer(1);

            int errorCode = VRInput.VRInput_GetInputSourceHandle(inputName, longBuffer);
            if (errorCode !=0 ){
                logger.warning( "An error code of " + errorCode + " was reported while fetching an input manifest" );
            }
            long handle = longBuffer.get(0);
            inputHandles.put(inputName, handle);
        }

        return inputHandles.get(inputName);
    }

    public Map<String, BoneStance> getModelRelativeSkeletonPositions(String actionName){
        LWJGLSkeletonData skeletonData = getOrFetchSkeletonData(actionName);

        ByteBuffer skeletonBuffer = BufferUtils.createByteBuffer(VRBoneTransform.SIZEOF*skeletonData.boneNames.length);
        VRBoneTransform.Buffer boneBuffer = new VRBoneTransform.Buffer(skeletonBuffer);

        VRInput.VRInput_GetSkeletalBoneData(skeletonData.skeletonAction, VR.EVRSkeletalTransformSpace_VRSkeletalTransformSpace_Model, VR.EVRSkeletalMotionRange_VRSkeletalMotionRange_WithoutController, boneBuffer);

        Map<String, BoneStance> positions = new HashMap<>();

        int i=0;
        for(VRBoneTransform boneTransform : boneBuffer){
            String boneName = skeletonData.boneNames[i];

            Vector3f position = new Vector3f(boneTransform.position$().v(0),boneTransform.position$().v(1), boneTransform.position$().v(2) );

            //flip the pitch
            //Quaternion rotation = new Quaternion(-boneTransform.orientation().x(), boneTransform.orientation().y(), -boneTransform.orientation().z(), boneTransform.orientation().w() );
            Quaternion rotation = new Quaternion(boneTransform.orientation().x(), boneTransform.orientation().y(), boneTransform.orientation().z(), boneTransform.orientation().w() );

            positions.put(boneName, new BoneStance(position, rotation) );
            i++;
        }
        return positions;
    }

    /**
     * Given a hand armature (which should have 31 bones with names as defined in the below link)
     * it will pull from the requested action name and update the bones to be at the
     * appropriate positions. Note that all OpenVr compatible devices will have the same bone names
     * (although the fidelity of their positions may vary)
     *
     * See https://github.com/ValveSoftware/openvr/wiki/Hand-Skeleton
     *
     * NOTE: the bone orientation is surprising and non-natural. If you build a hand model, and it appears
     * distorted try importing the example (as described in the above link) into blender from the fbx format. This will
     * give bones that appear not to lie along the anatomical bone set. Despite looking odd those bones work correctly
     * (and bones that track anatomical bones seemingly do not). The library Tamarin also has a starting blender file
     * that can be used.
     *
     * @param actionName the action name by which a particular skeleton has been bound to.
     * @param armature a JMonkey armature (aka set of bones)
     * @param handMode the hands "stance". See {@link HandMode} for more details
     */
    public void updateHandSkeletonPositions( String actionName, Armature armature, HandMode handMode ){
        LWJGLSkeletonData skeletonData = getOrFetchSkeletonData(actionName);

        ByteBuffer skeletonBuffer = BufferUtils.createByteBuffer(VRBoneTransform.SIZEOF*skeletonData.boneNames.length);
        VRBoneTransform.Buffer boneBuffer = new VRBoneTransform.Buffer(skeletonBuffer);

        //EVRSkeletalTransformSpace_VRSkeletalTransformSpace_Parent means ask for bones relative to their parent (which is also what JME armature wants to talk about)
        withErrorCodeWarning("Getting bone data", VRInput.VRInput_GetSkeletalBoneData(skeletonData.skeletonAction, VR.EVRSkeletalTransformSpace_VRSkeletalTransformSpace_Parent, handMode.openVrHandle, boneBuffer));

        int i=0;
        for(VRBoneTransform boneTransform : boneBuffer){
            String boneName = skeletonData.boneNames[i];

            HmdQuaternionf orientation = boneTransform.orientation();
            HmdVector4 position = boneTransform.position$();

            Vector3f positionJme = new Vector3f(position.v(0), position.v(1), position.v(2) );
            Quaternion orientationJME = new Quaternion(orientation.x(), orientation.y(),orientation.z() , orientation.w());

            Joint joint;
            if (boneName.equals("Root")){
                joint = armature.getRoots()[0];
            }else{
                joint = armature.getJoint(boneName);
            }
            if (joint!=null){
                joint.setLocalTranslation(positionJme);

                joint.setLocalRotation(orientationJME);
            }
            i++;
        }
    }

    /**
     * Fetches (or gets from the cache) data on the hand skeleton
     *
     * @param actionName the input name, e.g. /actions/default/in/HandSkeletonLeft.
     * @return data on the skeleton (e.g. names)
     */
    public LWJGLSkeletonData getOrFetchSkeletonData(String actionName ){
        if(!skeletonActions.containsKey(actionName)){

            long actionHandle = fetchActionHandle(actionName);

            IntBuffer intBuffer = BufferUtils.createIntBuffer(1);

            withErrorCodeWarning("getting Bone count", VRInput.VRInput_GetBoneCount(actionHandle, intBuffer) );

            int numberOfBones = intBuffer.get(0); //hopefully 31 for the full hand

            String[] boneNames = new String[numberOfBones];

            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(200);

            for(int i=0;i<boneNames.length;i++){
                withErrorCodeWarning("getting Bone Name", VRInput.VRInput_GetBoneName(actionHandle, i, byteBuffer));

                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                boneNames[i] = new String(bytes, StandardCharsets.UTF_8).trim();
                byteBuffer.rewind();
            }

            LWJGLSkeletonData skeletonData = new LWJGLSkeletonData(actionName, actionHandle, boneNames);
            if (numberOfBones>0){ //don't cache a failed attempt to get bones, maybe the controller will wake up later
                skeletonActions.put(actionName, skeletonData);
            }
            return skeletonData;
        }else{
            return skeletonActions.get(actionName);
        }
    }

    private static void withErrorCodeWarning(String eventText, int errorCode){
        if (errorCode !=0 ){
            logger.warning( "An error code of " + errorCode + " was reported while " + eventText);
        }
    }
}
