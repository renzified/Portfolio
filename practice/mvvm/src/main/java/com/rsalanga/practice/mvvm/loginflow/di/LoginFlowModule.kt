package com.rsalanga.practice.mvvm.loginflow.di

import com.rsalanga.practice.mvvm.loginflow.identifier.IdentifierViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Every binding the login flow needs. Nothing outside `loginflow` should reference this module.
 */
internal val loginFlowModule = module {
    viewModelOf(::IdentifierViewModel)
}
