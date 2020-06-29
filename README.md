![Perspective](./app/src/main/res/drawable/banner.png)

PerspectivePotVAndroid
======================

This is an Android implementation of Perspective: Perils of the Void, a 3D puzzle game.

Prerequisites
=============

* Android SDK usually as part of [AndroidStudio](https://developer.android.com/studio).
Specifically the location of the android sdk/ndk must be known. Either add the ANDROID_SDK_ROOT
environment variable `export ANDROID_SDK_ROOT=<path_to_sdk>` or as part of  `local.properties`:
```
ndk.dir=<path_to_ndk>
sdk.dir=<path_to_sdk>
```

Setup
=====

Use [PerspectiveSuite](https://github.com/AletheiaWareLLC/PerspectiveSuite)
to build PerspectivePotVAndroid

Build
=====
Build debug and release version

```
    ./gradlew build
```

Clean
=====
Clean all versions

```
./gradlew clean
```

Install
=======

Install debug version

```
./gradlew InstallDebug
```

Installs and runs the test for debug

```
./gradlew connectedDebugAndroidTest
```

Info
====

To see the list of tasks

```
./gradlew tasks
```
