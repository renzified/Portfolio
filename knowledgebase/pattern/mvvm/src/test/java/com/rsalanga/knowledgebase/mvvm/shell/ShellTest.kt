package com.rsalanga.knowledgebase.mvvm.shell

import com.rsalanga.knowledgebase.mvvm.core.UiText
import com.rsalanga.knowledgebase.mvvm.data.messages.UserMessages
import com.rsalanga.knowledgebase.mvvm.data.session.Session
import com.rsalanga.knowledgebase.mvvm.navigation.AccountKey
import com.rsalanga.knowledgebase.mvvm.navigation.CatalogKey
import com.rsalanga.knowledgebase.mvvm.navigation.SignInKey
import com.rsalanga.knowledgebase.mvvm.navigation.backStackFor
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
    fun `N7 - signing out replaces the stack with sign-in`() {
        assertEquals(
            listOf(SignInKey),
            backStackFor(Session.SignedOut, listOf(CatalogKey, AccountKey))
        )
    }

    @Test
    fun `N7 - signing in leaves sign-in for the catalog`() {
        assertEquals(listOf(CatalogKey), backStackFor(Session.SignedIn("Ana"), listOf(SignInKey)))
    }

    @Test
    fun `N7 - a stack that already matches the session is left alone`() {
        assertNull(backStackFor(Session.SignedIn("Ana"), listOf(CatalogKey, AccountKey)))
        assertNull(backStackFor(Session.SignedOut, listOf(SignInKey)))
        assertNull(backStackFor(Session.Unknown, listOf(CatalogKey)))
    }
}
