package com.github.anrimian.musicplayer.ui.library.common.compositions

import com.github.anrimian.filesync.models.state.file.FileSyncState
import com.github.anrimian.musicplayer.domain.models.composition.Composition
import com.github.anrimian.musicplayer.domain.models.composition.CurrentComposition
import com.github.anrimian.musicplayer.domain.models.composition.DeletedComposition
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList
import com.github.anrimian.musicplayer.domain.models.utils.ListPosition
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand
import com.github.anrimian.musicplayer.ui.common.mvp.ListMvpView
import moxy.viewstate.strategy.alias.AddToEndSingle
import moxy.viewstate.strategy.alias.OneExecution
import moxy.viewstate.strategy.alias.Skip

interface BaseLibraryCompositionsView<C : Composition> : ListMvpView<C> {

    @OneExecution
    fun showSelectPlayListDialog()

    @OneExecution
    fun showAddingToPlayListError(errorCommand: ErrorCommand)

    @OneExecution
    fun showAddingToPlayListComplete(playList: PlayList, compositions: List<Composition>)

    @OneExecution
    fun showConfirmDeleteDialog(compositionsToDelete: List<Composition>)

    @OneExecution
    fun showDeleteCompositionError(errorCommand: ErrorCommand)

    @OneExecution
    fun showDeleteCompositionMessage(compositionsToDelete: List<DeletedComposition>)

    @Skip
    fun onCompositionSelected(composition: C, position: Int)

    @Skip
    fun onCompositionUnselected(composition: C, position: Int)

    @Skip
    fun setItemsSelected(selected: Boolean)

    @AddToEndSingle
    fun showSelectionMode(count: Int)

    @Skip
    fun shareCompositions(selectedCompositions: Collection<Composition>)

    @OneExecution
    fun showErrorMessage(errorCommand: ErrorCommand)

    @AddToEndSingle
    fun setDisplayCoversEnabled(isCoversEnabled: Boolean)

    @AddToEndSingle
    fun showRandomMode(isRandomModeEnabled: Boolean)

    @OneExecution
    fun onCompositionsAddedToPlayNext(compositions: List<Composition>)

    @OneExecution
    fun onCompositionsAddedToQueue(compositions: List<Composition>)

    @AddToEndSingle
    fun showCurrentComposition(currentComposition: CurrentComposition)

    @OneExecution
    fun restoreListPosition(listPosition: ListPosition)

    @AddToEndSingle
    fun showFilesSyncState(states: Map<Long, FileSyncState>)

}