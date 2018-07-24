package com.github.anrimian.simplemusicplayer.ui.storage_library_screen;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.github.anrimian.simplemusicplayer.domain.business.library.StorageLibraryInteractor;
import com.github.anrimian.simplemusicplayer.domain.models.composition.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.composition.folders.FileSource;
import com.github.anrimian.simplemusicplayer.domain.models.composition.folders.Folder;
import com.github.anrimian.simplemusicplayer.domain.models.composition.folders.FolderFileSource;
import com.github.anrimian.simplemusicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.simplemusicplayer.ui.common.error.parser.ErrorParser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created on 23.10.2017.
 */

@InjectViewState
public class StorageLibraryPresenter extends MvpPresenter<StorageLibraryView> {

    private final StorageLibraryInteractor interactor;
    private final ErrorParser errorParser;
    private final Scheduler uiScheduler;

    @Nullable
    private String path;

    private List<FileSource> sourceList = new ArrayList<>();

    private final CompositeDisposable presenterDisposable = new CompositeDisposable();

    public StorageLibraryPresenter(@Nullable String path,
                                   StorageLibraryInteractor interactor,
                                   ErrorParser errorParser,
                                   Scheduler uiScheduler) {
        this.path = path;
        this.interactor = interactor;
        this.errorParser = errorParser;
        this.uiScheduler = uiScheduler;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        if (path == null) {
            getViewState().hideBackPathButton();
        } else {
            getViewState().showBackPathButton(path);
        }
        getViewState().bindList(sourceList);

        loadMusic();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenterDisposable.dispose();
    }

    void onTryAgainButtonClicked() {
        loadMusic();
    }

    void onCompositionClicked(Composition composition) {
        interactor.playMusic(composition)
                .subscribe();//TODO handle error later
    }

    void onPlayAllButtonClicked() {
        interactor.playAllMusicInPath(path)
                .subscribe();//TODO handle error later
    }

    void onBackPathButtonClicked() {
        if (path == null) {
            throw new IllegalStateException("can not go back in root screen");
        }
        goBackToPreviousPath();
    }

    void onDeleteCompositionButtonClicked(Composition composition) {
        interactor.deleteComposition(composition)
                .observeOn(uiScheduler)
                .subscribe();//TODO displayError
    }

    private void goBackToPreviousPath() {
        String targetPath = null;
        int lastSlashIndex = path.lastIndexOf('/');
        int firstSlashIndex = path.indexOf("/");
        if (lastSlashIndex != -1 && firstSlashIndex != lastSlashIndex) {
            targetPath = path.substring(0, lastSlashIndex);
        }
        getViewState().goBackToMusicStorageScreen(targetPath);
    }

    private void loadMusic() {
        getViewState().showLoading();
        interactor.getCompositionsInPath(path)
                .observeOn(uiScheduler)
                .subscribe(this::onMusicLoaded, this::onMusicLoadingError);
    }

    private void onMusicLoaded(Folder folder) {
        subscribeOnFolderMusic(folder);
        subscribeOnSelfDeleting(folder);
    }

    private void onMusicLoadingError(Throwable throwable) {
        ErrorCommand errorCommand = errorParser.parseError(throwable);
        getViewState().showError(errorCommand);
    }

    private void subscribeOnSelfDeleting(Folder folder) {
        presenterDisposable.add(folder.getSelfDeleteObservable()
                .observeOn(uiScheduler)
                .subscribe(this::onFolderDeleted));
    }

    @SuppressWarnings("unused")
    private void onFolderDeleted(Object o) {
        if (path == null) {
            getViewState().showEmptyList();
        } else {
            goBackToPreviousPath();
        }
    }

    private void subscribeOnFolderMusic(Folder folder) {
        presenterDisposable.add(folder.getFilesObservable()
                .observeOn(uiScheduler)
                .subscribe(this::onMusicOnFoldersReceived));
    }

    private void onMusicOnFoldersReceived(List<FileSource> sources) {
        List<FileSource> oldList = new ArrayList<>(sourceList);

        sourceList.clear();
        sourceList.addAll(sources);

        getViewState().updateList(oldList, sourceList);

        if (sourceList.isEmpty()) {
            getViewState().showEmptyList();
        } else {
            getViewState().showList();
        }
    }
}