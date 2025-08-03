package com.onemillionworlds.tamarin.vrhands.functions.handmenu;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.onemillionworlds.tamarin.actions.HandSide;
import com.onemillionworlds.tamarin.actions.XrActionBaseAppState;
import com.onemillionworlds.tamarin.actions.actionprofile.ActionHandle;
import com.onemillionworlds.tamarin.actions.state.BooleanActionState;
import com.onemillionworlds.tamarin.openxr.XrBaseAppState;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import com.onemillionworlds.tamarin.vrhands.functions.BoundHandFunction;

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
 * <p>
 * The menu can be a tree, where passing the hand through 1 item opens up a new set of branches
 * <p>
 * The menu remains open for as long as the digital action remains held. When the action is released where the hand is
 * controls what is selected (if anything)
 */
public class HandRingMenuFunction<T> implements BoundHandFunction{

    private final List<MenuItem<T>> topLevelMenuItems;
    private List<MenuBranch<T>> currentOpenPath = List.of();

    private final Consumer<Optional<T>> selectionConsumer;

    ActionHandle digitalActionToOpenMenu;

    private BoundHand boundHand;
    private XrActionBaseAppState actionBasedOpenVrState;
    private XrBaseAppState vrAppState;
    Node menuNode = new Node("MenuNode");
    private boolean menuOpen = false;

    /**
     * For selecting leaves this is the futhest the palm can be from the centre and still be considered a select
     */
    private float maximumSelectRange = 0.1f;
    /**
     * This records a set of menu item paths (The great grandparent -> grandparent -> parent etc) and the ring
     * centre node that should be shown if that path is active
     */
    private final Map<ArrayList<MenuBranch<T>>, Node> subRingCentreNodes = new HashMap<>();


    /**
     * For the first ring of items determines where they should be placed. Default behaviour is to use the top
     * 3 quarters, starting on the left, evenly spaced.
     * <p>
     * Zero radians is straight up, negative is left
     */
    private InnerRingPositioner innerRingItemPositioner = (index, items) -> -0.75f * FastMath.PI + (float) (index * 1.5f * Math.PI / (items - 1));

    /**
     * For the subcategory rings (which can have children of children of children etc) this function determines the angle
     * at which the child members should be.
     * <p>
     * Zero radians is straight up, negative is left
     */
    private ChildRingPositioner childRingPositioner = this::defaultChildRingPositioner;

    /**
     * Where the centre of the first ring is
     */
    private float firstRingRadius = 0.15f;

    /**
     * Distance between ring 1, ring 2, ring 3 etc
     */
    private float interRingDistance = 0.15f;

    private final Collection<LeafPositionData> leafPositions = new HashSet<>();
    private final Collection<BranchPositionData> branchPositions = new HashSet<>();
    /**
     * Dynamic geometries update every time the menu is opened.
     */
    Map<Node, Supplier<Spatial>> dynamicGeometrySuppliers = new HashMap<>();

    /**
     * @param menuItems               the tree of menu items
     * @param selectionConsumer       when an item is selected it is given to this consumer
     * @param digitalActionToOpenMenu The digital action (button press) that opens the menu
     */
    public HandRingMenuFunction(List<MenuItem<T>> menuItems, Consumer<Optional<T>> selectionConsumer, ActionHandle digitalActionToOpenMenu){
        this.topLevelMenuItems = menuItems;
        this.selectionConsumer = selectionConsumer;
        this.digitalActionToOpenMenu = digitalActionToOpenMenu;
    }


    @Override
    public void onBind(BoundHand boundHand, AppStateManager stateManager){
        this.boundHand = boundHand;
        this.actionBasedOpenVrState = stateManager.getState(XrActionBaseAppState.ID, XrActionBaseAppState.class);
        this.vrAppState = stateManager.getState(XrBaseAppState.ID, XrBaseAppState.class);
        Node rootNode = ((SimpleApplication) stateManager.getApplication()).getRootNode();
        rootNode.attachChild(menuNode);
        menuNode.setCullHint(Spatial.CullHint.Always);
        buildMenu();
        stateManager.getApplication().getRenderManager().preloadScene(menuNode);
    }

