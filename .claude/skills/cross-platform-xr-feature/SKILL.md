---
name: cross-platform-xr-feature
description: Checklist for adding or changing OpenXR-backed functionality in Tamarin (new extension, session behaviour, app-state API, action-system change) so it lands consistently in core, desktop (LWJGL), Android (openxr-bindings-native) and the desktop simulation. Use whenever a change touches XrBaseAppState, XrActionBaseAppState, OpenXrSessionManager, OpenXrAndroidSessionManager, XrActionAppState or XrActionAndroidAppState.
---

# Adding an OpenXR feature across all platforms

Tamarin has one abstract API and three implementations: desktop VR, Android VR and desktop simulation (no headset).
Most bugs in this area come from updating one implementation and forgetting the others. Work through every layer
below. If a layer genuinely doesn't apply, say so explicitly rather than silently skipping it.

## 1. Decide where the logic lives

Put as much as possible in **core, free of OpenXR types**. Good examples are `InteractionProfileTracker`,
`XrSessionObservables` and `GameplayInterrupt`: pure Java classes that take plain values and are unit tested. The
platform layers then only translate OpenXR calls and events into calls on these classes.

## 2. Layers to touch (in this order)

| # | Layer | File(s) | Notes |
|---|---|---|---|
| 1 | Settings | `tamarin-core/.../openxr/XrSettings.java` | If an extension is needed, add it to the default `requiredXrExtensions` list in the constructor with a trailing comment naming the LWJGL constant, matching existing lines. Add a getter/setter pair with Javadoc for any user-facing option. |
| 2 | Public API | `openxr/XrBaseAppState.java` or `actions/XrActionBaseAppState.java` | Abstract method or concrete method with full Javadoc. App-facing state should be a `subscribeToX()` that returns an `observable` subscription. |
| 3 | Shared VR logic | `openxr/XrVrAppState.java` | Code common to desktop and Android VR that needs no binding types. If a platform object is required, define a small core interface (like `PassthroughControl`) that both session managers implement, and expose it through an abstract hook (like `getPassthroughControl()`). |
| 4 | Desktop VR | `tamarin-desktop/.../openxr/OpenXrSessionManager.java`, `XrAppState.java`, `actions/XrActionAppState.java` | LWJGL bindings. |
| 5 | Android VR | `tamarin-android/.../openxr/OpenXrAndroidSessionManager.java`, `XrAndroidAppState.java`, `actions/XrActionAndroidAppState.java` | openxr-bindings-native. Mirror step 4 as closely as possible: same method names, same order, same log messages. That keeps the two files diffable. |
| 6 | Desktop simulation | `tamarin-core/.../openxr/DesktopSimulatingXrAppState.java`, `actions/DesktopSimulatingXrActionAppState.java` | Implement as a no-op or a sensible default (e.g. "always focused", "passthrough unsupported"). Never throw: apps rely on swapping VR and simulation freely. |
| 7 | Tests | `tamarin-core/src/test/...` | Unit test the core logic from step 1. |

## 3. Porting between desktop and Android

The two session managers are near-copies that differ in binding API. Typical translations:

| Desktop (LWJGL) | Android (`com.onemillionworlds.tamarin.openxrbindings`) |
|---|---|
| `try (MemoryStack stack = stackPush())` | `try (MemoryStack stack = MemoryStack.stackGet().push())` (`openxrbindings.memory.MemoryStack`) |
| Extension functions on per-extension classes, e.g. `FBPassthrough.xrCreatePassthroughFB(...)` | All functions on `XR10`, e.g. `XR10.xrCreatePassthroughFB(...)` |
| Constants on `XR10` / extension classes | `XR10Constants` |
| Output handle via `PointerBuffer` then `new XrFoo(pointer.get(0), session)` | Typed `XrFoo.HandleBuffer buf = XrFoo.create(1, stack)` then `buf.getByIndex(0)` |
| `org.lwjgl.openxr.XrSomeStruct.calloc(stack).type$Default()` | Usually the same shape, under `openxrbindings` |

To check whether the Android bindings have a function or struct, search the cached sources jar:

```
find ~/.gradle/caches/modules-2/files-2.1/com.onemillionworlds.tamarin/openxr-bindings-native -name "*-sources.jar"
unzip -l <jar> | grep -i <StructName>
unzip -p <jar> <path/to/XR10.java> | grep -n "<functionName>"
```

Use the version in `gradle/libs.versions.toml` (`openxrBindingsNative`). If something is missing there, the bindings
repo needs a release first. Tell the user rather than working around it.

To spot accidental drift after editing, diff the two files with the `Android` suffix normalised:

```
diff <(sed 's/Android//g' tamarin-android/src/main/java/tamarin/android/openxr/OpenXrAndroidSessionManager.java) \
     tamarin-desktop/src/main/java/com/onemillionworlds/tamarin/openxr/OpenXrSessionManager.java
```

The baseline diff is large (imports and binding idioms), so look for the hunks around the code you just changed.

## 4. Runtime robustness

- Guard extension use with `checkExtensionLoaded(...)`, or the session manager's own extension map. A missing
  extension should log once (e.g. `LOGGER.info` / `SingleOccurrenceLog`) and degrade gracefully, not crash.
- Interaction profiles that need an extension go in `InteractionProfileExtensions` so suggested bindings are skipped
  when it isn't loaded (see the `add-interaction-profile` skill).
- Session events are handled in each session manager's event polling. Route new event types into
  `XrSessionObservables` rather than exposing raw OpenXR events.

## 5. Verify

- `./gradlew :tamarin-core:test`, then `./gradlew build` (compiles Android too).
- Tell the user that headset behaviour is untested and suggest what to check in TamarinTestBed on desktop (SteamVR)
  and on Quest/Android.
