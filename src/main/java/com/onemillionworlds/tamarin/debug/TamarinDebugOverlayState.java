package com.onemillionworlds.tamarin.debug;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.OpenXrActionState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.openxr.XrAppState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import org.lwjgl.openxr.EXTHandTracking;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TamarinDebugOverlayState extends BaseAppState{

    public static final String ID = "TamarinDebugOverlayState";

    Set<HandDebugItems> turnedOnDebugItems;

    public TamarinDebugOverlayState(){
        this(HandDebugItems.values());
    }
    public TamarinDebugOverlayState(HandDebugItems... itemsToRender){
        super(ID);
        turnedOnDebugItems = Set.of(itemsToRender);
    }

    Node overlayRootNode = new Node("TamarinDebugOverlayStateRootNode");
    ViewportConfigurator viewportConfigurator;

    Map<HandSide, PerHandData> perHandData = new HashMap<>();

    @Override
    protected void initialize(Application app){
        viewportConfigurator = getState(XrAppState.ID, XrAppState.class).addAdditionalViewport(AdditionalViewportRequest.builder(overlayRootNode).build());

        perHandData.put(HandSide.LEFT, new PerHandData());
        perHandData.put(HandSide.RIGHT, new PerHandData());
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        boolean skeletonAvailable = getState(XrAppState.ID, XrAppState.class).checkExtensionLoaded(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME);

        VRHandsAppState vrHandsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
        OpenXrActionState openXrActionState = getState(OpenXrActionState.ID, OpenXrActionState.class);

        for(BoundHand boundHand : vrHandsAppState.getHandControls()){
            PerHandData handData = perHandData.get(boundHand.getHandSide());
            ActionHandle handPoseAction = boundHand.getHandPoseActionName();

            Optional<PoseActionState> pose = openXrActionState.getPose_worldRelative(handPoseAction, boundHand.getHandSide());

            pose.ifPresent(p -> {
                handData.debugPointsNode.setLocalTranslation(p.position());
                handData.debugPointsNode.setLocalRotation(p.orientation());
            });

            //do the non special case debug items
            for(HandDebugItems debugItem : HandDebugItems.values()){
                if (debugItem.isSpecialCase()){
                    continue;
                }
                if (turnedOnDebugItems.contains(debugItem)){
                    Node debugNode = handData.getOrBuildNodeForDebugPoint(debugItem);
                    debugNode.setLocalTranslation(debugItem.getDebugItemCreator().apply(boundHand).getWorldTranslation());
                    debugNode.setLocalRotation(debugItem.getDebugItemCreator().apply(boundHand).getWorldRotation());
                }
            }

            if (skeletonAvailable && turnedOnDebugItems.contains(HandDebugItems.SKELETON)){
                Optional<Map<HandJoint, BonePose>> skeletonOpt = openXrActionState.getSkeleton(handPoseAction, boundHand.getHandSide());
                if (skeletonOpt.isPresent()){
                    Map<HandJoint, BonePose> skeleton = skeletonOpt.get();
                    for(HandJoint joint : HandJoint.values()){
                        Node jointNode = handData.getOrBuildNodeForJoint(joint);
                        BonePose bonePose = skeleton.get(joint);
                        jointNode.setLocalTranslation(bonePose.position());
                        jointNode.setLocalRotation(bonePose.orientation());
                    }
                }
            }
        }
        overlayRootNode.updateLogicalState(tpf);
        overlayRootNode.updateGeometricState();

    }

    @Override
    protected void cleanup(Application app){
        if (viewportConfigurator!=null){
            viewportConfigurator.removeViewports();
            viewportConfigurator = null;
        }
    }

    @Override protected void onEnable(){}
    @Override protected void onDisable(){}

    private class PerHandData{
        private final Node debugPointsNode = new Node();
        private final Map<HandJoint, Node> jointPositions = new HashMap<>(HandJoint.values().length);

        private final Map<HandDebugItems, Node> otherDebugPoints = new HashMap<>();

        public PerHandData(){
            overlayRootNode.attachChild(debugPointsNode);
        }

        public Node getOrBuildNodeForJoint(HandJoint joint){
            return jointPositions.computeIfAbsent(joint, j -> {
                ColorRGBA colour = joint == HandJoint.WRIST_EXT ? ColorRGBA.Blue : ColorRGBA.White;
                Node jointNode = boxWithAxisLines(j.name(), colour, 0.002f);
                debugPointsNode.attachChild(jointNode);
                return jointNode;
            });
        }

        public Node getOrBuildNodeForDebugPoint(HandDebugItems debugItem){
            return otherDebugPoints.computeIfAbsent(debugItem, j -> {
                ColorRGBA colour = debugItem.getColorRGBA();
                Node jointNode = boxWithAxisLines(j.name(), colour, 0.004f);
                overlayRootNode.attachChild(jointNode);
                return jointNode;
            });
        }

    }

    private Node boxWithAxisLines(String name, ColorRGBA boxColor, float boxSize){
        Node node = new Node(name);
        node.attachChild(microBox(boxColor, boxSize));
        node.attachChild(microLine(ColorRGBA.Green, new Vector3f(boxSize * 3, 0, 0)));
        node.attachChild(microLine(ColorRGBA.Yellow, new Vector3f(0, boxSize * 3 , 0)));
        node.attachChild(microLine(ColorRGBA.Red, new Vector3f(0, 0, boxSize * 3)));
        return node;
    }

    private Geometry microBox(ColorRGBA colorRGBA, float size){
        Box b = new Box(size, size, size);
        Geometry geom = new Geometry("debugHandBox", b);
        Material mat = new Material(getApplication().getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", colorRGBA);
        mat.getAdditionalRenderState().setWireframe(true);
        geom.setMaterial(mat);
        geom.setUserData(BoundHand.NO_PICK, true);
        return geom;
    }

    private Geometry microLine(ColorRGBA colorRGBA, Vector3f vector){
        Line line = new Line(new Vector3f(0, 0, 0), vector);
        Geometry geometry = new Geometry("debugHandLine", line);
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setLineWidth(5);
        material.setColor("Color", colorRGBA);
        geometry.setMaterial(material);
        geometry.setUserData(BoundHand.NO_PICK, true);
        return geometry;
    }
}
