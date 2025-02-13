package com.onemillionworlds.tamarin.actions;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.app.Application;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.actions.actionprofile.Action;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionManifest;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionSet;
import com.onemillionworlds.tamarin.actions.actionprofile.SuggestedBindingsProfileView;
import com.onemillionworlds.tamarin.actions.state.Vector2fActionState;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.actions.state.FloatActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.openxr.OpenXrSessionManager;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import org.lwjgl.openxr.XR10;
import org.lwjgl.openxr.XrAction;
import org.lwjgl.openxr.XrActionCreateInfo;
import org.lwjgl.openxr.XrActionSet;
import org.lwjgl.openxr.XrActionSetCreateInfo;
import org.lwjgl.openxr.XrActionSpaceCreateInfo;
import org.lwjgl.openxr.XrActionStateBoolean;
import org.lwjgl.openxr.XrActionStateFloat;
import org.lwjgl.openxr.XrActionStateGetInfo;
import org.lwjgl.openxr.XrActionStateVector2f;
import org.lwjgl.openxr.XrActionSuggestedBinding;
import org.lwjgl.openxr.XrActionsSyncInfo;
import org.lwjgl.openxr.XrActiveActionSet;
import org.lwjgl.openxr.XrBoundSourcesForActionEnumerateInfo;
import org.lwjgl.openxr.XrHandJointLocationEXT;
import org.lwjgl.openxr.XrHandJointLocationsEXT;
import org.lwjgl.openxr.XrHandJointsLocateInfoEXT;
import org.lwjgl.openxr.XrHandTrackerCreateInfoEXT;
import org.lwjgl.openxr.XrHandTrackerEXT;
import org.lwjgl.openxr.XrHapticActionInfo;
import org.lwjgl.openxr.XrHapticBaseHeader;
import org.lwjgl.openxr.XrHapticVibration;
import org.lwjgl.openxr.XrInstance;
import org.lwjgl.openxr.XrInteractionProfileSuggestedBinding;
import org.lwjgl.openxr.XrPosef;
import org.lwjgl.openxr.XrQuaternionf;
import org.lwjgl.openxr.XrReferenceSpaceCreateInfo;
import org.lwjgl.openxr.XrSession;
import org.lwjgl.openxr.XrSessionActionSetsAttachInfo;
import org.lwjgl.openxr.XrSpace;
import org.lwjgl.openxr.XrSpaceLocation;
import org.lwjgl.openxr.XrSpaceVelocity;
import org.lwjgl.openxr.XrVector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.openxr.EXTHandTracking;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This app state provides action based OpenXR calls (aka modern VR and AR).
 * <p>
 * See <a href="https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#_action_overview">khronos spec action_overview</a>
 */
public class XrActionAppState extends XrActionBaseAppState{

    private static final XrPosef identityPose;

    static{
        XrVector3f position = XrVector3f.calloc().set(0.0f, 0.0f, 0.0f);
        XrQuaternionf orientation = XrQuaternionf.calloc().set(0.0f, 0.0f, 0.0f, 1.0f);
        identityPose = XrPosef.create();
        identityPose.position$(position);
        identityPose.orientation(orientation);
    }

    boolean suppressRepeatedErrors = true;

    Set<String> errorsPreviouslyReported = new HashSet<>();

    public static final String ID = XrActionBaseAppState.ID;

    private static final Logger LOGGER = Logger.getLogger(XrActionAppState.class.getName());

    /**
     * A map of the action -> input -> handle for action space. Typically one for each hand.
     */
    private final Map<ActionHandle,Map<String, Long>> poseActionInputSpaceHandles = new HashMap<>();

    /**
     * A map of paths (e.g. /user/hand/right) to the handle used to address it.
     */
    private final Map<String, Long> pathCache = new HashMap<>();

    /**
     * Holds things like XR10.XR_REFERENCE_SPACE_TYPE_STAGE -> the memory handle of the reference space.
     */
    private final Map<Long, Long> referenceSpaceHandles = new HashMap<>();

    private XrSession xrSessionHandle;
    private XrInstance xrInstance;

    private OpenXrSessionManager openXRGL;

    private Map<String, XrActionSet> actionSets;

    /**
     * This is action set -> action name -> action
     */
    private Map<String, Map<String,XrAction>> actions;

    private final EnumMap<HandSide,XrHandTrackerEXT> handTrackers = new EnumMap<>(HandSide.class);

    /**
     * Contains the currently active profiles
     */
    private XrActionsSyncInfo xrActionsSyncInfo;

    private PendingActions pendingActions;

    List<Runnable> runAfterActionsRegistered = new ArrayList<>(0);

