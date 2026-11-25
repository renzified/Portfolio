package com.rsalanga.knowledgebase.fragment.mvvm.shell

import com.rsalanga.knowledgebase.fragment.mvvm.core.UiText
import com.rsalanga.knowledgebase.fragment.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.fragment.mvvm.data.session.Session
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.CatalogKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.SignInKey
import com.rsalanga.knowledgebase.fragment.mvvm.navigation.sessionRedirect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellTest {

    @Test
    fun `N6 - messages queue up and are removed by id once shown`() {
        val messages = UserMessages()

        messages.post(UiText.Raw("first"))
        messages.post(UiText.Raw("second"))
        val head = messages.messages.value.first()
        messages.shown(head.id)

        assertEquals(listOf(UiText.Raw("second")), messages.messages.value.map { it.text })
    }

    @Test
    fun `N7 - signing out sends any screen to sign-in`() {
        assertEquals(SignInKey, sessionRedirect(Session.SignedOut, isOnSignIn = false))
    }

    @Test
    fun `N7 - signing in leaves sign-in for the catalog`() {
        assertEquals(CatalogKey, sessionRedirect(Session.SignedIn("Ana"), isOnSignIn = true))
    }

    @Test
    fun `N7 - a back stack that already matches the session is left alone`() {
        assertNull(sessionRedirect(Session.SignedIn("Ana"), isOnSignIn = false))
        assertNull(sessionRedirect(Session.SignedOut, isOnSignIn = true))
        assertNull(sessionRedirect(Session.Unknown, isOnSignIn = false))
    }
}
