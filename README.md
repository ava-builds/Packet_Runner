# Packet Runner: Network Town

A cozy isometric life-sim for learning computer networking. You are the town's network engineer:
neighbors phone you (install a router, fix the Wi-Fi, help with an IP address, solve a puzzling case), you walk to their
building and solve the job. Concepts follow *Computer Networks: A Systems Approach* (Peterson and Davie).

The game is **fully playable offline**: no account and no internet are needed, and everything is saved on the phone.

Kotlin, Jetpack Compose (minSdk 24). Open the project in Android Studio and run the `app` configuration.
Packages: `game` (town, pathfinding, requests, day clock, monitor, XP, upgrades), `network` (the simulator, scenarios,
puzzles and case files), `work` (the real-time work life), `notify` (call notifications), `data` (local save, notebook,
optional cloud backup) and `ui` (screens and drawing).

## What is in the game

**Customer jobs.** Neighbors phone you and you go to their building:

- *Install* (houses without internet), *Fix* (the Wi-Fi keeps dropping; the cafe has a card reader that needs low latency)
  and *Help* ("out of IP addresses": DHCP pool and lease). You work in a small room with Scan, Cable, Device and Settings tools.
- *City Hall*: fix a router's forwarding table by setting subnet masks (longest prefix match).
- *Apartments*: NAT. Fill the translation table, deliver replies, add a port-forwarding rule.
- *Case jobs* (called by houses that already have internet): look-and-fix jobs with a story, symptoms, scan tables, four
  possible actions (wrong ones cost money and explain themselves) and a concept card in English and Korean.
  There are fifteen cases:
  ARP (stale cache, wrong gateway; MAC changes per hop while IP stays), IP fragmentation (MTU and offsets),
  ICMP and TTL (ping and traceroute: a slow hop, a routing loop, a silent router),
  Wi-Fi (the exposed node and the hidden node), an office tower (hub to switch, a switch that remembers a printer's old port),
  a delivery van (Mobile IP: expired registration, reverse tunnel) and a streamer and a bank (UDP against TCP).

**The office.** The Network Monitor (catch problems early for a bonus), a Lab with six mini-games that pay once a day
(CRC error detection, line encoding, sliding window, hub to switch with a learning bridge, Mobile IP for a delivery driver,
UDP or TCP for four apps), a Workbench (subnet calculator) and an Upgrade shop (faster scooter, cheaper cable spool and three
decorations for your office room). Upgrades are saved with the game.

**Progress.** Every job, Lab game and early catch gives XP and levels you up. Neighbors have friendship hearts. The town has
day and night, little people and cars that move around, and bubbles over buildings that need you.

**Real-time work life.** The game follows your real clock. On first launch you choose your work week (hours, lunch, days off).

- Customer calls come during work hours and the phone rings; clock in and out yourself or let the game do it.
  A call you miss goes to the voicemail inbox, where you can return it until clock-out. Calls left at clock-out make those neighbors a little sad.
- Notifications ("customer_calls") arrive even when the app is closed (WorkManager). The app asks for the notification permission once.
- Lunch with a neighbor, and a Weekly topic card (one of ten topics of the course, with a way to practise it).
- Friday payday with a bonus for the week's calls, free time after work and on days off (your office room), and a Saturday market for gifts.
- **Demo mode** (Profile → Work settings) runs the clock 60 times faster, so a work day passes in 9 minutes. Handy for presentations.

## Saving on the phone

The game (day, money, town, friendship, upgrades, XP), the Engineer's notebook and the work schedule and calls are saved on the phone
with DataStore, a moment after they change. The app opens straight into the town; there is no login. Closing the app loses nothing.

## Engineer's notebook

The Notebook tab opens your notes: create, read, search, edit and delete. After a job, the "What you just did" card can save the
concepts you used as a note. Notes are kept on the phone.

## Optional: cloud backup

Firebase login and Firestore are an optional **Cloud backup** in Profile. The app builds and plays fully without Firebase; the
Cloud backup section in Profile is hidden until `google-services.json` is added. With it you can log in and copy your town and notebook
to your account (and restore them on another phone). While you are logged in, every change to the game is copied to the cloud as well
(look for the cloud mark in the top bar: `...` saving, a tick saved, `!` failed). The copy on the phone is always the one you play.

### Firebase setup

1. Open the [Firebase console](https://console.firebase.google.com) and create a project (Analytics can stay off).
2. In the project, add an **Android app** with the package name `com.avabuilds.packetrunner`.
3. Download `google-services.json` and put it in the `app/` folder (next to `app/build.gradle.kts`).
4. In the console open **Build → Authentication → Get started → Sign-in method** and switch on **Email/Password**.
5. Run the app again. In Profile → Cloud backup, sign up or log in, then use **Back up now** / **Restore from cloud**, change your nickname,
   log out, or delete the account (this removes the online copy only; the game on the phone stays).

### Firestore

Your save is stored in one document, `players/{your uid}`, and your notes in `players/{uid}/notes`. To turn it on:

1. In the Firebase console open **Build → Firestore Database → Create database** (production mode is fine, pick a nearby region).
2. Open the **Rules** tab, paste the contents of `firestore.rules` from this repo, and press **Publish**. The rules let each player read and write only their own documents.
3. Run the app, log in, play a little, and check **Firestore Database → Data → players** for your document.

## Tests

```
./gradlew testDebugUnitTest
```

The rules of the game and the networking logic are plain Kotlin and are unit-tested (the game engine, request planning, the simulator, the
puzzles, every case file, the work clock and calls, saving and the notebook).
