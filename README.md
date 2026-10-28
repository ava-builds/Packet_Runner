# Packet Runner: Network Town

A cozy isometric life-sim for learning computer networking. You are the town's network engineer:
neighbors phone you (install a router, fix the Wi-Fi, help with an IP address), you walk to their
building and solve the job in a small room with Scan, Cable, Device and Settings tools. Concepts follow
*Computer Networks: A Systems Approach* (Peterson and Davie).

Kotlin, Jetpack Compose. Packages: `game` (grid, pathfinding, requests, day clock, monitor), `network`
(the network simulator and scenarios), `data` (local save, notebook, optional cloud backup), `ui` (screens and drawing).

## What is in the town so far

Office (Network Monitor), houses (install a router), the cafe (hidden node, RTS/CTS), IP address jobs (DHCP pool and lease),
and City Hall (fix a router's forwarding table by setting subnet masks, longest prefix match), and the Apartments (NAT: fill the translation table, deliver replies, add a port-forwarding rule). The office also has a Lab with six mini-games (CRC error detection, line encoding, sliding window, hub to switch with a learning bridge, Mobile IP for a delivery driver, UDP or TCP for four apps) that pay once a day. The office also has a Workbench (subnet calculator) and an Upgrade shop (faster scooter, cheaper cable); upgrades are saved with the game.

## Playing offline

No account and no internet are needed. The game (day, money, town, friendship, upgrades), the Engineer's notebook and the work schedule and calls are saved on the phone with DataStore, and the app opens straight into the town.

## Firebase setup (optional Cloud backup)

The app builds and plays fully without Firebase; the Cloud backup section in Profile is hidden until `google-services.json` is added. To turn it on:

1. Open the [Firebase console](https://console.firebase.google.com) and create a project (Analytics can stay off).
2. In the project, add an **Android app** with the package name `com.avabuilds.packetrunner`.
3. Download `google-services.json` and put it in the `app/` folder (next to `app/build.gradle.kts`).
4. In the console open **Build → Authentication → Get started → Sign-in method** and switch on **Email/Password**.
5. Run the app again. In Profile → Cloud backup, sign up or log in, then use Back up now / Restore from cloud, change your nickname, log out, or delete the account.

## Saving the game (Firestore)

Your day, money, friendship, built networks and solved jobs are saved to Firestore under `players/{your uid}` a moment after they change
(look for the cloud mark next to your nickname: `...` saving, a tick saved, `!` failed). To turn it on:

1. In the Firebase console open **Build → Firestore Database → Create database** (production mode is fine, pick a nearby region).
2. Open the **Rules** tab, paste the contents of `firestore.rules` from this repo, and press **Publish**.
3. Run the app, log in, play a little, and check **Firestore Database → Data → players** for your document.

## Engineer's notebook

The 📓 pill opens your notebook: create, read, search, edit and delete notes. Notes live in Firestore under
`players/{uid}/notes` (the same rules cover them). After a job, the "What you just did" card can save the concepts you used as a note.
The notebook is kept on the phone; Back up now copies it to the cloud too.

## Tests

```
./gradlew testDebugUnitTest
```
