package com.jkjobs.app.data.remote

/**
 * ===========================================================================
 *  EDIT THIS FILE WHEN A SOURCE WEBSITE CHANGES ITS LAYOUT
 * ===========================================================================
 * Government / university sites redesign without notice. When notifications
 * stop appearing for a source, the fix is almost always here:
 *   1. Open the source URL in a desktop browser.
 *   2. Right-click a notification link -> "Inspect".
 *   3. Find the repeating container element (a <tr>, <li>, or <div> that
 *      wraps EACH notification row) and update `rowSelector`.
 *   4. Update `linkSelector` to the <a> tag inside that row (usually just "a").
 *
 * `rowSelector` and `linkSelector` are CSS selectors, same syntax as jQuery.
 */
data class JobSource(
    val name: String,
    val listUrl: String,
    val rowSelector: String,
    val linkSelector: String = "a",
    // Some sites put the date in a sibling <td>/<span> - selector relative to the row.
    val dateSelector: String? = null
)

object JobSources {

    val ALL = listOf(
        JobSource(
            name = "JKSSB",
            listUrl = "https://jkssb.nic.in/",
            rowSelector = "table tr",          // JKSSB lists notifications in a table on the homepage
            linkSelector = "a",
            dateSelector = "td:nth-child(1)"
        ),
        JobSource(
            name = "JKPSC",
            listUrl = "https://jkpsc.nic.in/",
            rowSelector = "table tr",
            linkSelector = "a",
            dateSelector = "td:nth-child(1)"
        ),
        JobSource(
            name = "University of Kashmir",
            listUrl = "https://www.kashmiruniversity.net/notifications.aspx",
            // This is an ASP.NET page; notifications usually render as a list of <a> tags
            // inside a repeater/grid. "table tr" is the safest first guess - adjust if empty.
            rowSelector = "table tr",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "SKIMS Srinagar",
            listUrl = "https://www.skims.ac.in/index.php?option=com_content&view=category&id=91:recruitment",
            // Joomla-based site - recruitment notices are usually list items under a category blog layout.
            rowSelector = "div.item, li.item, .cat-list-row",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "University of Jammu",
            listUrl = "https://jammuuniversity.ac.in/",
            // The homepage has a scrolling "New Jobs" / notifications ticker - selector may need
            // tightening to just that ticker element once you inspect the live page.
            rowSelector = "table tr, marquee a, .notice a",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "Central University of Jammu",
            listUrl = "https://www.cujammu.ac.in/en/viewAllJobs/",
            // This is a clean dedicated jobs listing page - usually one row per job in a table or card list.
            rowSelector = "table tr, .job-list-item, li",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "J&K Police",
            listUrl = "https://www.jkpolice.gov.in/Recruitment-Notification",
            rowSelector = "table tr, .view-content .views-row",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "SMVDU",
            listUrl = "https://smvdu.ac.in/jobs/",
            // WordPress site - notices are usually list items or article blocks.
            rowSelector = "li, article, .post",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "BGSBU",
            listUrl = "https://www.bgsbu.ac.in/recruitment/2025/home.aspx",
            // ASP.NET page - update the year in the URL each year, or switch to whatever
            // the current "Recruitment" quick-link on bgsbu.ac.in points to.
            rowSelector = "table tr",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "IUST",
            listUrl = "https://recruitment.iust.ac.in/",
            rowSelector = "table tr, li, .job-item",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "SKUAST Jammu",
            listUrl = "https://www.skuastjammu.ac.in/",
            // Homepage notice board - tighten this selector to the notices widget once you inspect it.
            rowSelector = "table tr, marquee a, .notice a",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "SKUAST Kashmir",
            listUrl = "https://www.skuastkashmir.ac.in/",
            rowSelector = "table tr, marquee a, .notice a",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "GMC Jammu",
            listUrl = "https://www.gmcjammu.nic.in/",
            rowSelector = "table tr, marquee a, .notice a",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "GMC Srinagar",
            listUrl = "https://www.gmcs.ac.in/",
            rowSelector = "table tr, marquee a, .notice a",
            linkSelector = "a",
            dateSelector = null
        ),
        JobSource(
            name = "JKUpdates",
            listUrl = "https://jkadworld.com/category/jammu-kashmir-jobs/",
            // WordPress blog layout - each post is usually an <article> or <h2 class="entry-title">.
            // This one aggregates JKSSB/JKPSC/universities/private jobs all in one place, so expect
            // some overlap with the dedicated sources above - that's fine, duplicates are deduped by link.
            rowSelector = "article, h2.entry-title, .post",
            linkSelector = "a",
            dateSelector = "time"
        ),
        JobSource(
            name = "Jehlum",
            listUrl = "https://jehlum.in/jobs/",
            // Clean WordPress blog layout - each posting is an <h2> with a single link inside.
            // Posts here are mostly PRIVATE/local jobs (shops, schools, companies) rather than
            // government recruitment, and post very frequently (often multiple per hour).
            rowSelector = "h2",
            linkSelector = "a",
            dateSelector = null
        )
        // Add more sources here, e.g. Central University of Kashmir, JKBOSE,
        // by copying a JobSource block above and adjusting the selectors.
    )
}
