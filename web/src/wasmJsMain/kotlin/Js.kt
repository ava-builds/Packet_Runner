// The few things only the browser can do. Each is a one-line JavaScript function the Kotlin code calls.

@JsFun("(key) => { try { return localStorage.getItem(key); } catch (e) { return null; } }")
external fun jsStorageGet(key: String): String?

@JsFun("(key, value) => { try { localStorage.setItem(key, value); return true; } catch (e) { return false; } }")
external fun jsStorageSet(key: String, value: String): Boolean

@JsFun("(key) => { try { localStorage.removeItem(key); } catch (e) { } }")
external fun jsStorageRemove(key: String)

/** Plays notes given as "hz:ms,hz:ms" with the Web Audio API. */
@JsFun(
    """(spec) => {
        try {
            const Ctx = window.AudioContext || window.webkitAudioContext;
            const ctx = window.__prAudio || (window.__prAudio = new Ctx());
            if (ctx.state === 'suspended') ctx.resume();
            let t = ctx.currentTime + 0.02;
            spec.split(',').forEach((n) => {
                const parts = n.split(':'); const hz = Number(parts[0]); const sec = Number(parts[1]) / 1000;
                const osc = ctx.createOscillator(); const gain = ctx.createGain();
                osc.type = 'sine'; osc.frequency.value = hz;
                gain.gain.setValueAtTime(0.0001, t);
                gain.gain.exponentialRampToValueAtTime(0.35, t + 0.006);
                gain.gain.exponentialRampToValueAtTime(0.0001, t + sec);
                osc.connect(gain); gain.connect(ctx.destination);
                osc.start(t); osc.stop(t + sec + 0.05);
                t += sec;
            });
        } catch (e) { }
    }""",
)
external fun jsPlayNotes(spec: String)

@JsFun("() => (typeof Notification === 'undefined') ? 'unsupported' : Notification.permission")
external fun jsNotificationPermission(): String

@JsFun("() => { try { if (typeof Notification !== 'undefined') { const p = Notification.requestPermission(); if (p && p.catch) p.catch(() => {}); } } catch (e) { } }")
external fun jsRequestNotificationPermission()

/** Shows a notification after [delayMs] if the page is in the background and the player allowed it. */
@JsFun(
    """(id, delayMs, title, body) => {
        window.__prTimers = window.__prTimers || {};
        clearTimeout(window.__prTimers[id]);
        window.__prTimers[id] = setTimeout(() => {
            try {
                if (document.hidden && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
                    const n = new Notification(title, { body: body, tag: 'call-' + id });
                    n.onclick = () => { window.focus(); n.close(); };
                }
            } catch (e) { }
        }, delayMs);
    }""",
)
external fun jsScheduleNotification(id: Int, delayMs: Double, title: String, body: String)

@JsFun("(id) => { if (window.__prTimers) { clearTimeout(window.__prTimers[id]); delete window.__prTimers[id]; } }")
external fun jsCancelNotification(id: Int)

@JsFun("() => { if (window.__prTimers) { Object.keys(window.__prTimers).forEach((k) => clearTimeout(window.__prTimers[k])); window.__prTimers = {}; } }")
external fun jsCancelAllNotifications()

@JsFun("() => window.location.search")
external fun jsLocationSearch(): String
