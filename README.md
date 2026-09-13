# Tamarin
A VR utilities library for JMonkeyEngine using OpenXR. 

## Typical imports

To successfully use Tamarin you should typically have at least the following dependencies

    implementation "com.onemillionworlds:tamarin:$tamarin_version"

    implementation "org.jmonkeyengine:jme3-core:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-lwjgl3:$jmonkeyengine_version"
    implementation "org.jmonkeyengine:jme3-desktop:$jmonkeyengine_version"

Optionally you can also have

    implementation "com.simsilica:lemur:$lemur_version"

## Logging

This library uses simple java.util.Logger for maximum compatibility. Bind that to your preferred logging library.

## Documentation

See https://github.com/oneMillionWorlds/Tamarin/wiki for documentation 

See https://github.com/oneMillionWorlds/TamarinTestBed for examples

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

Note that the keyId is just the last 8 characters of the long id, and the secretRing must be explicitly exported `gpg --export-secret-keys -o secring.gpg`

## Nexus

Project is provisioned on https://central.sonatype.com/publishing

Deploy to sonatype via pipeline by:
- Running in gitlab the publish job
- Go to https://central.sonatype.com/publishing and log in
- Go to the staging repository and select the repository
- If all looks well "close" the repository and then Release it
- Tag the release


Deploy to sonatype manually by:
- Editing the build.gradle file to have a non snapshot version
- Running `./gradlew publishMavenJavaPublicationToSonaTypeRepository` (see https://docs.gradle.org/current/userguide/publishing_maven.html)
- Go to https://central.sonatype.com/publishing and log in as user oneMillionWorlds
- Go to the staging repository and select the repository
- If all looks well "close" the repository and then Release it
- Tag the release

### Testing a deployment before publishing

A `VALIDATED` deployment can be consumed straight from the Portal, so a release can be tested from a real
consuming project before it is made permanent.

Add to the consuming `build.gradle`:

    repositories {
        maven {
            name = "centralManualTesting"
            url = "https://central.sonatype.com/api/v1/publisher/deployments/download/"
            credentials(HttpHeaderCredentials)
            authentication { header(HttpHeaderAuthentication) }
        }
    }

And to that project's `gradle.properties` (or better, your user level `~/.gradle/gradle.properties`):

    centralManualTestingAuthHeaderName=Authorization
    centralManualTestingAuthHeaderValue=Bearer <base64 of tokenUsername:tokenPassword>

Where the token value is the base64 encoding of `tokenUsername:tokenPassword`, e.g.

    printf '%s:%s' "$CENTRAL_USERNAME" "$CENTRAL_PASSWORD" | base64 -w 0

Then depend on the version being released as normal. That URL serves files from any of your validated
deployments; to pin to one specific deployment use
`https://central.sonatype.com/api/v1/publisher/deployment/<deploymentId>/download/` instead.

Remember to remove this repository from the consuming project once the version is actually published.

### Testing without uploading at all

For quick local iteration skip the Portal entirely and use maven local:

    ./gradlew publishToMavenLocal

Then add `mavenLocal()` to the consuming project's repositories. Use a distinct version number so it is
obvious which artifacts are being picked up.


