package com.github.anrimian.filesync.stubs

import com.github.anrimian.filesync.SyncInteractor
import com.github.anrimian.filesync.models.RemoteFileSource
import com.github.anrimian.filesync.models.SyncEnvCondition
import com.github.anrimian.filesync.models.repo.RemoteRepoFullInfo
import com.github.anrimian.filesync.models.repo.RemoteRepoInfo
import com.github.anrimian.filesync.models.repo.RemoteRepoType
import com.github.anrimian.filesync.models.repo.RepoSetupTemplate
import com.github.anrimian.filesync.models.state.SyncState
import com.github.anrimian.filesync.models.state.file.FileSyncState
import com.github.anrimian.filesync.models.task.FileTaskInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class StubSyncInteractor<K, T, I> : SyncInteractor<K, T, I> {
    override fun onAppStarted() {}
    override fun requestFileSync() {}
    override fun runFileTasks() {}

    override fun addRemoteRepository(template: RepoSetupTemplate): Completable {
        return Completable.never()
    }

    override fun removeRemoteRepository(repo: RemoteRepoInfo): Completable {
        return Completable.never()
    }

    override fun setRemoteRepositoryEnabled(repo: RemoteRepoInfo): Completable {
        return Completable.never()
    }
    override fun onLocalFileAdded() {}
    override fun onLocalFileDeleted(key: K) {}
    override fun onLocalFilesDeleted(keys: List<K>) {}
    override fun notifyLocalFileChanged() {}
    override fun isSyncEnabled() = false
    override fun setSyncEnabled(enabled: Boolean) {}
    override fun onScheduledSyncCalled(): Completable = Completable.never()
    override fun getSyncConditions(): List<SyncEnvCondition> {
        return emptyList()
    }

    override fun setSyncConditionEnabled(condition: SyncEnvCondition, enabled: Boolean) {}

    override fun getAvailableRemoteRepositories(): List<RemoteRepoType> {
        return emptyList()
    }

    override fun getSyncStateObservable(): Observable<SyncState> {
        return Observable.never()
    }

    override fun getRepositoriesObservable(): Observable<List<RemoteRepoFullInfo>> {
        return Observable.never()
    }

    override fun getTasksListObservable(): Observable<List<FileTaskInfo<K>>> {
        return Observable.never()
    }

    override fun getTasksCountObservable(): Observable<Int> {
        return Observable.never()
    }

    override fun getFileSyncStateObservable(fileId: I): Observable<FileSyncState> {
        return Observable.never()
    }

    override fun getFilesSyncStateObservable(): Observable<Map<I, FileSyncState>> {
        return Observable.never()
    }

    override fun getHasScheduledTasksObservable(): Observable<Boolean> {
        return Observable.never()
    }

    override fun requestFileSource(fileId: I): Single<RemoteFileSource> {
        return Single.never()
    }
}