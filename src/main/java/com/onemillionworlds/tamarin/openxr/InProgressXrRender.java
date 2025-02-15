package com.onemillionworlds.tamarin.openxr;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.texture.FrameBuffer;

/**
 * We start a frame before the JME render and finish it after. This is a "continuation" that allows Xr to finish its
 * work and also gives us the camera positions required to position the players eyes in the virtual world.
 */
public class InProgressXrRender{
    public static EyePositionData NO_EYE_POSITION = new EyePositionData(
            new Vector3f(),
            new Quaternion().fromAngleNormalAxis(0, Vector3f.UNIT_Y),
            new FieldOfViewData(0,0,0,0)
    );

    public static InProgressXrRender NO_XR_FRAME = new InProgressXrRender(false, false, 0, NO_EYE_POSITION, NO_EYE_POSITION, null, null, -1, -1);

    boolean inProgressXr;
    boolean shouldRender;
    long predictedDisplayTime;

    EyePositionData leftEye;
    EyePositionData rightEye;

    FrameBuffer leftBufferToRenderTo;
    FrameBuffer rightBufferToRenderTo;

    int leftSwapchainImageIndex;
    int rightSwapchainImageIndex;

    public InProgressXrRender(boolean inProgressXr, boolean shouldRender, long predictedDisplayTime, EyePositionData leftEye, EyePositionData rightEye, FrameBuffer leftBufferToRenderTo, FrameBuffer rightBufferToRenderTo, int leftSwapchainImageIndex, int rightSwapchainImageIndex){
        this.inProgressXr = inProgressXr;
        this.shouldRender = shouldRender;
        this.predictedDisplayTime = predictedDisplayTime;
        this.leftEye = leftEye;
        this.rightEye = rightEye;
        this.leftBufferToRenderTo = leftBufferToRenderTo;
        this.rightBufferToRenderTo = rightBufferToRenderTo;
        this.leftSwapchainImageIndex = leftSwapchainImageIndex;
        this.rightSwapchainImageIndex = rightSwapchainImageIndex;
    }

    public boolean isInProgressXr(){
        return inProgressXr;
    }

    public boolean isShouldRender(){
        return shouldRender;
    }

    public long getPredictedDisplayTime(){
        return predictedDisplayTime;
    }

    public EyePositionData getLeftEye(){
        return leftEye;
    }

    public EyePositionData getRightEye(){
        return rightEye;
    }

    public FrameBuffer getLeftBufferToRenderTo(){
        return leftBufferToRenderTo;
    }

    public int getLeftSwapchainImageIndex(){
        return leftSwapchainImageIndex;
    }

    public FrameBuffer getRightBufferToRenderTo(){
        return rightBufferToRenderTo;
    }

    public int getRightSwapchainImageIndex(){
        return rightSwapchainImageIndex;
    }

    public record EyePositionData(
            Vector3f eyePosition,
            Quaternion eyeRotation,
            FieldOfViewData fieldOfView
    ){
        public Matrix4f calculateProjectionMatrix(float nearClip, float farClip){
            return XrUtils.createProjectionMatrix(fieldOfView, nearClip, farClip);
        }
    }

    /**
     * The field of view data for a single eye. NOTE! the left and right angles are held seperately because the OPENXR runtime
     * may request a non symmetric field of view. This is the case for the Oculus Quest 2. Do not just add left and right
     * together and use that as the field of view.
     * @param angleLeft angle in radians
     * @param angleRight angle in radians
     * @param angleUp angle in radians
     * @param angleDown angle in radians
     */
    public record FieldOfViewData(
            float angleLeft,
            float angleRight,
            float angleUp,
            float angleDown
    ){}
}
