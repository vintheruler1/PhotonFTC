# NOTICE - Forked

Hi I'm Vin from 30435 Klutch Robotics and this is a fork of Eeshwar's PhotonFTC lib (but renamed to ~ to match our theming + parts after a clutch). I will be attempting to maintain this and keep it working and hopefully adding on future hardwareDevices.

# PhotonFTC
[![](https://jitpack.io/v/vintheruler1/PhotonFTC.svg)](https://jitpack.io/#vintheruler1/PhotonFTC)

This project is an initiative to push the FIRST api to its absolute limits, and to **give access to functionality and methods not normally exposed to the user**

## Documentation:
 - [Legacy PhotonCore Overview (not mine)](https://photondocs.pages.dev/)

## Installation instructions :
Installation is simple. Just go to build.gradle in your **TeamCode** module and add the following under **repositories**

```
repositories {
    maven { url = 'https://jitpack.io' }
}
```

Then add the following under **dependencies** (in TeamCode)

```
dependencies {
    implementation 'com.github.vintheruler1:PhotonFTC:v1.0.1'
}
```

Then run a gradle sync, and everything should download!

## OnBotJava:
Is not supported.
