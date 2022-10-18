package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.app.SimpleApplication;
import com.jme3.app.VRAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.TamarinUtilities;
import com.onemillionworlds.tamarin.compatibility.ActionBasedOpenVrState;
import com.onemillionworlds.tamarin.compatibility.DigitalActionState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.HandSide;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A hand ring menu function has a menu of geometries that are opened from an action and then the hand is
 * moved through them to select them.
 *
 * The menu can be a tree, where passing the hand through 1 item opens up a new set of branches
 *
 * The menu remains open for as long as the digital action remains held. When the action is released where the hand is
 * controls what is selected (if anything)
 */
public class HandRingMenuFunction<T> implements BoundHandFunction{

    private static final String MENU_BRANCH_PATH = "MENU_BRANCH_PATH";

    private final List<MenuItem<T>> topLevelMenuItems;
    private List<MenuBranch<T>> currentOpenPath = List.of();

    private final Consumer<Optional<T>> selectionConsumer;

    String digitalActionToOpenMenu;

    private BoundHand boundHand;
    private ActionBasedOpenVrState actionBasedOpenVrState;
    private VRAppState vrAppState;
    Node menuNode = new Node("MenuNode");
    private boolean menuOpen = false;

    /**
     * For selecting leaves this is the futhest the palm can be from the centre and still be considered a select
     */
    @Setter
    private float maximumSelectRange = 0.1f;
    /**
     * This records a set of menu item paths (The great grandparent -> grandparent -> parent etc) and the ring
     * centre node that should be shown if that path is active
     */
    private final Map<ArrayList<MenuBranch<T>>, Node> subRingCentreNodes = new HashMap<>();


    /**
     * For the first ring of items determines where they should be placed. Default behaviour is to use the top
     * 3 quarters, starting on the left, evenly spaced.
     *
     * Zero radians is straight up, negative is left
     */
    @Setter
    private InnerRingPositioner innerRingItemPositioner = (index, items) -> -0.75f * FastMath.PI + (float)(index * 1.5f*Math.PI/(items-1));

    /**
     * For the subcategory rings (which can have children of children of children etc) this function determines the angle
     * at which the child members should be.
     *
     * Zero radians is straight up, negative is left
     */
    @Setter
    private ChildRingPositioner childRingPositioner = this::defaultChildRingPositioner;

    /**
     * Where the centre of the first ring is
     */
    @Setter
    @Getter
    private float firstRingRadius = 0.15f;

    /**
     * Distance between ring 1, ring 2, ring 3 etc
     */
    @Setter
    @Getter
    private float interRingDistance = 0.15f;

    private final Collection<ItemPositionData> leafPositions = new HashSet<>();

    /**
     * Dynamic geometries update every time the menu is opened.
     */
    Map<Node, Supplier<Spatial>> dynamicGeometrySuppliers = new HashMap<>();

    /**
     * @param menuItems the tree of menu items
     * @param selectionConsumer when an item is selected it is given to this consumer
     * @param digitalActionToOpenMenu The digital action (button press) that opens the menu
     */
    public HandRingMenuFunction(List<MenuItem<T>> menuItems, Consumer<Optional<T>> selectionConsumer, String digitalActionToOpenMenu){
        this.topLevelMenuItems = menuItems;
        this.selectionConsumer = selectionConsumer;
        this.digitalActionToOpenMenu = digitalActionToOpenMenu;
    }


    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand= boundHand;
        this.actionBasedOpenVrState = stateManager.getState(ActionBasedOpenVrState.ID, ActionBasedOpenVrState.class);
        this.vrAppState = stateManager.getState(VRAppState.class);
        Node rootNode = ((SimpleApplication)stateManager.getApplication()).getRootNode();
        rootNode.attachChild(menuNode);
        menuNode.setCullHint(Spatial.CullHint.Always);
        buildMenu();
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        menuNode.removeFromParent();
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        DigitalActionState menuButtonPressed = actionBasedOpenVrState.getDigitalActionState(digitalActionToOpenMenu, boundHand.getHandSide().restrictToInputString);
        if (menuButtonPressed.state && !this.menuOpen){
            openMenu();
        }

        if (!menuButtonPressed.state && this.menuOpen){
            closeMenuAndSelect();
        }

