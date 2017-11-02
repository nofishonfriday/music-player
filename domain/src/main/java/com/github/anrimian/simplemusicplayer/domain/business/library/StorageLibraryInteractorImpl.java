package com.github.anrimian.simplemusicplayer.domain.business.library;

import com.github.anrimian.simplemusicplayer.domain.business.player.MusicPlayerInteractor;
import com.github.anrimian.simplemusicplayer.domain.models.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.exceptions.FileNodeNotFoundException;
import com.github.anrimian.simplemusicplayer.domain.models.files.FileSource;
import com.github.anrimian.simplemusicplayer.domain.models.files.FolderFileSource;
import com.github.anrimian.simplemusicplayer.domain.models.files.MusicFileSource;
import com.github.anrimian.simplemusicplayer.domain.repositories.MusicProviderRepository;
import com.github.anrimian.simplemusicplayer.domain.utils.tree.FileTree;
import com.github.anrimian.simplemusicplayer.domain.utils.tree.visitors.CollectVisitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Single;

/**
 * Created on 24.10.2017.
 */

public class StorageLibraryInteractorImpl implements StorageLibraryInteractor {

    @Nullable
    private FileTree<Composition> musicFileTree;

    private MusicProviderRepository musicProviderRepository;
    private MusicPlayerInteractor musicPlayerInteractor;

    public StorageLibraryInteractorImpl(MusicProviderRepository musicProviderRepository,
                                        MusicPlayerInteractor musicPlayerInteractor) {
        this.musicProviderRepository = musicProviderRepository;
        this.musicPlayerInteractor = musicPlayerInteractor;
    }

    @Override
    public void playAllMusicInPath(@Nullable String path) {
            getMusicFileTree().map(tree -> findNodeByPath(tree, path))
                    .map(this::getAllCompositions)
                    .subscribe(musicPlayerInteractor::startPlaying);
    }

    @Override
    public Single<List<FileSource>> getMusicInPath(@Nullable String path) {
        return getMusicFileTree()
                .map(tree -> getFilesListByPath(tree, path))
                .map(this::applyOrder);
    }

    @Override
    public void playMusic(Composition composition) {
        musicPlayerInteractor.startPlaying(composition);
    }

    private List<FileSource> applyOrder(List<FileSource> FileSources) {
        List<FileSource> sortedList = new ArrayList<>();
        List<FileSource> musicList = new ArrayList<>();
        for (FileSource fileSource: FileSources) {
            if (fileSource instanceof FolderFileSource) {
                sortedList.add(fileSource);
            } else {
                musicList.add(fileSource);
            }
        }
        sortedList.addAll(musicList);
        return sortedList;
    }

    private List<Composition> getAllCompositions(FileTree<Composition> compositionFileTree) {
        List<Composition> compositions = new LinkedList<>();
        compositionFileTree.accept(new CollectVisitor<>(compositions));
        return compositions;
    }

    private List<FileSource> getFilesListByPath(FileTree<Composition> tree, @Nullable String path) {
        FileTree<Composition> compositionFileTree = findNodeByPath(tree, path);
        List<FileSource> musicList = new ArrayList<>();
        for (FileTree<Composition> node : compositionFileTree.getChildren()) {
            FileSource fileSource;
            Composition data = node.getData();
            if (data == null) {
                fileSource = new FolderFileSource(tree.getFullPathOfNode(node));
            } else {
                fileSource = new MusicFileSource(data);
            }
            musicList.add(fileSource);
        }
        return musicList;
    }

    private FileTree<Composition> findNodeByPath(FileTree<Composition> tree, @Nullable String path) {
        FileTree<Composition> result = tree.findNodeByPath(path);
        if (result == null) {
            throw new FileNodeNotFoundException("node not found for path: " + path);
        }
        return result;
    }

    private Single<FileTree<Composition>> getMusicFileTree() {
        if (musicFileTree == null) {
            return createMusicFileTree().doOnSuccess(musicFileTree -> this.musicFileTree = musicFileTree);
        } else {
            return Single.just(musicFileTree);
        }
    }

    private Single<FileTree<Composition>> createMusicFileTree() {
        return musicProviderRepository.getAllCompositions()
                .map(compositions -> {
                    FileTree<Composition> musicFileTree = new FileTree<>(null);
                    for (Composition composition: compositions) {
                        String filePath = composition.getFilePath();
                        musicFileTree.addFile(composition, filePath);
                    }
                    return musicFileTree;
                })
                .map(this::removeUnusedRootComponents);
    }

    private FileTree<Composition> removeUnusedRootComponents(FileTree<Composition> tree) {
        FileTree<Composition> root = tree;
        while (root.getData() == null && root.getChildCount() <= 1) {
            root = root.getFirstChild();
        }
        return root;
    }
}
