package com.onemillionworlds.tamarin.debug;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;
import com.onemillionworlds.tamarin.actions.XrActionBaseAppState;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.actions.state.PoseActionState;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.viewports.AdditionalViewportRequest;
import com.onemillionworlds.tamarin.viewports.ViewportConfigurator;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.VRHandsAppState;
import org.lwjgl.openxr.EXTHandTracking;

import java.util.HashMap;
import java.util.List;
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
        viewportConfigurator = getState(XrBaseAppState.ID, XrBaseAppState.class).addAdditionalViewport(AdditionalViewportRequest.builder(overlayRootNode).build());

        perHandData.put(HandSide.LEFT, new PerHandData());
        perHandData.put(HandSide.RIGHT, new PerHandData());
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        boolean skeletonAvailable = getState(XrBaseAppState.ID, XrBaseAppState.class).checkExtensionLoaded(EXTHandTracking.XR_EXT_HAND_TRACKING_EXTENSION_NAME);

        VRHandsAppState vrHandsAppState = getState(VRHandsAppState.ID, VRHandsAppState.class);
        XrActionBaseAppState openXrActionState = getState(XrActionBaseAppState.ID, XrActionBaseAppState.class);

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

            if (turnedOnDebugItems.contains(HandDebugItems.PALM_PICK_POINTS)){
                Node palmNode = boundHand.getPalmNode();
                List<Vector3f> palmPickPoints = boundHand.getPalmPickPoints();
                for(int i = 0; i<palmPickPoints.size(); i++ ){
                    Vector3f worldPosition = palmNode.localToWorld(palmPickPoints.get(i), null);
                    handData.setPalmPickSphere(i, worldPosition, boundHand.getPalmPickSphereRadius());
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

        private final Map<Integer, Spatial> palmPickSpheres = new HashMap<>();
        private final Map<Integer, Float> palmPickRadiuses = new HashMap<>();

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

        public void setPalmPickSphere(int index, Vector3f worldPosition, float radius){
            // this rebuilds each time in case the radius has changed (a bit ineffient, but
            // it's just for debug

            Float oldSize = palmPickRadiuses.get(index);
            Spatial sphere = palmPickSpheres.get(index);
            if (oldSize ==null || oldSize !=radius){
                if (sphere!=null){
                    sphere.removeFromParent();
                }
                ColorRGBA colour = HandDebugItems.PALM_PICK_POINTS.getColorRGBA();
                sphere = microSphere(colour, radius);
                overlayRootNode.attachChild(sphere);
                palmPickSpheres.put(index,sphere);
                palmPickRadiuses.put(index, radius);
            }
            sphere.setLocalTranslation(worldPosition);
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

    private Geometry microSphere(ColorRGBA colorRGBA, float radius){
        Sphere line = new Sphere(10,10, radius);
        Geometry geometry = new Geometry("debugHandSphere", line);
        Material material = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setLineWidth(5);
        material.setColor("Color", colorRGBA);
        material.getAdditionalRenderState().setWireframe(true);
        geometry.setMaterial(material);
        geometry.setUserData(BoundHand.NO_PICK, true);
        return geometry;
    }
}
