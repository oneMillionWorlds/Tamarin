---
name: testbed-example
description: Add or update an example of a Tamarin feature in the two example apps, TamarinTestBed (PCVR/desktop) and TamarinTestBedAndroid (Quest/Android), and hand it to the user to test. Covers bumping the alpha version, publishing Tamarin to mavenLocal and pointing both testbeds at it. Use when asked to add a testbed example, demo a new feature, or get a Tamarin change ready for the user to test on a headset.
---

# Adding a testbed example

New Tamarin features usually get a matching example in two sibling repos:

| Repo | Default location | Example package |
|---|---|---|
| TamarinTestBed (PCVR, also desktop simulation) | `../TamarinTestBed` | `src/main/java/example/` (package `example`) |
| TamarinTestBedAndroid (Quest) | `../TamarinTestBedAndroid` | `app/src/main/java/com/onemillionworlds/example/` (package `com.onemillionworlds.example`) |

## 0. Check the repos are present

Look for both directories as siblings of this repo. If either is missing, **stop and ask the user to check it out**
(https://github.com/oneMillionWorlds/TamarinTestBed and
https://github.com/oneMillionWorlds/TamarinTestBedAndroid, as siblings of `Tamarin`). Do not clone them yourself.

Run `git status` in each. The user often has work in progress there, so don't overwrite, revert or "tidy" files you
didn't create. Don't commit in the testbeds unless asked.

## 1. Bump the Tamarin alpha version and publish locally

1. In this repo's `gradle.properties`, increment the alpha number (e.g. `version=3.2.0-alpha9` becomes
   `3.2.0-alpha10`). Do this once for each round of changes handed to the user to test, so it is unambiguous which
   build a testbed is running. If the version isn't an alpha (e.g. a plain release version), use -alpha1.
2. `./gradlew publishToMavenLocal` publishes `tamarin-core`, `tamarin` (desktop) and `tamarin-android`. Signing is
   skipped automatically without a GPG key.
3. In **both** testbeds, set `tamarin = "<new version>"` under `[versions]` in `gradle/libs.versions.toml`. Both
   already list `mavenLocal()` as a repository. Leave the commented-out `includeBuild('../Tamarin')` block in
   TamarinTestBed's `settings.gradle` alone; the mavenLocal route is the one used.

If Tamarin is changed again during the session, repeat all three steps with another increment.

## 2. Write the example

Each example is a self-contained JME `BaseAppState` named `<Feature>ExampleState`. The README explains that each one
is meant to inspire a separate project, so self-contained duplication is intentional. Read one or two recent examples
in full before writing (`PassthroughExampleState`, `GameplayInterruptExampleState` and `SessionFocusExampleState` are
good references) and match their structure:
- a Lemur window with explanatory text and a "Back to menu" style exit to `MenuExampleState`;
- `XrBaseAppState` / `XrActionBaseAppState` obtained by ID, never the platform-specific classes, so the same code works
  in VR and simulation;
- thorough comments, because the examples double as documentation.

**Write the example once and copy it to the other repo.** The two copies of an example should be identical apart from
the `package` line and the `...example.actions.ActionHandles` / `ActionSets` imports. Diff them afterwards to confirm:

```
diff ../TamarinTestBed/src/main/java/example/FooExampleState.java \
     ../TamarinTestBedAndroid/app/src/main/java/com/onemillionworlds/example/FooExampleState.java
```

Only diverge if the feature genuinely differs by platform, and explain why in a comment.

## 3. Wire it into each app

| What | PCVR (TamarinTestBed) | Android (TamarinTestBedAndroid) |
|---|---|---|
| Menu button | `example/MenuExampleState.java`: add a `lemurWindow.addChild(new Button("..."))` block (above the "Utilities" label) that detaches the menu and attaches the example | `MenuExampleState.java`: same block (above the "Exit" button) |
| `XrSettings` options | `example/Main.java` `main(...)` | `game/TamarinTestBedAndroid.java`, where `XrAndroidAppState` is attached |
| New actions / bindings | `Main.manifest()` plus `example/actions/ActionHandles` | `example/Manifest.java` plus `example/actions/ActionHandles` |
| Platform declarations | n/a | `app/src/main/AndroidManifest.xml` (`uses-feature` / `uses-permission`, e.g. Quest hand tracking, passthrough), always with a comment and `android:required="false"` where the feature is optional |

If the feature also matters in desktop simulation, check `example/MainVrSimulationMode.java` too.

Comment any `XrSettings` lines you add with what they enable and which example uses them, matching existing lines.

## 4. Compile-check, then hand over

- PCVR: `./gradlew compileJava` in `../TamarinTestBed`
- Android: `./gradlew :app:compileDebugJavaWithJavac` in `../TamarinTestBedAndroid`

Don't launch the apps or install to a device unless asked. The user tests on real hardware. Finish with a short
hand-over:
- the Tamarin version published, and confirmation that both testbeds point to it;
- the menu button text to look for in each app;
- what to try and what correct behaviour looks like (e.g. "open the system menu: the cube should stop spinning and
  the status text should change to Paused");
- anything that can only be checked on one platform, or that needs specific hardware or runtime support.