    List<Runnable> runAfterActionsSync = new ArrayList<>(0);

    XrAppState xrAppState;

    /**
     * Creates an OpenXrActionState with a single active action set (but with potentially more than one registered, ready to use later).
     * <p>
     * Registers an action manifest. An actions manifest is a file that defines "actions" a player can make. This will also contain suggestions at to what physical
     * buttons should be bound to the actions on directly supported controllers (other controllers may be configured by
     * the end user).
     * </p>
     * @param manifest a class describing all the actions (abstract versions of buttons, hand poses etc) available to the application
     * @param startingActionSet the name of the action set that should be activated at the start of the application (aka the one that will work)
     */
    public XrActionAppState(ActionManifest manifest, String startingActionSet){
        this(manifest, List.of(startingActionSet));
    }


    /**
     * Creates an OpenXrActionState with a single active action set (but with potentially more than one registered, ready to use later).
     * <p>
     * Registers an action manifest. An actions manifest is a file that defines "actions" a player can make.
     * (An action is an abstract version of a button press). This will also contain suggestions at to what physical
     * buttons should be bound to the actions on directly supported controllers (other controllers may be configured by
     * the end user).
     * </p>
     *
     * @param manifest a class describing all the actions (abstract versions of buttons, hand poses etc) available to the application
     * @param startingActionSets the names of the action sets that should be activated at the start of the application (aka the ones that will work)
     */
    public XrActionAppState(ActionManifest manifest, List<String> startingActionSets){
        super();
        if (startingActionSets.isEmpty()){
            throw new RuntimeException("No starting action sets specified, that means no actions will be usable. Probably not what you want. If you really really want that then set anything here then call setActiveActionSets() with no arguments later");
        }
        this.pendingActions = new PendingActions(manifest, startingActionSets);
    }


