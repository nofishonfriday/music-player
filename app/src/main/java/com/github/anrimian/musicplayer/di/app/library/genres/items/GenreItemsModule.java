package com.github.anrimian.musicplayer.di.app.library.genres.items;

import static com.github.anrimian.musicplayer.di.app.SchedulerModule.UI_SCHEDULER;

import com.github.anrimian.filesync.SyncInteractor;
import com.github.anrimian.musicplayer.domain.interactors.library.LibraryGenresInteractor;
import com.github.anrimian.musicplayer.domain.interactors.player.LibraryPlayerInteractor;
import com.github.anrimian.musicplayer.domain.interactors.playlists.PlayListsInteractor;
import com.github.anrimian.musicplayer.domain.interactors.settings.DisplaySettingsInteractor;
import com.github.anrimian.musicplayer.domain.models.sync.FileKey;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;
import com.github.anrimian.musicplayer.ui.library.genres.items.GenreItemsPresenter;

import javax.annotation.Nonnull;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Scheduler;

@Module
public class GenreItemsModule {

    private final long id;

    public GenreItemsModule(long id) {
        this.id = id;
    }

    @Provides
    @Nonnull
    GenreItemsPresenter genreItemsPresenter(LibraryGenresInteractor interactor,
                                            PlayListsInteractor playListsInteractor,
                                            LibraryPlayerInteractor playerInteractor,
                                            DisplaySettingsInteractor displaySettingsInteractor,
                                            SyncInteractor<FileKey, ?, Long> syncInteractor,
                                            ErrorParser errorParser,
                                            @Named(UI_SCHEDULER) Scheduler uiScheduler) {
        return new GenreItemsPresenter(id,
                interactor,
                playListsInteractor,
                playerInteractor,
                displaySettingsInteractor,
                syncInteractor,
                errorParser,
                uiScheduler);
    }
}
