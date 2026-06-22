# JK Job Alerts — Android Studio Project

A native Android app (Kotlin + Jetpack Compose) that tracks job notifications for
Jammu & Kashmir from JKSSB, JKPSC, and any other source you add — with saved jobs
and push-style local alerts when new postings appear.

## How to open this project
1. Install **Android Studio** (latest stable).
2. `File → Open` and select this folder (`JKJobsApp`).
3. Let Gradle sync (first sync downloads dependencies — needs internet).
4. Run on an emulator or your phone (`Run ▶`).

## What's actually inside
- **Feed tab** — live list of notifications, pulled directly from JKSSB and JKPSC's
  websites by the app itself (no backend server required).
- **Saved tab** — bookmark any posting; stored locally in a Room database.
- **Alerts tab** — choose how often (1h / 3h / 6h / daily) the app checks for new
  postings in the background, using WorkManager. A notification fires when new
  ones are found.

## Sources currently included (16 total)
**Aggregators**
- **JKUpdates** — jkadworld.com — pulls from JKSSB, JKPSC, universities, and private
  jobs all in one feed. Good as a catch-all / backup, since it'll often have a
  notification even if one of the dedicated source selectors below temporarily breaks.
- **Jehlum** — jehlum.in/jobs — mostly local/private listings (shops, schools, small
  companies), posts very frequently. Heads up: this one alone can post several jobs
  an hour, so it'll likely dominate your feed and notifications volume. If that's too
  noisy, consider giving Jehlum its own longer alert interval, or filtering it out of
  notifications while still keeping it in the feed.

  **This is already handled for you**: `JobRepository.kt` has a
  `lowPrioritySourcesForAlerts` set (currently just `"Jehlum"`) — postings from
  sources in that set still appear in the Feed and Saved tabs normally, they just
  don't trigger a push notification. Add/remove names from that set to change which
  sources are allowed to ping you.

**Recruitment boards**
- **JKSSB** — jkssb.nic.in
- **JKPSC** — jkpsc.nic.in
- **J&K Police** — jkpolice.gov.in

**Universities**
- **University of Kashmir** — kashmiruniversity.net
- **University of Jammu** — jammuuniversity.ac.in
- **Central University of Jammu** — cujammu.ac.in
- **SMVDU** — smvdu.ac.in
- **BGSBU** — bgsbu.ac.in
- **IUST** — recruitment.iust.ac.in
- **SKUAST Jammu** — skuastjammu.ac.in
- **SKUAST Kashmir** — skuastkashmir.ac.in

**Medical colleges / institutes**
- **SKIMS Srinagar** — skims.ac.in
- **GMC Jammu** — gmcjammu.nic.in
- **GMC Srinagar** — gmcs.ac.in

All sources are fetched in parallel on every refresh (see `JobScraper.kt`), so adding
more sources later won't slow things down much.

**Note on JKUpdates overlap:** since JKUpdates re-posts notifications that also come
from the dedicated sources above, you may sometimes see what's effectively the same
notification twice in the feed (once linking to the official site, once linking to
JKUpdates' article about it). The dedup logic only catches exact-link duplicates, not
"this is the same news from two different pages." If that gets noisy, the easiest fix
is to remove JKUpdates from `JobSources.kt` and keep it as a single fallback, or vice
versa — keep JKUpdates and remove the harder-to-scrape dedicated sources.

JKSSB/JKPSC/Central University of Jammu use plain HTML tables or a dedicated jobs
page, so they're the most reliable out of the box. The rest use heavier templated
platforms or homepage notice-boards/marquees, so they use broader selectors and are
more likely to need the quick tightening described below after your first build.

The newer ones (Kashmir Uni, SKIMS, Jammu Uni, J&K Police) use slightly looser,
best-guess selectors since their layouts are more template-heavy (Joomla/Drupal/
ASP.NET) than JKSSB/JKPSC's simple tables. They're more likely to need a selector
tweak after your first build — see the section below.

## The honest limitation — please read this
JKSSB and JKPSC don't publish a public API. This app works by **parsing the HTML**
of their notification pages directly from the phone (see `data/remote/JobScraper.kt`).
That's normal for this kind of app (most "sarkari naukri" apps work the same way),
but it means:

- If JKSSB/JKPSC redesign their website, the scraper will return nothing until you
  update the CSS selectors.
- **All the selectors live in one file: `data/remote/JobSources.kt`.** That file has
  step-by-step comments on how to find the right selector using your browser's
  "Inspect" tool. This is a 2-minute fix, not a rewrite, when it happens.
- I wrote generic best-guess selectors (`table tr`, first link in each row) based on
  how these sites are typically structured. **Test it after building — if the feed
  comes back empty, open jkssb.nic.in / jkpsc.nic.in in a browser, inspect a
  notification row, and adjust `rowSelector` / `linkSelector` accordingly.**

## Adding more sources (University of Kashmir, J&K Police, SKIMS, etc.)
Open `JobSources.kt` and add another `JobSource(...)` entry with that site's URL and
selectors. No other code changes needed — the scraper and UI both already loop over
the whole list.

## Things you may want to add later
- ~~An app icon~~ — done. A chinar leaf (Kashmir's iconic autumn tree) in amber on
  deep teal, with a small alert badge — ties the app to the region instead of a
  generic briefcase icon. It's a full adaptive icon (foreground/background layers
  for API 26+, flattened PNG fallback for older devices), generated at all 5
  density buckets. See `app_icon_preview.png` alongside this zip for a quick look,
  or `mipmap-anydpi-v26/ic_launcher.xml` in the project for the adaptive descriptor.
- Filtering by category (e.g. only show "10th pass" or "graduate" posts) — would
  need simple keyword matching added to `JobRepository`.
- A "private jobs" source — same pattern, just add another `JobSource`.
