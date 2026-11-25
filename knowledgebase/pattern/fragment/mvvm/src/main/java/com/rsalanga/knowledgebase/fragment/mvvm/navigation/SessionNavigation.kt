package com.rsalanga.knowledgebase.fragment.mvvm.navigation

import com.rsalanga.knowledgebase.fragment.mvvm.data.session.Session

/**
 * FMVVM-N7: maps the session to the destination the app must show, or `null` when the current
 * back stack is already valid. The Activity (the NavController's owner) applies it by navigating
 * there and clearing everything below.
 *
 * Pure, so it is unit-tested without Android (`ShellTest`).
 */
fun sessionRedirect(session: Session, isOnSignIn: Boolean): Any? = when (session) {
    Session.Unknown -> null
    Session.SignedOut -> if (isOnSignIn) null else SignInKey
    is Session.SignedIn -> if (isOnSignIn) CatalogKey else null
}