    @Override
    public void onUnbind(BoundHand boundHand, AppStateManager stateManager){
        menuNode.removeFromParent();
    }

    @Override
    public void update(float timeSlice, BoundHand boundHand, AppStateManager stateManager){
        BooleanActionState menuButtonPressed = actionBasedOpenVrState.getBooleanActionState(digitalActionToOpenMenu, boundHand.getHandSide().restrictToInputString);
        if(menuButtonPressed.getState() && !this.menuOpen){
            openMenu();
        }

        if(!menuButtonPressed.getState() && this.menuOpen){
            closeMenuAndSelect();
        }

        if(this.menuOpen){
            //scan the palm area for menu items
            scanForMenuInteraction().ifPresent(this::updateRingVisibilityForSelectedPath);
        }

    }

    public void closeMenuAndSelect(){
        menuNode.setCullHint(Spatial.CullHint.Always);
        selectionConsumer.accept(pickForItemData());
        currentOpenPath = List.of();
        menuOpen = false;
    }

    public void openMenu(){
        menuNode.setCullHint(Spatial.CullHint.Inherit);
        Vector3f handPosition = boundHand.getPalmNode().getWorldTranslation();
        Vector3f headPosition = this.vrAppState.getVrCameraPosition();
        menuNode.setLocalTranslation(boundHand.getPalmNode().getWorldTranslation());
        menuNode.lookAt(guessShoulderPosition(headPosition, handPosition), Vector3f.UNIT_Y);
        subRingCentreNodes.values().forEach(n -> n.setCullHint(Spatial.CullHint.Always));
        refreshDynamicGeometry();
        menuOpen = true;
    }

    /**
     * Looks through spatials near the hands and if it finds one that implies a menu path change returns it
     *
     * @return A menu path that the spatial corresponds to
     */
    private Optional<List<MenuBranch<T>>> scanForMenuInteraction(){

        Vector3f palmCentre = boundHand.getPalmNode().getWorldTranslation();

        return branchPositions.stream()
                .filter(l -> l.position.distanceSquared(palmCentre) < maximumSelectRange * maximumSelectRange)
                .filter(l -> currentOpenPath.containsAll(l.getParents())) //if the branch is open
                .min(Comparator.comparingDouble(l -> l.position.distanceSquared(palmCentre)))
                .map(BranchPositionData::getOwnedPath);
    }

    private Optional<T> pickForItemData(){

        Vector3f palmCentre = boundHand.getPalmNode().getWorldTranslation();

        return leafPositions.stream()
                .filter(l -> l.position.distanceSquared(palmCentre) < maximumSelectRange * maximumSelectRange)
                .filter(l -> currentOpenPath.containsAll(l.getParents())) //if the branch with this leaf is open
                .min(Comparator.comparingDouble(l -> l.position.distanceSquared(palmCentre)))
                .map(l -> l.getMenuLeaf().getLeafItem());
    }