    @Override
    public void initialize(Application app){
        xrAppState = getState(XrAppState.ID, XrAppState.class);
        this.openXRGL = xrAppState.getXrSession();
        this.xrSessionHandle = openXRGL.getXrSession();
        this.xrInstance = xrSessionHandle.getInstance();
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

    @Override
    public List<PhysicalBindingInterpretation> getPhysicalBindingForAction(ActionHandle actionHandle){
        if (!isReady()){
            return List.of();
        }

        List<PhysicalBindingInterpretation> results = new ArrayList<>(1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            XrBoundSourcesForActionEnumerateInfo enumerateInfo = XrBoundSourcesForActionEnumerateInfo.malloc(stack)
                    .type$Default()
                    .next(0)
                    .action(obtainActionHandle(actionHandle));

            XrSession xrSession = xrAppState.getXrSession().getXrSession();

            IntBuffer countOutput = stack.mallocInt(1);
            checkResponseCode(XR10.xrEnumerateBoundSourcesForAction(xrSession, enumerateInfo, countOutput, null));

            int sourceCount = countOutput.get(0);

            LongBuffer sources = stack.mallocLong(sourceCount);
            checkResponseCode(XR10.xrEnumerateBoundSourcesForAction(xrSession, enumerateInfo, countOutput, sources));

            for (int i = 0; i < sourceCount; i++) {
                long sourcePath = sources.get(i);
                results.add(PhysicalBindingInterpretation.interpretRawValue(longToPath(sourcePath)));
            }
        }
        return results;
    }

    private boolean sessionFocussed(){
        return openXRGL.isSessionFocused();
    }


    /**
     * Registers an action manifest. An actions manifest is a file that defines "actions" a player can make.
     * (An action is an abstract version of a button press).
     * <p>
     * Note that this method should only be called after the state is initialised.
     * </p>
     * @param manifest a class describing all the actions (abstract versions of buttons, hand poses etc) available to the application
     * @param startingActionSets the names of the action sets that should be activated at the start of the application (aka the ones that will work)
     */
    private void registerActions(ActionManifest manifest, List<String> startingActionSets){
        //see https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#_action_overview for examples of these calls

        assert actionSets == null: "Actions have already been registered, consider using action sets and activating and deactivating them as required";

        if (startingActionSets.isEmpty()){
            LOGGER.log(Level.WARNING, "No starting action sets specified, that means no actions will be usable. Probably not what you want.");
        }

        LOGGER.log(Level.INFO, "Registering manifest");

        actionSets = new HashMap<>();
        actions = new HashMap<>();

        Map<List<String>, LongBuffer> subActionPaths = new HashMap<>();

        try (MemoryStack stack = stackPush()){

            for(ActionSet actionSet : manifest.getActionSets()){
                XrActionSetCreateInfo actionSetCreate = XrActionSetCreateInfo.calloc(stack);
                actionSetCreate.type$Default();
                actionSetCreate.actionSetName(stack.UTF8(actionSet.getName()));
                actionSetCreate.localizedActionSetName(stack.UTF8(actionSet.getTranslatedName()));
                actionSetCreate.priority(actionSet.getPriority());

                PointerBuffer actionSetPointer = stack.mallocPointer(1);
                checkResponseCode(
                        "Creating action set " + actionSet.getName() + " (" + actionSet.getTranslatedName() + ") p:" + actionSet.getPriority(),
                        XR10.xrCreateActionSet(xrInstance, actionSetCreate, actionSetPointer));

                XrActionSet xrActionSet = new XrActionSet(actionSetPointer.get(), xrInstance);
                actionSets.put(actionSet.getName(), xrActionSet);

                for(Action action : actionSet.getActions()){
                    XrActionCreateInfo xrActionCreateInfo = XrActionCreateInfo.calloc(stack);
                    xrActionCreateInfo.type$Default();
                    xrActionCreateInfo.next(NULL);
                    xrActionCreateInfo.actionName(stack.UTF8(action.getActionName()));
                    xrActionCreateInfo.actionType(action.getActionType().getOpenXrOption());
                    xrActionCreateInfo.localizedActionName(stack.UTF8(action.getTranslatedName()));


                    List<String> supportedSubActionPaths = action.getSupportedSubActionPaths();
                    if(!action.getSupportedSubActionPaths().isEmpty()){
                        LongBuffer subActionsLongBuffer = subActionPaths.computeIfAbsent(supportedSubActionPaths, paths -> {
                            LongBuffer standardSubActionPaths = BufferUtils.createLongBuffer(paths.size());
                            for(String path : paths){
                                standardSubActionPaths.put(pathToLong(path, true));
                            }
                            return standardSubActionPaths;
                        });
                        subActionsLongBuffer.rewind();
                        xrActionCreateInfo.subactionPaths(subActionsLongBuffer);
                    }
                    xrActionCreateInfo.countSubactionPaths(supportedSubActionPaths.size());

                    PointerBuffer actionPointer = stack.mallocPointer(1);
                    withResponseCodeLogging("xrStringToPath", XR10.xrCreateAction(xrActionSet, xrActionCreateInfo, actionPointer));
                    XrAction xrAction = new XrAction(actionPointer.get(), xrActionSet);
                    actions.computeIfAbsent(actionSet.getName(), name -> new HashMap<>()).put(action.getActionName(), xrAction);

                    if(action.getActionType() == ActionType.POSE){
                        if(action.getSupportedSubActionPaths().isEmpty()){
                            LOGGER.warning(actionSet.getName() + ":" + action.getActionName() + " is a pose action but does not have any sub action paths");
                        }
                        for(String input : action.getSupportedSubActionPaths()){

                            XrActionSpaceCreateInfo actionSpaceCreateInfo = XrActionSpaceCreateInfo.create()
                                    .type$Default()
                                    .action(xrAction)
                                    .poseInActionSpace(XrPosef.malloc(stack)
                                            .position$(XrVector3f.calloc(stack)
                                                    .set(0, 0, 0))
                                            .orientation(XrQuaternionf.malloc(stack)
                                                    .x(0).y(0).z(0).w(1)))
                                    .subactionPath(pathToLong(input, true));
                            PointerBuffer spacePointer = stack.mallocPointer(1);

                            withResponseCodeLogging("Create pose space", XR10.xrCreateActionSpace(xrSessionHandle, actionSpaceCreateInfo, spacePointer));

                            poseActionInputSpaceHandles.computeIfAbsent(new ActionHandle(action.getActionSetName(), action.getActionName()), key -> new HashMap<>()).put(input, spacePointer.get(0));
                        }

                    }
                }
            }

            Collection<SuggestedBindingsProfileView> suggestedBindingsGroupedByProfile = manifest.getSuggestedBindingsGroupedByProfile();

            for(SuggestedBindingsProfileView profile : suggestedBindingsGroupedByProfile){
                long deviceProfileHandle = pathToLong(profile.getProfileName(), false);

                Set<Map.Entry<SuggestedBindingsProfileView.ActionData, String>> suggestedBindings = profile.getSetToActionToBindingMap().entrySet();
                XrActionSuggestedBinding.Buffer suggestedBindingsBuffer = XrActionSuggestedBinding.create(suggestedBindings.size());

                Iterator<Map.Entry<SuggestedBindingsProfileView.ActionData, String>> suggestedBindingIterator = suggestedBindings.iterator();
                for(int i = 0; i < suggestedBindings.size(); i++){
                    Map.Entry<SuggestedBindingsProfileView.ActionData, String> actionAndBinding = suggestedBindingIterator.next();
                    LongBuffer bindingHandleBuffer = BufferUtils.createLongBuffer(1);
                    withResponseCodeLogging("xrStringToPath:" + actionAndBinding.getValue(), XR10.xrStringToPath(xrInstance, actionAndBinding.getValue(), bindingHandleBuffer));

                    XrAction action = actions.get(actionAndBinding.getKey().getActionSet()).get(actionAndBinding.getKey().getActionName());
                    suggestedBindingsBuffer.position(i);
                    suggestedBindingsBuffer.action(action);
                    suggestedBindingsBuffer.binding(bindingHandleBuffer.get());
                }
                suggestedBindingsBuffer.position(0); //reset ready for reading

                XrInteractionProfileSuggestedBinding xrInteractionProfileSuggestedBinding = XrInteractionProfileSuggestedBinding.calloc(stack)
                        .type$Default()
                        .interactionProfile(deviceProfileHandle)
                        .suggestedBindings(suggestedBindingsBuffer);

                withResponseCodeLogging("xrSuggestInteractionProfileBindings", XR10.xrSuggestInteractionProfileBindings(xrInstance, xrInteractionProfileSuggestedBinding));
            }

            PointerBuffer actionSetsBuffer = stack.callocPointer(actionSets.size());

            actionSets.values().forEach(actionSet -> actionSetsBuffer.put(actionSet.address()));
            actionSetsBuffer.flip();  // Reset the position back to the start of the buffer

            XrSessionActionSetsAttachInfo actionSetsAttachInfo = XrSessionActionSetsAttachInfo.create();
            actionSetsAttachInfo.type$Default();
            actionSetsAttachInfo.actionSets(actionSetsBuffer);
            withResponseCodeLogging("xrAttachSessionActionSets", XR10.xrAttachSessionActionSets(xrSessionHandle, actionSetsAttachInfo));

            setActiveActionSets(startingActionSets);

            if(xrAppState.checkExtensionLoaded(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME)){
                for(HandSide handSide : HandSide.values()){
                    XrHandTrackerCreateInfoEXT createHandTracking = XrHandTrackerCreateInfoEXT.calloc(stack)
                            .type(EXTHandTracking.XR_TYPE_HAND_TRACKER_CREATE_INFO_EXT)
                            .hand(handSide.skeletonIndex) // Indicate which hand to track
                            .handJointSet(EXTHandTracking.XR_HAND_JOINT_SET_DEFAULT_EXT); // Use the default hand joint set

                    PointerBuffer handTrackingPointerBuffer = BufferUtils.createPointerBuffer(1);

                    // some runtimes (e.g. Monado with HTV Vive) report that they have the EXT_HAND_TRACKING_EXTENSION but
                    // then report unsupported when you try to use it. Gracefully accept that and don't crash
                    boolean success = withResponseCodeLogging("Setup hand tracking", EXTHandTracking.xrCreateHandTrackerEXT(xrSessionHandle, createHandTracking, handTrackingPointerBuffer));
                    if (success){
                        XrHandTrackerEXT handTrackerEXT = new XrHandTrackerEXT(handTrackingPointerBuffer.get(), xrSessionHandle);
                        handTrackers.put(handSide, handTrackerEXT);
                    }else{
                        LOGGER.warning("XR_EXT_hand_tracking not *actually* available, correcting" );
                        xrAppState.getExtensionsLoaded().put(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME, false);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void setActiveActionSets(List<String> actionSets){

        List<XrActionSet> activeActionSets = new ArrayList<>(actionSets.size());

        for(String actionSet : actionSets){
            XrActionSet actionSetXr = this.actionSets.get(actionSet);
            if(actionSetXr==null){
                throw new RuntimeException("Action set not found: " + actionSet);
            }
            activeActionSets.add(actionSetXr);
        }

        this.xrActionsSyncInfo = XrActionsSyncInfo.create();
        this.xrActionsSyncInfo.type$Default();
        this.xrActionsSyncInfo.countActiveActionSets(activeActionSets.size());
        XrActiveActionSet.Buffer activeActionSetsBuffer = XrActiveActionSet.calloc(activeActionSets.size());
        for(XrActionSet activeActionSet : activeActionSets){
            activeActionSetsBuffer.actionSet(activeActionSet);
        }
        for(int i=0; i<activeActionSets.size(); i++){
            activeActionSetsBuffer.position(i);
            activeActionSetsBuffer.actionSet(activeActionSets.get(i));
        }
        activeActionSetsBuffer.position(0);
        this.xrActionsSyncInfo.activeActionSets(activeActionSetsBuffer);
    }

    private static ByteBuffer stringToByte(String str){
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] nullTerminatedBytes = new byte[strBytes.length + 1];
        System.arraycopy(strBytes, 0, nullTerminatedBytes, 0, strBytes.length);
        nullTerminatedBytes[nullTerminatedBytes.length - 1] = 0;  // add null terminator
        ByteBuffer buffer = BufferUtils.createByteBuffer(nullTerminatedBytes.length);
        buffer.put(nullTerminatedBytes);
        buffer.rewind();
        return buffer;
    }

    /**
     * @param eventText A context string
     * @param errorCode the error code returned by openXR
     * @return if it's a success
     */
    private boolean withResponseCodeLogging(String eventText, int errorCode){
        //error code 0 is ultra common and means all is well. Don't flood the logs with it
        if (errorCode != XR10.XR_SUCCESS){
            ByteBuffer buffer = BufferUtils.createByteBuffer(XR10.XR_MAX_RESULT_STRING_SIZE);
            XR10.xrResultToString(xrInstance, errorCode, buffer);

            String message = errorCode + " " + MemoryUtil.memUTF8(buffer, MemoryUtil.memLengthNT1(buffer))+ " occurred during " + eventText+ ". ";

            if (errorCode<0){

                if (!suppressRepeatedErrors || !errorsPreviouslyReported.contains(message)){
                    errorsPreviouslyReported.add(message);

                    message += CallResponseCode.getResponseCode(errorCode).map(CallResponseCode::getErrorMessage).orElse("");

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    new Throwable(message).printStackTrace(pw);
                    LOGGER.warning(sw.toString());

                    if (suppressRepeatedErrors){
                        LOGGER.warning("Further identical errors will be suppressed. If you don't want that call doNotSupressRepeatedErrors()");
                    }
                }
            }else{
                if (LOGGER.isLoggable(Level.INFO)){
                    LOGGER.info(message+CallResponseCode.getResponseCode(errorCode).map(CallResponseCode::getErrorMessage).orElse(""));
                }
            }
            return false;
        }
        return true;
    }

    private void checkResponseCode(String context, int errorCode){
        openXRGL.checkResponseCode(context, errorCode);
    }

    private void checkResponseCode(int errorCode){
        openXRGL.checkResponseCode(errorCode);
    }

    @Override
    public void doNotSuppressRepeatedErrors(){
        suppressRepeatedErrors = false;
    }

    @Override
    public BooleanActionState getBooleanActionState(ActionHandle action, String restrictToInput){
        if (!isReady()){
            return new BooleanActionState(false, false);
        }

        XrActionStateBoolean actionState = XrActionStateBoolean.create();
        actionState.type$Default();
        XrActionStateGetInfo actionInfo = XrActionStateGetInfo.create();
        actionInfo.type$Default();
        actionInfo.action(obtainActionHandle(action));

        if (restrictToInput != null){
            actionInfo.subactionPath(pathToLong(restrictToInput, true));
        }

        int responseCode = XR10.xrGetActionStateBoolean(xrSessionHandle, actionInfo, actionState);
        if(responseCode == XR10.XR_ERROR_ACTION_TYPE_MISMATCH){
            throw new IncorrectActionTypeException("Called for the boolean action but the action is not of that type");
        }
        checkResponseCode("getActionState", responseCode);

        return new BooleanActionState(actionState.currentState(), actionState.changedSinceLastSync());
    }

    private XrAction obtainActionHandle(ActionHandle actionHandle){
        try{
            return actions.get(actionHandle.actionSetName()).get(actionHandle.actionName());
        }catch(NullPointerException nullPointerException){
            throw new RuntimeException("No action found for " + actionHandle + ". Have you registered it in the manifest?", nullPointerException);
        }
    }

    @Override
    public Optional<PoseActionState> getPose_worldRelative(ActionHandle actionName, HandSide handSide){

        Optional<PoseActionState> rawPoseOpt = getPose(actionName, handSide, true);
        return rawPoseOpt.map(pose -> {
            Node observer = xrAppState.getObserver();
            Quaternion observerRotation = observer.getWorldRotation();

            Vector3f velocity_vrWorld = observerRotation.mult(pose.velocity());
            Vector3f angularVelocity_vrWorld = observerRotation.mult(pose.angularVelocity());

            Vector3f localPosition = observer.localToWorld(pose.position(), null);

            Quaternion localRotation = observer.getWorldRotation().mult(pose.orientation());
            return new PoseActionState(localPosition, localRotation, velocity_vrWorld, angularVelocity_vrWorld);
        });

    }

    @Override
    public Optional<PoseActionState> getPose(ActionHandle action, HandSide handSide, boolean stageRelative){
        if (!isReady()){
            return Optional.empty();
        }

        long predictedTime = openXRGL.getPredictedFrameTime();
        if (predictedTime==0){
            //not set up yet
            return Optional.empty();
        }

        XrSpaceVelocity spaceVelocity = XrSpaceVelocity.create()
                .type$Default();

        XrSpaceLocation spaceLocation = XrSpaceLocation.create()
                .type$Default()
                .next(spaceVelocity);

        long spaceHandle;
        try{
            spaceHandle = poseActionInputSpaceHandles.get(action).get(handSide.restrictToInputString);
        } catch(NullPointerException nullPointerException){
            throw new RuntimeException("No pose action found for " + action + " and handSide " + handSide + ". Have you registered in it the manifest?", nullPointerException);
        }

        XrSpace poseSpace = new XrSpace(spaceHandle, xrSessionHandle);
        long handleForReferenceSpace = getOrCreateReferenceSpaceHandle(stageRelative?XR10.XR_REFERENCE_SPACE_TYPE_STAGE:XR10.XR_REFERENCE_SPACE_TYPE_LOCAL);
        XrSpace relativeToSpace = new XrSpace(handleForReferenceSpace, xrSessionHandle);
        withResponseCodeLogging("getPose", XR10.xrLocateSpace(poseSpace, relativeToSpace, predictedTime, spaceLocation));

        long locationFlags = spaceLocation.locationFlags();
        if ((locationFlags & XR10.XR_SPACE_LOCATION_POSITION_VALID_BIT) != 0 &&
                (locationFlags & XR10.XR_SPACE_LOCATION_ORIENTATION_VALID_BIT) != 0) {
            // The pose is valid. You can use spaceLocation.pose() to get the position and orientation of the hand.
            XrPosef handPose = spaceLocation.pose();
            Vector3f position = xrVector3fToJME(handPose.position$());
            Quaternion rotation = xrQuaternionToJme(handPose.orientation());

            long velocityFlags = spaceVelocity.velocityFlags();
            if ((velocityFlags & XR10.XR_SPACE_VELOCITY_ANGULAR_VALID_BIT) != 0 && (velocityFlags & XR10.XR_SPACE_VELOCITY_LINEAR_VALID_BIT) != 0) {
                // full data available, yay!
                return Optional.of(new PoseActionState(position, rotation, xrVector3fToJME(spaceVelocity.linearVelocity()), xrVector3fToJME(spaceVelocity.angularVelocity())));
            }else{
                //fall back to just the position data
                return Optional.of(new PoseActionState(position, rotation));
            }
        } else {
            // The pose is not valid. The hand may be out of tracking range. Probably fine
            if (LOGGER.isLoggable(Level.FINE)){
                LOGGER.fine("Hand pose is not valid. But may just be out of tracking range.");
            }
            return Optional.empty();
        }
    }

    /**
     * Gets the joint positions of the hand in the coordinate system defined by the pose (So if the pose is a grip
     * pose it's relative to the grip, if it's the aim pose its relative to the aim).
     * <p>
     * It's only really a good idea to call this if the pose fetch has already succeeded
     * </p>
     * @param poseAction the pose (just for the coordinate system)
     * @param handSide the handside to get the joint positions for
     */
    public Optional<Map<HandJoint, BonePose>> getSkeleton(ActionHandle poseAction, HandSide handSide){
        if (!isReady()){
            return Optional.empty();
        }

        long predictedTime = openXRGL.getPredictedFrameTime();
        if (predictedTime==0){
            //not set up yet
            return Optional.empty();
        }

        long spaceHandle;
        try{
            spaceHandle =  poseActionInputSpaceHandles.get(poseAction).get(handSide.restrictToInputString);
        } catch(NullPointerException nullPointerException){
            throw new RuntimeException("No pose action found for " + poseAction + " and handSide " + handSide + ". Have you registered it in the manifest?", nullPointerException);
        }


        XrSpace poseSpace = new XrSpace(spaceHandle, xrSessionHandle);

        XrHandJointLocationsEXT handJointLocations = XrHandJointLocationsEXT.create()
                .type$Default()
                .jointLocations(XrHandJointLocationEXT.create(EXTHandTracking.XR_HAND_JOINT_COUNT_EXT));

        XrHandJointsLocateInfoEXT locateInfo = XrHandJointsLocateInfoEXT.create()
                .type$Default()
                .baseSpace(poseSpace)
                .time(predictedTime);
        if (handTrackers.containsKey(handSide)){
            Map<HandJoint, BonePose> results = new HashMap<>();

            withResponseCodeLogging("Get joint locations",EXTHandTracking.xrLocateHandJointsEXT(handTrackers.get(handSide), locateInfo, handJointLocations));

            XrHandJointLocationEXT.Buffer xrHandJointLocationEXTS = handJointLocations.jointLocations();

            for(HandJoint joint : HandJoint.values()){
                XrHandJointLocationEXT xrHandJointLocationEXT = xrHandJointLocationEXTS.get(joint.getJointIndex());
                results.put(joint, new BonePose(
                        xrVector3fToJME(xrHandJointLocationEXT.pose().position$()),
                        xrQuaternionToJme(xrHandJointLocationEXT.pose().orientation()),
                        xrHandJointLocationEXT.radius())
                );
            }
            return Optional.of(results);
        }else {
            LOGGER.warning("No hand tracker for handSide " + handSide);
            return Optional.empty();
        }
    }

    @Override
    public FloatActionState getFloatActionState(ActionHandle action, String restrictToInput ){
        if (!isReady()){
            return new FloatActionState(0, false);
        }

        XrActionStateFloat actionState = XrActionStateFloat.create();
        actionState.type$Default();
        XrActionStateGetInfo actionInfo = XrActionStateGetInfo.create();
        actionInfo.type$Default();
        actionInfo.action(obtainActionHandle(action));

        if (restrictToInput != null){
            actionInfo.subactionPath(pathToLong(restrictToInput, true));
        }
        int responseCode = XR10.xrGetActionStateFloat(xrSessionHandle, actionInfo, actionState);
        if(responseCode == XR10.XR_ERROR_ACTION_TYPE_MISMATCH){
            throw new IncorrectActionTypeException("Called for the float action but the action is not of that type");
        }
        checkResponseCode("getActionState", responseCode);
        return new FloatActionState(actionState.currentState(), actionState.changedSinceLastSync());
    }

    @Override
    public Vector2fActionState getVector2fActionState(ActionHandle action){
        return getVector2fActionState(action, null);
    }

    @Override
    public Vector2fActionState getVector2fActionState(ActionHandle action, String restrictToInput ){

        if (!isReady()){
            return new Vector2fActionState(0, 0, false);
        }

        XrActionStateVector2f actionState = XrActionStateVector2f.create();
        actionState.type$Default();
        XrActionStateGetInfo actionInfo = XrActionStateGetInfo.create();
        actionInfo.type$Default();
        actionInfo.action(obtainActionHandle(action));
        if (restrictToInput != null){
            actionInfo.subactionPath(pathToLong(restrictToInput, true));
        }

        int responseCode = XR10.xrGetActionStateVector2f(xrSessionHandle, actionInfo, actionState);
        if(responseCode == XR10.XR_ERROR_ACTION_TYPE_MISMATCH){
            throw new IncorrectActionTypeException("Called for the Vector2f action but the action is not of that type");
        }
        checkResponseCode("getVector2fActionState", responseCode);

        return new Vector2fActionState(actionState.currentState().x(), actionState.currentState().y(), actionState.changedSinceLastSync());
    }

    @Override
    public void triggerHapticAction(ActionHandle actionHandle, float duration, float frequency, float amplitude){
        triggerHapticAction( actionHandle, duration, frequency, amplitude, null );
    }

    @Override
    public void triggerHapticAction(ActionHandle action, float duration, float frequency, float amplitude, String restrictToInput ){
        if (!isReady()){
            return;
        }

        XrHapticVibration vibration = XrHapticVibration.create()
                .type$Default()
                .duration((long)(duration * 1_000_000_000))  // Duration in nanoseconds
                .frequency(frequency)
                .amplitude(amplitude);  // Amplitude in normalized units

        XrHapticActionInfo hapticActionInfo = XrHapticActionInfo.create()
                .type$Default()
                .action(obtainActionHandle(action));

        if (restrictToInput!=null){
            hapticActionInfo.subactionPath(pathToLong(restrictToInput, true));
        }
        XrHapticBaseHeader hapticBaseHeader = XrHapticBaseHeader.create(vibration);

        withResponseCodeLogging("Haptic Vibration", XR10.xrApplyHapticFeedback(xrSessionHandle, hapticActionInfo, hapticBaseHeader));
    }

    @Override
    public void update(float tpf){
        super.update(tpf);
        if (!sessionFocussed()){
            return;
        }

        if (pendingActions !=null){
            registerActions(pendingActions.pendingActionSets(), pendingActions.pendingActiveActionSetNames());
            pendingActions = null;
            runAfterActionsRegistered.forEach(Runnable::run);
            runAfterActionsRegistered=List.of();
        }

        if (xrActionsSyncInfo !=null){
            boolean success = withResponseCodeLogging("xrSyncActions", XR10.xrSyncActions(xrSessionHandle, this.xrActionsSyncInfo));

            if (success && !runAfterActionsSync.isEmpty()){
                runAfterActionsSync.forEach(Runnable::run);
                runAfterActionsSync.clear();
            }
        }
    }

    @Override
    public boolean isReady(){
        return actions!=null;
    }

    @Override
    public void runAfterActionsRegistered(Runnable runnable){
        if (pendingActions != null){
            runAfterActionsRegistered.add(runnable);
        }else{
            runnable.run();
        }
    }

    @Override
    public void runAfterNextActionsSync(Runnable runnable){
         runAfterActionsSync.add(runnable);
    }

    /**
     * @param referenceSpaceEnum Things like XR10.XR_REFERENCE_SPACE_TYPE_STAGE
     */
    private long getOrCreateReferenceSpaceHandle(long referenceSpaceEnum){
        if (this.referenceSpaceHandles.containsKey(referenceSpaceEnum)){
            return referenceSpaceHandles.get(referenceSpaceEnum);
        }

        XrReferenceSpaceCreateInfo spaceInfo = XrReferenceSpaceCreateInfo.create()
                .type$Default()
                .referenceSpaceType(XR10.XR_REFERENCE_SPACE_TYPE_STAGE)
                .poseInReferenceSpace(identityPose);
        PointerBuffer space = BufferUtils.createPointerBuffer(1);
        withResponseCodeLogging("Get space for " +referenceSpaceEnum, XR10.xrCreateReferenceSpace(xrSessionHandle, spaceInfo, space));
        long handle = space.get(0);
        referenceSpaceHandles.put(referenceSpaceEnum, handle);

        return handle;
    }

    private long pathToLong(String path, boolean cache){
        if (cache){
            Long handle = pathCache.get(path);
            if (handle!= null){
                return handle;
            }
        }

        LongBuffer pathHandleBuffer = BufferUtils.createLongBuffer(1);
        withResponseCodeLogging("xrStringToPath:"+path, XR10.xrStringToPath(xrInstance, path, pathHandleBuffer));
        long pathHandle = pathHandleBuffer.get(0);
        if (cache){
            pathCache.put(path, pathHandle);
        }
        return pathHandle;
    }

    private String longToPath(long pathHandle){
        try (MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer stringLengthUsed = stack.mallocInt(1);

            checkResponseCode(XR10.xrPathToString(xrInstance, pathHandle, stringLengthUsed, null));

            ByteBuffer pathStringBuffer = stack.malloc(stringLengthUsed.get(0));

            checkResponseCode(XR10.xrPathToString(xrInstance, pathHandle, stringLengthUsed, pathStringBuffer));

            return MemoryUtil.memUTF8(pathStringBuffer, stringLengthUsed.get(0) - 1);
        }
    }

    private Vector3f xrVector3fToJME(XrVector3f in){
        return new Vector3f(in.x(), in.y(), in.z());
    }

    private Quaternion xrQuaternionToJme(XrQuaternionf in){
        return new Quaternion(in.x(), in.y(), in.z(), in.w());
    }

    /**
     *
     * Given a hand armature with names as defined in the passed boneNameMappings
     * it will use the bone stances (presumably from an ealier call to get the actions) to set the armature positions.
     * <p>
     * See <a href="https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XR_EXT_hand_tracking">Hand tracking spec</a>
     * <p>
     * NOTE: the bone orientation is surprising and non-natural. If you build a hand model, and it appears
     * distorted try importing the bones from Tamarin's starter blender file
     *
     * @param armature a JMonkey armature (aka set of bones)
     * @param boneStances the bone positions (as reported by OpenXR)
     * @param boneNameMappings the bone names to use. This is a map from the HandJoint enum to the bone name
     */
    public static void updateHandSkeletonPositions(Armature armature, Map<HandJoint, BonePose> boneStances, Map<HandJoint, String> boneNameMappings){
        for(Map.Entry<HandJoint, BonePose> bonePose : boneStances.entrySet()){
            String boneName = boneNameMappings.get(bonePose.getKey());
            Joint joint = armature.getJoint(boneName);
            if (joint!=null){
                joint.setLocalTranslation(bonePose.getValue().position());
                joint.setLocalRotation(bonePose.getValue().orientation());
            }
        }
    }

    public static class IncorrectActionTypeException extends RuntimeException{
        public IncorrectActionTypeException(String message){
            super(message);
        }
    }

    private record PendingActions(
        ActionManifest pendingActionSets,
        List<String> pendingActiveActionSetNames
    ){}

}
