package com.github.anrimian.musicplayer.ui.library.albums.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.anrimian.filesync.models.state.file.FileSyncState
import com.github.anrimian.musicplayer.Constants
import com.github.anrimian.musicplayer.Constants.Tags
import com.github.anrimian.musicplayer.R
import com.github.anrimian.musicplayer.databinding.FragmentBaseFabListBinding
import com.github.anrimian.musicplayer.di.Components
import com.github.anrimian.musicplayer.domain.models.albums.Album
import com.github.anrimian.musicplayer.domain.models.albums.AlbumComposition
import com.github.anrimian.musicplayer.domain.models.composition.Composition
import com.github.anrimian.musicplayer.domain.models.composition.CurrentComposition
import com.github.anrimian.musicplayer.domain.models.composition.DeletedComposition
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList
import com.github.anrimian.musicplayer.domain.models.utils.ListPosition
import com.github.anrimian.musicplayer.ui.common.dialogs.shareCompositions
import com.github.anrimian.musicplayer.ui.common.dialogs.showConfirmDeleteDialog
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand
import com.github.anrimian.musicplayer.ui.common.format.FormatUtils
import com.github.anrimian.musicplayer.ui.common.format.MessagesUtils
import com.github.anrimian.musicplayer.ui.common.toolbar.AdvancedToolbar
import com.github.anrimian.musicplayer.ui.common.view.ViewUtils
import com.github.anrimian.musicplayer.ui.editor.album.newAlbumEditorIntent
import com.github.anrimian.musicplayer.ui.editor.common.DeleteErrorHandler
import com.github.anrimian.musicplayer.ui.editor.common.ErrorHandler
import com.github.anrimian.musicplayer.ui.library.albums.items.adaptar.AlbumCompositionsAdapter
import com.github.anrimian.musicplayer.ui.library.common.compositions.BaseLibraryCompositionsFragment
import com.github.anrimian.musicplayer.ui.playlist_screens.choose.ChoosePlayListDialogFragment
import com.github.anrimian.musicplayer.ui.playlist_screens.choose.newChoosePlayListDialogFragment
import com.github.anrimian.musicplayer.ui.utils.fragments.BackButtonListener
import com.github.anrimian.musicplayer.ui.utils.fragments.DialogFragmentRunner
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.FragmentNavigation
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.FragmentNavigationListener
import com.github.anrimian.musicplayer.ui.utils.slidr.SlidrPanel
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.RecyclerViewUtils
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.touch_helper.short_swipe.ShortSwipeCallback
import com.google.android.material.snackbar.Snackbar
import moxy.ktx.moxyPresenter

fun newAlbumItemsFragment(albumId: Long): AlbumItemsFragment {
    val args = Bundle()
    args.putLong(Constants.Arguments.ID_ARG, albumId)
    val fragment = AlbumItemsFragment()
    fragment.arguments = args
    return fragment
}

