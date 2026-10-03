---
name: add-interaction-profile
description: Add support for a new OpenXR interaction profile (a controller, or a hand/gesture profile) to Tamarin's controllerprofile package, including extension gating and the Action.ActionBuilder withSuggestAllKnown* helpers. Use when asked to support a new controller/headset input, or to add or fix paths on an existing profile class.
---

# Adding an OpenXR interaction profile

Profile classes live in `tamarin-core/src/main/java/com/onemillionworlds/tamarin/actions/controllerprofile/`. They are
pure constants plus a fluent path builder. They contain no logic and are platform-independent.

## 1. Get the paths from the spec, not from memory

Take the profile path and every component path from the OpenXR specification ("Interaction Profile Paths" section, or
the extension's own section). Use WebFetch on https://registry.khronos.org/OpenXR/specs/1.1/html/xrspec.html if needed.
Note:
- which top-level user paths the profile supports (`/user/hand/left`, `/user/hand/right`, sometimes others). Some
  components exist on only one hand (e.g. X/Y vs A/B on Touch).
- whether the profile requires an extension (e.g. `XR_EXT_hand_interaction`, `XR_HTC_vive_cosmos_controller_interaction`).
  Profiles promoted to core in OpenXR 1.1 may still need the extension on 1.0 runtimes.

## 2. Write the class

Copy the structure of an existing class. Use `OculusTouchController` for a controller with per-hand differences and
`HandInteractionExt` for an extension-gated profile. The shape is:

```java
public class FooController{
    public static final String PROFILE = "/interaction_profiles/vendor/foo_controller";
    public static final String REQUIRED_EXTENSION = "XR_..."; // only if extension-gated

    public static class InteractionProfiles{ LEFT_HAND, RIGHT_HAND ... }
    public static class ComponentPaths{ /input/.../click etc, with Javadoc on non-obvious ones }

    public static BindingPathBuilder pathBuilder(){ ... }
    public static class BindingPathBuilder{ leftHand(), rightHand() }
    public static class BindingPathBuilderHand{ one method per component, e.g. triggerValue(), gripPose(), haptic() }
}
```

- The class Javadoc should say which physical device(s) the profile represents, plus any platform caveats (e.g.
  AndroidManifest permissions on Quest, no haptics on hands).
- Method names on the hand builder should match the names other profiles use for equivalent components (`gripPose()`,
  `aimPose()`, `triggerValue()`, `haptic()`, ...). The `withSuggestAllKnown*` helpers rely on this consistency.
- Follow the CLAUDE.md conventions: Java 11, British English, `){` brace style.

## 3. Wire it in

1. **Extension gating.** If the profile needs an extension:
   - add `PROFILE -> REQUIRED_EXTENSION` to the map in `InteractionProfileExtensions` (the desktop and Android action
     states use it to skip suggesting bindings when the extension isn't loaded, because suggesting bindings for that
     profile without the extension is an OpenXR error).
   - decide whether to request the extension by default in the `XrSettings` constructor. Request it by default if it
     is harmless when unsupported; otherwise document how users add it with `addRequiredXrExtension`.
2. **Suggest-all helpers.** In `actions/actionprofile/Action.java`, `ActionBuilder.withSuggestAllKnown*Bindings()`
   (haptic, grip pose, aim pose, select, grab) add a left and right line for the new profile where it has an
   equivalent component. Skip helpers the profile can't support (e.g. no haptic on hands). Also update the Javadoc
   `<li>` list of profiles in that file if the profile should be listed.
3. **Nothing else should be needed** in the desktop or Android modules. If you find yourself editing
   `XrActionAppState` / `XrActionAndroidAppState`, switch to the `cross-platform-xr-feature` skill and mirror the change
   in both.

## 4. Verify

- `./gradlew :tamarin-core:test` (`ActionManifestTest` covers manifest validation; add tests there if you add
  builder behaviour beyond extra suggested-binding lines).
- Grep that every `pathBuilder()` method returns a string starting with a valid user path and component path. Typos
  in path strings only fail at runtime (`xrSuggestInteractionProfileBindings` returns `XR_ERROR_PATH_UNSUPPORTED`).
- Tell the user it needs a check on the real device or runtime.
