package com.onemillionworlds.tamarin.compatibility;

import org.lwjgl.openvr.VRApplications;

import java.io.File;
import java.util.logging.Logger;

public class ApplicationRegistration{

    private static final Logger logger = Logger.getLogger(ActionBasedOpenVrState.class.getName());

    public static void registerApplication(File applicationManifest, String VRAppKey){

        if (applicationManifest.exists()) {
            String path = applicationManifest.getAbsolutePath(); //Must be absolute path
            int error;
            if (true || !VRApplications.VRApplications_IsApplicationInstalled(VRAppKey)) {
                error = VRApplications.VRApplications_AddApplicationManifest(path, false);
                if (error != 0){
                    throw new RuntimeException("Error while registering application; " + VRApplications.VRApplications_GetApplicationsErrorNameFromEnum(error));
                }
            } else {
                logger.info("VR App with Id " + VRAppKey + "already installed");
            }

            long pid = ProcessHandle.current().pid();
            error = VRApplications.VRApplications_IdentifyApplication((int) pid, VRAppKey);
            if (error != 0){
                throw new RuntimeException("Error while setting application id; " + VRApplications.VRApplications_GetApplicationsErrorNameFromEnum(error));
            }
        } else {
            throw new RuntimeException("No manifest found at " + applicationManifest.getAbsolutePath());
        }
    }

}