    /**
     * Builds the entire menu, the menu is traversed by hiding and unhiding parts of it
     */
    private void buildMenu(){
        for(int menuItemIndex = 0; menuItemIndex < topLevelMenuItems.size(); menuItemIndex++){
            float angle = innerRingItemPositioner.determineAngleForItem(menuItemIndex, topLevelMenuItems.size());

            Vector3f position = new Vector3f(firstRingRadius * FastMath.sin(angle), firstRingRadius * FastMath.cos(angle), 0);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = topLevelMenuItems.get(menuItemIndex);
            attachOrConfigureIcon(menuItemNode, menuItem);
            menuNode.attachChild(menuItemNode);

            if(menuItem instanceof MenuBranch<T> menuBranch){
                ArrayList<MenuBranch<T>> ringPathControlled = new ArrayList<>(List.of(menuBranch));
                buildSubItem(1, angle, menuBranch.getSubItems(), ringPathControlled);
                branchPositions.add(new BranchPositionData(List.of(), ringPathControlled, menuItemNode.getWorldTranslation(), menuBranch));
            } else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>) menuItem;
                leafPositions.add(new LeafPositionData(List.of(), menuItemNode.getWorldTranslation(), menuLeaf));
            }
        }
    }

    private void attachOrConfigureIcon(Node menuItemNode, MenuItem<T> menuItem){
        if(menuItem.isDynamicIcon()){
            Supplier<Spatial> dynamicOptionGeometry = menuItem.getDynamicOptionGeometry();
            menuItemNode.attachChild(dynamicOptionGeometry.get());
            dynamicGeometrySuppliers.put(menuItemNode, dynamicOptionGeometry);
        } else{
            menuItemNode.attachChild(menuItem.getOptionGeometry());
        }
    }

    private void refreshDynamicGeometry(){
        for(Map.Entry<Node, Supplier<Spatial>> entry : dynamicGeometrySuppliers.entrySet()){
            entry.getKey().detachAllChildren();
            entry.getKey().attachChild(entry.getValue().get());
        }
    }

    private void buildSubItem(int ringIndex, float angleOfParent, List<MenuItem<T>> menuItems, ArrayList<MenuBranch<T>> parents){
        Node ringCentreNode = new Node("Ring " + ringIndex + " at angle " + angleOfParent);
        ringCentreNode.setCullHint(Spatial.CullHint.Always);
        menuNode.attachChild(ringCentreNode);
        subRingCentreNodes.put(parents, ringCentreNode);

        float ringRadius = firstRingRadius + ringIndex * interRingDistance;
        //flat rings get uncomfortably far away the greater the radius, bring them closer as they get wider
        float amountToBringRingCloser = 0.5f * ringIndex * interRingDistance;

        for(int menuItemIndex = 0; menuItemIndex < menuItems.size(); menuItemIndex++){
            float angle = childRingPositioner.determineAngleForItem(menuItemIndex, menuItems.size(), angleOfParent, ringIndex);
            Vector3f position = new Vector3f(ringRadius * FastMath.sin(angle), ringRadius * FastMath.cos(angle), amountToBringRingCloser);

            Node menuItemNode = new Node();
            menuItemNode.setLocalTranslation(position);
            MenuItem<T> menuItem = menuItems.get(menuItemIndex);
            attachOrConfigureIcon(menuItemNode, menuItem);

            ringCentreNode.attachChild(menuItemNode);

            if(menuItem instanceof MenuBranch<T> menuBranch){
                ArrayList<MenuBranch<T>> branchOwnedPath = new ArrayList<>(parents);
                branchOwnedPath.add(menuBranch);
                buildSubItem(ringIndex + 1, angle, menuBranch.getSubItems(), branchOwnedPath);
                branchPositions.add(new BranchPositionData(parents, branchOwnedPath, menuItemNode.getWorldTranslation(), menuBranch));
            } else{
                MenuLeaf<T> menuLeaf = (MenuLeaf<T>) menuItem;
                leafPositions.add(new LeafPositionData(parents, menuItemNode.getWorldTranslation(), menuLeaf));
            }
        }
    }

    private void updateRingVisibilityForSelectedPath(List<MenuBranch<T>> path){
        if(currentOpenPath == path){
            return;
        }

        for(Map.Entry<ArrayList<MenuBranch<T>>, Node> ring : subRingCentreNodes.entrySet()){
            if(path.containsAll(ring.getKey())){
                ring.getValue().setCullHint(Spatial.CullHint.Inherit);
            } else{
                ring.getValue().setCullHint(Spatial.CullHint.Always);
            }
        }
        currentOpenPath = path;
    }

    /**
     * The default child ring positioner prefers to have the middle of the ring near the parent. But if the ring
     * goes under the hand and could sensibly be positioned nearer the top (i.e. has loads of items) do that instead
     */
    private float defaultChildRingPositioner(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth){
        float ringRadius = firstRingRadius + (ringDepth) * interRingDistance;
        float circumferencePerItem = 0.15f;
        float anglePerItem = circumferencePerItem / ringRadius;

        float totalAngleUsed = anglePerItem * totalNumberOfSubItems;

        float centreOfRing = angleOfParent;

        float maxAngle = centreOfRing + totalAngleUsed / 2;
        float minAngle = centreOfRing - totalAngleUsed / 2;

        if(totalAngleUsed > 1.5 * FastMath.PI){
            centreOfRing = 0; //just put the ring starting at the top
        } else if(maxAngle > 0.75 * FastMath.PI){
            centreOfRing -= maxAngle - 0.75f * FastMath.PI;
        } else if(minAngle < -0.75 * FastMath.PI){
            centreOfRing += Math.abs(minAngle) - 0.75f * FastMath.PI;
        }

        return centreOfRing + (itemIndex - (totalNumberOfSubItems - 1) / 2f) * anglePerItem;

    }

    private Vector3f guessShoulderPosition(Vector3f headPosition, Vector3f handPosition){
        Vector3f handRelativeToHead = handPosition.subtract(headPosition);
        float shoulderHeight = headPosition.y - 0.15f;

        /* Typically the player will be looking directly at the hand whose menu they are opening.
         * This makes the look direction not very helpful, but together with which hand it is we can guess fairly
         * confidently what angle the shoulder will be at relative to the line between the head and the hand
         */
        float headToHandAngle = FastMath.atan2(handRelativeToHead.z, handRelativeToHead.x);
        float headToShoulderAngle = headToHandAngle + (boundHand.getHandSide() == HandSide.RIGHT ? +1 : -1) * 0.4f * FastMath.PI; //the constant is a bit dead reckoning based on experiment

        float centreHeadToShoulderDistance = 0.12f; //the constant is a bit dead reckoning based on experiment

        return new Vector3f(headPosition.x + centreHeadToShoulderDistance * FastMath.cos(headToShoulderAngle), shoulderHeight, headPosition.z + centreHeadToShoulderDistance * FastMath.sin(headToShoulderAngle));

    }

    public float getFirstRingRadius(){
        return this.firstRingRadius;
    }

    public float getInterRingDistance(){
        return this.interRingDistance;
    }

    public void setMaximumSelectRange(float maximumSelectRange){
        this.maximumSelectRange = maximumSelectRange;
    }

    public void setInnerRingItemPositioner(InnerRingPositioner innerRingItemPositioner){
        this.innerRingItemPositioner = innerRingItemPositioner;
    }

    public void setChildRingPositioner(ChildRingPositioner childRingPositioner){
        this.childRingPositioner = childRingPositioner;
    }

    public void setFirstRingRadius(float firstRingRadius){
        this.firstRingRadius = firstRingRadius;
    }

    public void setInterRingDistance(float interRingDistance){
        this.interRingDistance = interRingDistance;
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
         *
         * @param ringDepth what ring this is, the first child ring is 1, child of child is 2 etc
         */
        float determineAngleForItem(int itemIndex, int totalNumberOfSubItems, float angleOfParent, int ringDepth);
    }

    private final class LeafPositionData{
        private final List<MenuBranch<T>> parents;
        private final Vector3f position;
        private final MenuLeaf<T> menuLeaf;

        public LeafPositionData(List<MenuBranch<T>> parents, Vector3f position, MenuLeaf<T> menuLeaf){
            this.parents = parents;
            this.position = position;
            this.menuLeaf = menuLeaf;
        }

        public List<MenuBranch<T>> getParents(){
            return this.parents;
        }

        public Vector3f getPosition(){
            return this.position;
        }

        public MenuLeaf<T> getMenuLeaf(){
            return this.menuLeaf;
        }

        public boolean equals(final Object o){
            if(o == this) return true;
            if(!(o instanceof HandRingMenuFunction.LeafPositionData)) return false;
            final LeafPositionData other = (LeafPositionData) o;
            final Object this$parents = this.getParents();
            final Object other$parents = other.getParents();
            if(this$parents == null ? other$parents != null : !this$parents.equals(other$parents)) return false;
            final Object this$position = this.getPosition();
            final Object other$position = other.getPosition();
            if(this$position == null ? other$position != null : !this$position.equals(other$position)) return false;
            final Object this$menuLeaf = this.getMenuLeaf();
            final Object other$menuLeaf = other.getMenuLeaf();
            if(this$menuLeaf == null ? other$menuLeaf != null : !this$menuLeaf.equals(other$menuLeaf)) return false;
            return true;
        }

        public int hashCode(){
            final int PRIME = 59;
            int result = 1;
            final Object $parents = this.getParents();
            result = result * PRIME + ($parents == null ? 43 : $parents.hashCode());
            final Object $position = this.getPosition();
            result = result * PRIME + ($position == null ? 43 : $position.hashCode());
            final Object $menuLeaf = this.getMenuLeaf();
            result = result * PRIME + ($menuLeaf == null ? 43 : $menuLeaf.hashCode());
            return result;
        }

        public String toString(){
            return "HandRingMenuFunction.LeafPositionData(parents=" + this.getParents() + ", position=" + this.getPosition() + ", menuLeaf=" + this.getMenuLeaf() + ")";
        }
    }

    private final class BranchPositionData{
        private final List<MenuBranch<T>> parents;
        private final List<MenuBranch<T>> ownedPath;
        private final Vector3f position;
        private final MenuBranch<T> menuLeaf;

        public BranchPositionData(List<MenuBranch<T>> parents, List<MenuBranch<T>> ownedPath, Vector3f position, MenuBranch<T> menuLeaf){
            this.parents = parents;
            this.ownedPath = ownedPath;
            this.position = position;
            this.menuLeaf = menuLeaf;
        }

        public List<MenuBranch<T>> getParents(){
            return this.parents;
        }

        public List<MenuBranch<T>> getOwnedPath(){
            return this.ownedPath;
        }

        public Vector3f getPosition(){
            return this.position;
        }

        public MenuBranch<T> getMenuLeaf(){
            return this.menuLeaf;
        }

        public boolean equals(final Object o){
            if(o == this) return true;
            if(!(o instanceof HandRingMenuFunction.BranchPositionData)) return false;
            final BranchPositionData other = (BranchPositionData) o;
            final Object this$parents = this.getParents();
            final Object other$parents = other.getParents();
            if(this$parents == null ? other$parents != null : !this$parents.equals(other$parents)) return false;
            final Object this$ownedPath = this.getOwnedPath();
            final Object other$ownedPath = other.getOwnedPath();
            if(this$ownedPath == null ? other$ownedPath != null : !this$ownedPath.equals(other$ownedPath)) return false;
            final Object this$position = this.getPosition();
            final Object other$position = other.getPosition();
            if(this$position == null ? other$position != null : !this$position.equals(other$position)) return false;
            final Object this$menuLeaf = this.getMenuLeaf();
            final Object other$menuLeaf = other.getMenuLeaf();
            if(this$menuLeaf == null ? other$menuLeaf != null : !this$menuLeaf.equals(other$menuLeaf)) return false;
            return true;
        }

        public int hashCode(){
            final int PRIME = 59;
            int result = 1;
            final Object $parents = this.getParents();
            result = result * PRIME + ($parents == null ? 43 : $parents.hashCode());
            final Object $ownedPath = this.getOwnedPath();
            result = result * PRIME + ($ownedPath == null ? 43 : $ownedPath.hashCode());
            final Object $position = this.getPosition();
            result = result * PRIME + ($position == null ? 43 : $position.hashCode());
            final Object $menuLeaf = this.getMenuLeaf();
            result = result * PRIME + ($menuLeaf == null ? 43 : $menuLeaf.hashCode());
            return result;
        }

        public String toString(){
            return "HandRingMenuFunction.BranchPositionData(parents=" + this.getParents() + ", ownedPath=" + this.getOwnedPath() + ", position=" + this.getPosition() + ", menuLeaf=" + this.getMenuLeaf() + ")";
        }
    }
}
