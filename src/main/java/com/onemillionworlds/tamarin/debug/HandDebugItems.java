package com.onemillionworlds.tamarin.debug;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.onemillionworlds.tamarin.vrhands.BoundHand;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * A list of item that are available for debugging the hand skeleton and other points
 */
@AllArgsConstructor
@Getter
public enum HandDebugItems{
    SKELETON(null, null), //this one is a special case
    WRIST_NODE(ColorRGBA.Blue, BoundHand::getWristNode),
    PALM_NODE(ColorRGBA.Brown, BoundHand::getPalmNode),
    INDEX_FINGER_TIP(ColorRGBA.Cyan, BoundHand::getIndexFingerTip_xPointing),
    HAND_NODE_X_POINTING(ColorRGBA.Pink, BoundHand::getHandNode_xPointing),
    PALM_PICK_POINTS(ColorRGBA.Orange, null),
    ;

    final ColorRGBA colorRGBA;
    final Function<BoundHand, Node> debugItemCreator;

    public boolean isSpecialCase(){
        return debugItemCreator == null;
    }
}
