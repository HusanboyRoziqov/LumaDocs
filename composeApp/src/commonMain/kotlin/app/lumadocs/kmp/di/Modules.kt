package app.lumadocs.kmp.di

import app.lumadocs.kmp.utils.PendingUploadStore
import app.lumadocs.kmp.viewmodels.AddScreenViewModel
import app.lumadocs.kmp.viewmodels.DocumentDetailViewModel
import app.lumadocs.kmp.viewmodels.DocumentsViewModel
import app.lumadocs.kmp.viewmodels.SettingsViewModel
import app.lumadocs.kmp.viewmodels.StartScreenViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: Module

val commonModule = module {
    single { PendingUploadStore(get()) }
    viewModel { StartScreenViewModel(get()) }
    viewModel { AddScreenViewModel() }
    viewModel { DocumentsViewModel() }
    viewModel { DocumentDetailViewModel() }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
