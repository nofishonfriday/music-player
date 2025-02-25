package com.github.anrimian.musicplayer.di.app.library.compositions;

import static com.github.anrimian.musicplayer.di.app.SchedulerModule.UI_SCHEDULER;

import com.github.anrimian.filesync.SyncInteractor;
import com.github.anrimian.musicplayer.domain.interactors.library.LibraryCompositionsInteractor;
import com.github.anrimian.musicplayer.domain.interactors.player.LibraryPlayerInteractor;
import com.github.anrimian.musicplayer.domain.interactors.playlists.PlayListsInteractor;
import com.github.anrimian.musicplayer.domain.interactors.settings.DisplaySettingsInteractor;
import com.github.anrimian.musicplayer.domain.models.sync.FileKey;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;
import com.github.anrimian.musicplayer.ui.library.compositions.LibraryCompositionsPresenter;

import javax.annotation.Nonnull;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Scheduler;

/**
 * Created on 31.10.2017.
 */

@Module
public class LibraryCompositionsModule {

    @Provides
    @Nonnull
    LibraryCompositionsPresenter libraryCompositionsPresenter(LibraryCompositionsInteractor interactor,
                                                              PlayListsInteractor playListsInteractor,
                                                              LibraryPlayerInteractor playerInteractor,
                                                              DisplaySettingsInteractor displaySettingsInteractor,
                                                              SyncInteractor<FileKey, ?, Long> syncInteractor,
                                                              ErrorParser errorParser,
                                                              @Named(UI_SCHEDULER) Scheduler uiScheduler) {
        return new LibraryCompositionsPresenter(interactor,
                playListsInteractor,
                playerInteractor,
                displaySettingsInteractor,
                syncInteractor,
                errorParser,
                uiScheduler);
    }

}
