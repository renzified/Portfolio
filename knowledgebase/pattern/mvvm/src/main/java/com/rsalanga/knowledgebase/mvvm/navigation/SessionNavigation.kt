package com.rsalanga.knowledgebase.mvvm.navigation

import androidx.navigation3.runtime.NavKey
import com.rsalanga.knowledgebase.mvvm.data.session.Session

/**
 * MVVM-N7: maps the session to the back stack it requires, or `null` when [current] is already
 * valid. Pure, so it is unit-tested without Compose (`SessionNavigationTest`).
 */
fun backStackFor(session: Session, current: List<NavKey>): List<NavKey>? = when (session) {
    Session.Unknown -> null
    Session.SignedOut -> if (current == listOf(SignInKey)) null else listOf(SignInKey)
    is Session.SignedIn -> if (SignInKey in current) listOf(CatalogKey) else null
}
