package com.onemillionworlds.tamarin.actions.debug;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.onemillionworlds.tamarin.actions.state.BonePose;
import com.onemillionworlds.tamarin.handskeleton.HandJoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is for use in development. Find a bone pose you want to build your hand models with and use this class
 * to produce a script that will (more or less) automatically build the bone structure in blender.
 *
 * It is what I (Tamarin author) used to create the original bones in the Tamarin hand
 */
public class RestPose{
    private final String boneName;
    private final Vector3f position;
    private final Quaternion rotation;

    public RestPose(String boneName, Vector3f position, Quaternion rotation){
        this.boneName = boneName;
        this.position = position;
        this.rotation = rotation;
    }

    public Quaternion getRotation(){
        return rotation;
    }

    public Vector3f getPosition(){
        return position;
    }

    public String getBoneName(){
        return boneName;
    }

    /**
     * A function that builds the bones in a structure useful for blenders python script
     */
    public String outputBlenderBonePositions(){
        StringBuilder builder = new StringBuilder();

        Vector3f head = getPosition();
        Quaternion quaternion = getRotation();
        builder.append("    \"").append(boneName).append("\": {\n");
        builder.append("        \"head\": mathutils.Vector((").append(head.x).append(", ").append(head.y).append(", ").append(head.z).append(")),\n");
        builder.append("        \"quaternion\": mathutils.Quaternion((").append(quaternion.getW()).append(", ").append(quaternion.getX()).append(", ").append(quaternion.getY()).append(", ").append(quaternion.getZ()).append("))\n");
        builder.append("    },\n");

        return builder.toString();
    }

    /**
     * Creates a script that can be used in blender to produce the bone structure for the hand model. The idea is that
     * you find a hand posture you like then you can build your hand model around that posture.
     * <p>
     * You should create a new blender file, delete the default cube, create an armature with 1 bone called root, that
     *  (for the left hand) has zero roll and is at zero with its tail in the +z direction. Then run this script in the
     *  blender console.
     * <p>
     * Note that the bones do not align with the physical bones, this is expected and fine. It is because blender considers
     * +Y to be the bone's direction whereas I think JME considers -Z to be the direction. It all sorts itself out.
     * <p>
     * When exporting remember to set +Y up in the export options.
     */
    @SuppressWarnings("unused")
    public static String buildScriptToCreateBones(Map<HandJoint, BonePose> bonePoses, Map<HandJoint, String> boneNames){
        StringBuilder dataBuilder = new StringBuilder();

        for(HandJoint handJoint : bonePoses.keySet()){
            BonePose pose = bonePoses.get(handJoint);
            String boneName = boneNames.get(handJoint);
            dataBuilder.append(new RestPose(boneName, pose.position(), pose.orientation()).outputBlenderBonePositions());
        }
        try{
            String content = getResourceFileAsString("com/onemillionworlds/tamarin/blenderscripts/createBlenderBones.py");
            content = content.replace("[[BONE_DATA]]", dataBuilder.toString());

            System.out.println(content);
            return content;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private static String getResourceFileAsString(String fileName) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
