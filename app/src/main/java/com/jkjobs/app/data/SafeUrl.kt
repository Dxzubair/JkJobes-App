package com.jkjobs.app.data

/** Defense-in-depth: even though the scraper only keeps https:// links, the UI re-validates
 *  before launching any Intent, so a future scraper change/bug can't smuggle a non-http(s)
 *  or malformed URI into an ACTION_VIEW intent. */
object SafeUrl {
    fun isSafeToOpen(url: String): Boolean =
        url.startsWith("https://") && url.length < 2048 && !url.contains(" ")
}