        if (this.menuOpen){
            //scan the palm area and finger tip area for menu items
            pickForMenuInteraction().ifPresent(this::updateRingVisibilityForSelectedPath);
        }

    }

    public void closeMenuAndSelect(){
        menuNode.setCullHint(Spatial.CullHint.Always);
        selectionConsumer.accept(pickForItemData());

        menuOpen = false;
    }

    public void openMenu(){
        menuNode.setCullHint(Spatial.CullHint.Inherit);
        Vector3f handPosition = boundHand.getPalmNode().getWorldTranslation();
        Vector3f headPosition = TamarinUtilities.getVrCameraPosition(this.vrAppState);
        menuNode.setLocalTranslation(boundHand.getPalmNode().getWorldTranslation());
        menuNode.lookAt(guessShoulderPosition(headPosition, handPosition), Vector3f.UNIT_Y);
        subRingCentreNodes.values().forEach(n -> n.setCullHint(Spatial.CullHint.Always));
        refreshDynamicGeometry();
        menuOpen = true;
    }

    /**
     * Looks through spatials near the hands and if it finds one that implies a menu path change returns it
     * @return A menu path that the spatial corresponds to
     */
    private Optional<ArrayList<MenuBranch<T>>> pickForMenuInteraction(){
        CollisionResults results = new CollisionResults();

        Vector3f palmCentre = boundHand.getPalmNode().getWorldTranslation();
        BoundingSphere sphere = new BoundingSphere(0.02f, palmCentre);
        menuNode.collideWith(sphere, results);

        for(int index=0;index<results.size();index++){
            CollisionResult collision = results.getCollision(index);
            Spatial spatial = collision.getGeometry();
            while(spatial!=null && spatial.getUserData(MENU_BRANCH_PATH)==null){
                spatial = spatial.getParent();
            }
            if (spatial!=null){
                return Optional.of(spatial.getUserData(MENU_BRANCH_PATH));
            }
        }
        return Optional.empty();
    }

    private Optional<T> pickForItemData(){

        Vector3f palmCentre = boundHand.getPalmNode().getWorldTranslation();

        return leafPositions.stream()
                .filter(l -> l.position.distanceSquared(palmCentre)<maximumSelectRange*maximumSelectRange)
                .filter(l -> currentOpenPath.containsAll(l.getParents())) //if the branch with this leaf is open
                .min(Comparator.comparingDouble(l -> l.position.distanceSquared(palmCentre)))
                .map(l -> l.getMenuLeaf().getLeafItem());
    }

    /**
     * Builds the entire menu, the menu is traversed by hiding and unhiding parts of it
     */
    private void buildMenu(){

        for(int menuItemIndex = 0; menuItemIndex< topLevelMenuItems.size(); menuItemIndex++){
            float angle = innerRingItemPositioner.determineAngleForItem(menuItemIndex, topLevelMenuItems.size());

            Vector3f position = new Vector3f(firstRingRadius* FastMath.sin(angle), firstRingRadius*FastMath.cos(angle), 0);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = topLevelMenuItems.get(menuItemIndex);
            attachOrConfigureIcon(menuItemNode, menuItem);
            menuNode.attachChild(menuItemNode);

            if (menuItem instanceof MenuBranch){
                MenuBranch<T> menuBranch = (MenuBranch<T>)menuItem;
                ArrayList<MenuBranch<T>> ringPathControlled = new ArrayList<>(List.of(menuBranch));
                configureMenuBranchForTouch(menuItemNode, ringPathControlled);
                buildSubItem(1, angle, menuBranch.getSubItems(), ringPathControlled);
            }else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>)menuItem;
                leafPositions.add(new ItemPositionData(List.of(), menuItemNode.getWorldTranslation(), menuLeaf));
            }
        }
    }

    private void attachOrConfigureIcon(Node menuItemNode, MenuItem<T> menuItem){
        if (menuItem.isDynamicIcon()){
            Supplier<Spatial> dynamicOptionGeometry = menuItem.getDynamicOptionGeometry();
            menuItemNode.attachChild(dynamicOptionGeometry.get());
            dynamicGeometrySuppliers.put(menuItemNode, dynamicOptionGeometry);
        }else{
            menuItemNode.attachChild(menuItem.getOptionGeometry());
        }
    }

    private void refreshDynamicGeometry(){
        for(Map.Entry<Node,Supplier<Spatial>> entry: dynamicGeometrySuppliers.entrySet()){
            entry.getKey().detachAllChildren();
            entry.getKey().attachChild(entry.getValue().get());
        }
    }

    private void buildSubItem(int ringIndex, float angleOfParent, List<MenuItem<T>> menuItems, ArrayList<MenuBranch<T>> parents){
        Node ringCentreNode = new Node("Ring " + ringIndex + " at angle " + angleOfParent);
        ringCentreNode.setCullHint(Spatial.CullHint.Always);
        menuNode.attachChild(ringCentreNode);
        subRingCentreNodes.put(parents, ringCentreNode);

        float ringRadius = firstRingRadius + ringIndex*interRingDistance;
        //flat rings get uncomfortably far away the greater the radius, bring them closer as they get wider
        float amountToBringRingCloser =  ringIndex * interRingDistance;

        for(int menuItemIndex = 0; menuItemIndex<menuItems.size();menuItemIndex++){
            float angle = childRingPositioner.determineAngleForItem(menuItemIndex, menuItems.size(), angleOfParent, ringIndex);
            Vector3f position = new Vector3f(ringRadius* FastMath.sin(angle), ringRadius*FastMath.cos(angle), amountToBringRingCloser);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = menuItems.get(menuItemIndex);
            attachOrConfigureIcon(menuItemNode, menuItem);

            ringCentreNode.attachChild(menuItemNode);

            if (menuItem instanceof MenuBranch){
                MenuBranch<T> menuBranch = (MenuBranch<T>)menuItem;
                ArrayList<MenuBranch<T>> childParents = new ArrayList<>(parents);
                childParents.add(menuBranch);
                configureMenuBranchForTouch(menuItemNode, childParents);
                buildSubItem(ringIndex+1, angle, menuBranch.getSubItems(), childParents);
            }else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>)menuItem;
                leafPositions.add(new ItemPositionData(parents, menuItemNode.getWorldTranslation(), menuLeaf));
            }
        }
    }

    private void updateRingVisibilityForSelectedPath(List<MenuBranch<T>> path){
        for(Map.Entry<ArrayList<MenuBranch<T>>, Node> ring : subRingCentreNodes.entrySet()){
            if(path.containsAll(ring.getKey())){
                ring.getValue().setCullHint(Spatial.CullHint.Inherit);
            }else{
                ring.getValue().setCullHint(Spatial.CullHint.Always);
            }
        }
        currentOpenPath = path;
    }

    private void configureMenuBranchForTouch(Spatial menuBranchGeometry, List<MenuBranch<T>> pathToOpen){
        menuBranchGeometry.setUserData(MENU_BRANCH_PATH, pathToOpen);
    }

    /**
     * The default child ring positioner prefers to have the middle of the ring near the parent. But if the ring
     * goes under the hand and could sensibly be positioned nearer the top (i.e. has loads of items) do that instead
     */
    private float defaultChildRingPositioner(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth){
        float ringRadius = firstRingRadius + (ringDepth)*interRingDistance;
        float circumferencePerItem = 0.15f;
        float anglePerItem = circumferencePerItem/ringRadius;

        float totalAngleUsed = anglePerItem * totalNumberOfSubItems;

        float centreOfRing = angleOfParent;

        float maxAngle = centreOfRing + totalAngleUsed/2;
        float minAngle = centreOfRing + totalAngleUsed/2;

        if (totalAngleUsed > 1.5 * FastMath.PI){
            centreOfRing = 0; //just put the ring starting at the top
        }else if (maxAngle>0.75*FastMath.PI){
            centreOfRing-=maxAngle-0.75*FastMath.PI;
        }else if (minAngle<-0.75*FastMath.PI){
            centreOfRing+=Math.abs(minAngle)-0.75*FastMath.PI;
        }

        return centreOfRing + (itemIndex- (totalNumberOfSubItems-1)/2f) *anglePerItem;

    }

    private Vector3f guessShoulderPosition(Vector3f headPosition, Vector3f handPosition){
        Vector3f handRelativeToHead = handPosition.subtract(headPosition);
        float shoulderHeight = headPosition.y - 0.15f;

        /* Typically the player will be looking directly at the hand whose menu they are opening.
         * This makes the look direction not very helpful, but together with which hand it is we can guess fairly
         * confidently what angle the shoulder will be at relative to the line between the head and the hand
         */
        float headToHandAngle = FastMath.atan2(handRelativeToHead.z, handRelativeToHead.x);
        float headToShoulderAngle = headToHandAngle+ (boundHand.getHandSide() == HandSide.RIGHT?+1:-1)*0.4f*FastMath.PI; //the constant is a bit dead reckoning based on experiment

        float centreHeadToShoulderDistance = 0.12f; //the constant is a bit dead reckoning based on experiment

        return new Vector3f(headPosition.x + centreHeadToShoulderDistance*FastMath.cos(headToShoulderAngle), shoulderHeight, headPosition.z +centreHeadToShoulderDistance*FastMath.sin(headToShoulderAngle));

    }

    @FunctionalInterface
    public interface InnerRingPositioner{

        /**
         * Given the index of the item (and how many items there are) return what angle the
         * item should be at about the hand
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfItems);
    }

    @FunctionalInterface
    public interface ChildRingPositioner{

        /**
         * Given the index of the item (and how many items there are) return what angle the
         * item should be at about the hand
         * @param ringDepth what ring this is, the first child ring is 1, child of child is 2 etc
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth);
    }

    @Value
    private class ItemPositionData{
        List<MenuBranch<T>> parents;
        Vector3f position;
        MenuLeaf<T> menuLeaf;
    }
}
