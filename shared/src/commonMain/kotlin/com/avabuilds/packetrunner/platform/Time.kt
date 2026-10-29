package com.avabuilds.packetrunner.platform

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Milliseconds since 1970-01-01 UTC. */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