class AlbumItemsFragment : BaseLibraryCompositionsFragment(), AlbumItemsView,
    FragmentNavigationListener,
    BackButtonListener {

    private val presenter by moxyPresenter {
        Components.albumItemsComponent(getAlbumId()).albumItemsPresenter()
    }
    
    private lateinit var binding: FragmentBaseFabListBinding
    
    private lateinit var toolbar: AdvancedToolbar
    private lateinit var adapter: AlbumCompositionsAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var choosePlayListDialogRunner: DialogFragmentRunner<ChoosePlayListDialogFragment>
    
    private lateinit var deletingErrorHandler: ErrorHandler

    override fun getLibraryPresenter(): AlbumItemsPresenter = presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBaseFabListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = requireActivity().findViewById(R.id.toolbar)

        binding.progressStateView.onTryAgainClick { presenter.onTryAgainLoadCompositionsClicked() }

        RecyclerViewUtils.attachFastScroller(binding.recyclerView, true)
        adapter = AlbumCompositionsAdapter(
            this,
            binding.recyclerView,
            presenter.getSelectedCompositions(),
            presenter::onCompositionClicked,
            presenter::onCompositionLongClick,
            presenter::onCompositionIconClicked,
            this::onCompositionMenuClicked
        )
        binding.recyclerView.adapter = adapter
        binding.fab.setOnClickListener { presenter.onPlayAllButtonClicked() }
        ViewUtils.onLongVibrationClick(binding.fab, presenter::onChangeRandomModePressed)

        layoutManager = LinearLayoutManager(context)
        binding.recyclerView.layoutManager = layoutManager
        val callback = ShortSwipeCallback(requireContext(),
            R.drawable.ic_play_next,
            R.string.play_next,
            swipeCallback = presenter::onPlayNextCompositionClicked
        )
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        SlidrPanel.simpleSwipeBack(
            binding.listContainer,
            this,
            toolbar::onStackFragmentSlided
        )

        val fm = childFragmentManager
        deletingErrorHandler = DeleteErrorHandler(
            this,
            presenter::onRetryFailedDeleteActionClicked,
            this::showEditorRequestDeniedMessage
        )
        choosePlayListDialogRunner = DialogFragmentRunner(fm, Tags.SELECT_PLAYLIST_TAG) { f ->
            f.setOnCompleteListener(presenter::onPlayListToAddingSelected)
        }
    }

    override fun onFragmentResumed() {
//        super.onFragmentMovedOnTop();
        presenter.onFragmentMovedToTop()
        val toolbar: AdvancedToolbar = requireActivity().findViewById(R.id.toolbar)
        toolbar.setupSearch(null, null)
        toolbar.setTitleClickListener(null)
        toolbar.setupSelectionModeMenu(R.menu.library_compositions_selection_menu, this::onActionModeItemClicked)
        toolbar.setupOptionsMenu(R.menu.library_album_items_menu, this::onOptionsItemClicked)
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop(ViewUtils.getListPosition(layoutManager))
    }

    override fun onBackPressed(): Boolean {
        if (toolbar.isInActionMode) {
            presenter.onSelectionModeBackPressed()
            return true
        }
        if (toolbar.isInSearchMode) {
            toolbar.setSearchModeEnabled(false)
            return true
        }
        return false
    }

    override fun showAlbumInfo(album: Album) {
        toolbar.title = album.name
        toolbar.subtitle = FormatUtils.formatAlbumAdditionalInfo(
            context,
            album,
            R.drawable.ic_description_text_circle_inverse
        )
    }

    override fun showEmptyList() {
        binding.fab.visibility = View.GONE
        binding.progressStateView.showMessage(R.string.no_compositions)
    }

    override fun showEmptySearchResult() {
        binding.fab.visibility = View.GONE
        binding.progressStateView.showMessage(R.string.compositions_for_search_not_found)
    }

    override fun showList() {
        binding.fab.visibility = View.VISIBLE
        binding.progressStateView.hideAll()
    }

    override fun showLoading() {
        binding.progressStateView.showProgress()
    }

    override fun showLoadingError(errorCommand: ErrorCommand) {
        binding.progressStateView.showMessage(errorCommand.message, true)
    }

    override fun updateList(compositions: List<AlbumComposition>) {
        adapter.submitList(compositions)
    }

    override fun restoreListPosition(listPosition: ListPosition) {
        ViewUtils.scrollToPosition(layoutManager, listPosition)
    }

    override fun onCompositionSelected(composition: AlbumComposition, position: Int) {
        adapter.setItemSelected(position)
    }

    override fun onCompositionUnselected(composition: AlbumComposition, position: Int) {
        adapter.setItemUnselected(position)
    }

    override fun setItemsSelected(selected: Boolean) {
        adapter.setItemsSelected(selected)
    }

    override fun showSelectionMode(count: Int) {
        toolbar.showSelectionMode(count)
    }

    override fun showAddingToPlayListError(errorCommand: ErrorCommand) {
        MessagesUtils.makeSnackbar(
            binding.listContainer,
            getString(R.string.add_to_playlist_error_template, errorCommand.message),
            Snackbar.LENGTH_SHORT
        )
            .show()
    }

    override fun showAddingToPlayListComplete(playList: PlayList, compositions: List<Composition>) {
        val text = MessagesUtils.getAddToPlayListCompleteMessage(
            requireActivity(),
            playList,
            compositions
        )
        MessagesUtils.makeSnackbar(binding.listContainer, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun showSelectPlayListDialog() {
        val dialog = if (toolbar.isInActionMode) {
            newChoosePlayListDialogFragment(R.attr.actionModeStatusBarColor)
        } else {
            ChoosePlayListDialogFragment()
        }
        choosePlayListDialogRunner.show(dialog)
    }

    override fun showConfirmDeleteDialog(compositionsToDelete: List<Composition>) {
        showConfirmDeleteDialog(
            requireContext(),
            compositionsToDelete,
            presenter::onDeleteCompositionsDialogConfirmed
        )
    }

    override fun showDeleteCompositionError(errorCommand: ErrorCommand) {
        deletingErrorHandler.handleError(errorCommand) {
            MessagesUtils.makeSnackbar(
                binding.listContainer,
                getString(R.string.delete_composition_error_template, errorCommand.message),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun showDeleteCompositionMessage(compositionsToDelete: List<DeletedComposition>) {
        val text = MessagesUtils.getDeleteCompleteMessage(requireActivity(), compositionsToDelete)
        MessagesUtils.makeSnackbar(binding.listContainer, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun shareCompositions(selectedCompositions: Collection<Composition>) {
        shareCompositions(this, selectedCompositions)
    }

    override fun showCurrentComposition(currentComposition: CurrentComposition) {
        adapter.showCurrentComposition(currentComposition)
    }

    override fun setDisplayCoversEnabled(isCoversEnabled: Boolean) {
        adapter.setCoversEnabled(isCoversEnabled)
    }

    override fun showRandomMode(isRandomModeEnabled: Boolean) {
        FormatUtils.formatPlayAllButton(binding.fab, isRandomModeEnabled)
    }

    override fun showErrorMessage(errorCommand: ErrorCommand) {
        MessagesUtils.makeSnackbar(binding.listContainer, errorCommand.message, Snackbar.LENGTH_SHORT)
            .show()
    }

    override fun onCompositionsAddedToPlayNext(compositions: List<Composition>) {
        val message = MessagesUtils.getPlayNextMessage(requireContext(), compositions)
        MessagesUtils.makeSnackbar(binding.listContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCompositionsAddedToQueue(compositions: List<Composition>) {
        val message = MessagesUtils.getAddedToQueueMessage(requireContext(), compositions)
        MessagesUtils.makeSnackbar(binding.listContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun showEditAlbumScreen(album: Album) {
        startActivity(newAlbumEditorIntent(requireContext(), album.id))
    }

    override fun closeScreen() {
        FragmentNavigation.from(parentFragmentManager).goBack()
    }

    override fun showFilesSyncState(states: Map<Long, FileSyncState>) {
        adapter.showFileSyncStates(states)
    }

    private fun getAlbumId() = requireArguments().getLong(Constants.Arguments.ID_ARG)

    private fun onOptionsItemClicked(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_edit -> presenter.onEditAlbumClicked()
        }
    }

    private fun showEditorRequestDeniedMessage() {
        MessagesUtils.makeSnackbar(
            binding.listContainer,
            R.string.android_r_edit_file_permission_denied,
            Snackbar.LENGTH_LONG
        ).show()
    }

}