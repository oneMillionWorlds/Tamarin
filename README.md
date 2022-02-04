# Tamarin
A VR utilities library for JMonkeyEngine. 

## Typical imports

To successfully use Tamarin you should typically have at least the following dependencies

    implementation "org.jmonkeyengine:jme3-core:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-lwjgl3:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-vr:$jmonkeyengine_version"

Optionally you can also have

    implementation "com.simsilica:lemur:1.14.0"

## Logging

This library uses simple java.util.Logger for maximum compatibility. Bind that to your preferred logging library.

## Best practices

Grabbing and menu picking both use geometry picking. With simple scenes performance may be fine picking
against the root node. However with complex scenes this may become slow, creating a node that contains 
all the grabbables (or all the UI interactables) may give better performance

## Attribution and licensing

This project is licensed under the BSD-3 license, meaning it can be used in a commercial project free of charge with no need to provide attribution. That said if you want to mention that Tamarin is used in your project that would be very welcome.

The hand models are similarly included under that license; if you want to start with Tamarin models but tweak them the blenderFiles may be useful. There is no requirement to distribute any updates to hand models, but if you want to contribute improved models that is also very welcome.

## Coding standard

Standard Java coding conventions (try to match existing style). 

British English; "Centre", "Colour" etc

## Signing

To sign jars for maven central appropriate details will need to be in C:\Users\{user}\.gradle\gradle.properties. Will need

    signing.keyId=keyId
    signing.password=password
    signing.secretKeyRingFile=C:/Users/{user}/AppData/Roaming/gnupg/pubring.kbx
    
    ossrhUsername=your-jira-id
    ossrhPassword=your-jira-password

Note that the keyId is just the last 8 characters of the long id, and the secretRing must be explicitly `exported gpg --export-secret-keys -o secring.gpg`

## Nexus

Project is provisioned on https://s01.oss.sonatype.org/

## Upgrade

This library forward ports JMonkeyEngine 3.6 functionality into JMonkeyEngine 3.5. Expect breaking changes on upgrading
to JMonkeyEngine 3.6 (Although as this tracks the intended functionality in 3.6, so shouldn't be too painful)