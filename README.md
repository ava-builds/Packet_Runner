# Packet Runner: Network Town

A cozy isometric life-sim for learning computer networking. You are the town's network engineer:
neighbors phone you (install a router, fix the Wi-Fi, help with an IP address), you walk to their
building and solve the job in a small room with Scan, Cable, Device and Settings tools. Concepts follow
*Computer Networks: A Systems Approach* (Peterson and Davie).

Kotlin, Jetpack Compose. Packages: `game` (grid, pathfinding, requests, day clock, monitor), `network`
(the network simulator and scenarios), `data` (accounts), `ui` (screens and drawing).

## Firebase setup (needed for log in)

The app builds without Firebase and then shows "Firebase is not set up yet". To turn accounts on:

1. Open the [Firebase console](https://console.firebase.google.com) and create a project (Analytics can stay off).
2. In the project, add an **Android app** with the package name `com.avabuilds.packetrunner`.
3. Download `google-services.json` and put it in the `app/` folder (next to `app/build.gradle.kts`).
4. In the console open **Build → Authentication → Get started → Sign-in method** and switch on **Email/Password**.
5. Run the app again. Sign up, log in, change your nickname, log out, and delete the account from the 👤 pill.

## Tests

```
./gradlew testDebugUnitTest
```
