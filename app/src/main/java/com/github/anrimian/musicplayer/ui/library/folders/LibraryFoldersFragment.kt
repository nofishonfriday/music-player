package com.github.anrimian.musicplayer.ui.library.folders

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.anrimian.filesync.models.state.file.FileSyncState
import com.github.anrimian.musicplayer.Constants.Arguments.HIGHLIGHT_COMPOSITION_ID
import com.github.anrimian.musicplayer.Constants.Arguments.ID_ARG
import com.github.anrimian.musicplayer.Constants.Tags
import com.github.anrimian.musicplayer.R
import com.github.anrimian.musicplayer.databinding.FragmentLibraryFoldersBinding
import com.github.anrimian.musicplayer.di.Components
import com.github.anrimian.musicplayer.domain.models.composition.Composition
import com.github.anrimian.musicplayer.domain.models.composition.CurrentComposition
import com.github.anrimian.musicplayer.domain.models.composition.DeletedComposition
import com.github.anrimian.musicplayer.domain.models.folders.CompositionFileSource
import com.github.anrimian.musicplayer.domain.models.folders.FileSource
import com.github.anrimian.musicplayer.domain.models.folders.FolderFileSource
import com.github.anrimian.musicplayer.domain.models.folders.IgnoredFolder
import com.github.anrimian.musicplayer.domain.models.order.Order
import com.github.anrimian.musicplayer.domain.models.order.OrderType
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList
import com.github.anrimian.musicplayer.domain.models.utils.ListPosition
import com.github.anrimian.musicplayer.ui.common.dialogs.composition.showCompositionPopupMenu
import com.github.anrimian.musicplayer.ui.common.dialogs.input.InputTextDialogFragment
import com.github.anrimian.musicplayer.ui.common.dialogs.input.newInputTextDialogFragment
import com.github.anrimian.musicplayer.ui.common.dialogs.shareComposition
import com.github.anrimian.musicplayer.ui.common.dialogs.shareCompositions
import com.github.anrimian.musicplayer.ui.common.dialogs.showConfirmDeleteDialog
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand
import com.github.anrimian.musicplayer.ui.common.format.FormatUtils
import com.github.anrimian.musicplayer.ui.common.format.MessagesUtils
import com.github.anrimian.musicplayer.ui.common.menu.PopupMenuWindow
import com.github.anrimian.musicplayer.ui.common.toolbar.AdvancedToolbar
import com.github.anrimian.musicplayer.ui.common.view.ViewUtils
import com.github.anrimian.musicplayer.ui.editor.common.DeleteErrorHandler
import com.github.anrimian.musicplayer.ui.editor.common.ErrorHandler
import com.github.anrimian.musicplayer.ui.editor.composition.newCompositionEditorIntent
import com.github.anrimian.musicplayer.ui.equalizer.EqualizerDialogFragment
import com.github.anrimian.musicplayer.ui.library.common.order.SelectOrderDialogFragment
import com.github.anrimian.musicplayer.ui.library.folders.adapter.MusicFileSourceAdapter
import com.github.anrimian.musicplayer.ui.playlist_screens.choose.ChoosePlayListDialogFragment
import com.github.anrimian.musicplayer.ui.playlist_screens.choose.newChoosePlayListDialogFragment
import com.github.anrimian.musicplayer.ui.settings.folders.ExcludedFoldersFragment
import com.github.anrimian.musicplayer.ui.sleep_timer.SleepTimerDialogFragment
import com.github.anrimian.musicplayer.ui.utils.dialogs.ProgressDialogFragment
import com.github.anrimian.musicplayer.ui.utils.dialogs.newProgressDialogFragment
import com.github.anrimian.musicplayer.ui.utils.fragments.BackButtonListener
import com.github.anrimian.musicplayer.ui.utils.fragments.DialogFragmentDelayRunner
import com.github.anrimian.musicplayer.ui.utils.fragments.DialogFragmentRunner
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.FragmentNavigation
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.FragmentNavigationListener
import com.github.anrimian.musicplayer.ui.utils.fragments.safeShow
import com.github.anrimian.musicplayer.ui.utils.slidr.SlidrPanel
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.RecyclerViewUtils
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.touch_helper.short_swipe.ShortSwipeCallback
import com.google.android.material.snackbar.Snackbar
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import io.reactivex.rxjava3.disposables.CompositeDisposable
import moxy.MvpAppCompatFragment
import moxy.ktx.moxyPresenter

/**
 * Created on 23.10.2017.
 */

private const val LOCKED_SEARCH_MODE = "locked_search_mode"
private const val LAUNCHED_SEARCH_MODE = "launched_search_mode"

fun newFolderFragment(
    folderId: Long?,
    highlightCompositionId: Long = 0,
    lockedSearchMode: Boolean = false,
): LibraryFoldersFragment {
    val args = Bundle()
    args.putLong(ID_ARG, folderId ?: 0)
    args.putLong(HIGHLIGHT_COMPOSITION_ID, highlightCompositionId)
    args.putBoolean(LOCKED_SEARCH_MODE, lockedSearchMode)
    val fragment = LibraryFoldersFragment()
    fragment.arguments = args
    return fragment
}

class LibraryFoldersFragment : MvpAppCompatFragment(), LibraryFoldersView, BackButtonListener,
    FragmentNavigationListener {

    private val presenter by moxyPresenter {
        Components.getLibraryFolderComponent(getFolderId()).storageLibraryPresenter()
    }

    private lateinit var viewBinding: FragmentLibraryFoldersBinding

    private val fragmentDisposable = CompositeDisposable()

    private lateinit var toolbar: AdvancedToolbar
    private lateinit var adapter: MusicFileSourceAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var filenameDialogFragmentRunner: DialogFragmentRunner<InputTextDialogFragment>
    private lateinit var newFolderDialogFragmentRunner: DialogFragmentRunner<InputTextDialogFragment>
    private lateinit var choosePlaylistForFolderDialogRunner: DialogFragmentRunner<ChoosePlayListDialogFragment>
    private lateinit var selectOrderDialogRunner: DialogFragmentRunner<SelectOrderDialogFragment>
    private lateinit var choosePlayListDialogRunner: DialogFragmentRunner<ChoosePlayListDialogFragment>
    private lateinit var progressDialogRunner: DialogFragmentDelayRunner<ProgressDialogFragment>

    private lateinit var editorErrorHandler: ErrorHandler
    private lateinit var deletingErrorHandler: ErrorHandler

    private var subToolbarAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewBinding = FragmentLibraryFoldersBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = requireActivity().findViewById(R.id.toolbar)
        fragmentDisposable.add(toolbar.getSelectionModeObservable()
            .subscribe(this::onSelectionModeChanged)
        )
        viewBinding.progressStateView.onTryAgainClick { presenter.onTryAgainButtonClicked() }

        layoutManager = LinearLayoutManager(context)
        viewBinding.rvFileSources.layoutManager = layoutManager
        RecyclerViewUtils.attachFastScroller(viewBinding.rvFileSources, true)
        adapter = MusicFileSourceAdapter(
            this,
            viewBinding.rvFileSources,
            presenter.getSelectedFiles(),
            presenter.getSelectedMoveFiles(),
            presenter::onCompositionClicked,
            presenter::onFolderClicked,
            presenter::onItemLongClick,
            this::onFolderMenuClicked,
            presenter::onCompositionIconClicked,
            this::onCompositionMenuClicked
        )
        viewBinding.rvFileSources.adapter = adapter

        val callback = ShortSwipeCallback(requireContext(),
            R.drawable.ic_play_next,
            R.string.play_next,
            swipeCallback = presenter::onPlayNextSourceClicked
        )
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(viewBinding.rvFileSources)

        viewBinding.flHeader.setOnClickListener { presenter.onBackPathButtonClicked() }

        viewBinding.fab.setOnClickListener { presenter.onPlayAllButtonClicked() }
        ViewUtils.onLongVibrationClick(viewBinding.fab, presenter::onChangeRandomModePressed)

        viewBinding.vgFileMenu.visibility = View.INVISIBLE
        viewBinding.vgMoveFileMenu.visibility = View.INVISIBLE
        FormatUtils.formatLinkedFabView(viewBinding.vgFileMenu, viewBinding.fab)
        FormatUtils.formatLinkedFabView(viewBinding.vgMoveFileMenu, viewBinding.fab)

        viewBinding.btnCut.setOnClickListener { presenter.onMoveSelectedFoldersButtonClicked() }
        viewBinding.ivCopy.setOnClickListener { presenter.onCopySelectedFoldersButtonClicked() }

        //maybe will be moved to root fragment later
        view.findViewById<View>(R.id.iv_close)
            .setOnClickListener { presenter.onCloseMoveMenuClicked() }
        view.findViewById<View>(R.id.btnPaste)
            .setOnClickListener { presenter.onPasteButtonClicked() }
        view.findViewById<View>(R.id.btnPasteInNewFolder)
            .setOnClickListener { presenter.onPasteInNewFolderButtonClicked() }

        editorErrorHandler = ErrorHandler(
            this,
            presenter::onRetryFailedEditActionClicked,
            this::showEditorRequestDeniedMessage
        )
        deletingErrorHandler = DeleteErrorHandler(
            this,
            presenter::onRetryFailedDeleteActionClicked,
            this::showEditorRequestDeniedMessage
        )

        val fm = childFragmentManager
        selectOrderDialogRunner = DialogFragmentRunner(fm, Tags.ORDER_TAG) { fragment ->
            fragment.setOnCompleteListener(presenter::onOrderSelected)
        }
        choosePlayListDialogRunner = DialogFragmentRunner(fm, Tags.SELECT_PLAYLIST_TAG) { fragment ->
            fragment.setOnCompleteListener(presenter::onPlayListToAddingSelected)
        }
        choosePlaylistForFolderDialogRunner = DialogFragmentRunner(
            fm,
            Tags.SELECT_PLAYLIST_FOR_FOLDER_TAG
        ) { fragment ->
            fragment.setComplexCompleteListener { playlist, bundle ->
                val folderId = bundle.getLong(ID_ARG)
                presenter.onPlayListForFolderSelected(folderId, playlist)
            }
        }
        filenameDialogFragmentRunner = DialogFragmentRunner(
            fm,
            Tags.FILE_NAME_TAG
        ) { fragment ->
            fragment.setComplexCompleteListener { name, extra ->
                presenter.onNewFolderNameEntered(extra.getLong(ID_ARG), name)
            }
        }
        newFolderDialogFragmentRunner = DialogFragmentRunner(
            fm,
            Tags.NEW_FOLDER_NAME_TAG
        ) { fragment -> fragment.setOnCompleteListener(presenter::onNewFileNameForPasteEntered) }
        progressDialogRunner = DialogFragmentDelayRunner(fm, Tags.PROGRESS_DIALOG_TAG)

        val isSearchLaunched = requireArguments().getBoolean(LAUNCHED_SEARCH_MODE)
        showSubToolbar(isSearchLaunched, animate = false)
        viewBinding.btnPaste.isEnabled = !isSearchLaunched
        viewBinding.btnPasteInNewFolder.isEnabled = !isSearchLaunched
        if (isSearchLaunched) {
            presenter.onSearchTextChanged(toolbar.searchText)
        }

        if (getFolderId() != null) {
            val slidrConfig = SlidrConfig.Builder().position(SlidrPosition.LEFT).build()
            SlidrPanel.replace(
                viewBinding.contentContainer,
                {
                    if (requireArguments().getBoolean(LAUNCHED_SEARCH_MODE)) {
                        toolbar.setSearchModeEnabled(false)
                    }
                    toolbar.showSelectionMode(0)
                    FragmentNavigation.from(parentFragmentManager).goBack()
                },
                slidrConfig
            )
        }
    }

    override fun onStop() {
        super.onStop()
        presenter.onStop(ViewUtils.getListPosition(layoutManager))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentDisposable.clear()
    }

    override fun onFragmentResumed() {
        val inLockedSearchMode = requireArguments().getBoolean(LOCKED_SEARCH_MODE)
        presenter.onFragmentDisplayed(inLockedSearchMode)
        val act = requireActivity()
        val toolbar: AdvancedToolbar = act.findViewById(R.id.toolbar)
        if (inLockedSearchMode) {
            toolbar.isSearchLocked = true
        } else {
            toolbar.setupSearch(presenter::onSearchTextChanged)
            if (requireArguments().getBoolean(LAUNCHED_SEARCH_MODE)) {
                toolbar.isSearchLocked = false
            }
        }
        toolbar.setupSelectionModeMenu(R.menu.library_folders_selection_menu, this::onActionModeItemClicked)
        toolbar.setupOptionsMenu(R.menu.library_files_menu, this::onOptionsItemClicked)
    }

    override fun updateList(list: List<FileSource>) {
        adapter.submitList(list)
        val highlightCompositionId = requireArguments().getLong(HIGHLIGHT_COMPOSITION_ID)
        if (highlightCompositionId != 0L) {
            highlightComposition(highlightCompositionId)
        }
    }

    override fun showFolderInfo(folder: FolderFileSource?) {
        if (folder != null) {
            viewBinding.tvHeader.text = folder.name
        }
    }

    override fun showEmptyList() {
        viewBinding.fab.visibility = View.GONE
        val message = if (getFolderId() == null) {
            R.string.compositions_on_device_not_found
        } else {
            R.string.no_compositions_in_folder
        }
        viewBinding.progressStateView.showMessage(message, false)
    }

    override fun showEmptySearchResult() {
        viewBinding.fab.visibility = View.GONE
        viewBinding.progressStateView.showMessage(
            R.string.compositions_and_folders_for_search_not_found,
            false
        )
    }

    override fun showList() {
        viewBinding.fab.visibility = View.VISIBLE
        viewBinding.progressStateView.hideAll()
    }

    override fun showLoading() {
        viewBinding.progressStateView.showProgress()
    }

    override fun showError(errorCommand: ErrorCommand) {
        viewBinding.progressStateView.showMessage(errorCommand.message, true)
    }

    override fun goBackToParentFolderScreen() {
        FragmentNavigation.from(parentFragmentManager).goBack()
    }

    override fun onBackPressed(): Boolean {
        if (toolbar.isInActionMode) {
            presenter.onSelectionModeBackPressed()
            return true
        }
        if (toolbar.isInSearchMode && !requireArguments().getBoolean(LOCKED_SEARCH_MODE)) {
            setSearchModeActive(false)
            return true
        }
        if (getFolderId() != null) {
            presenter.onBackPathButtonClicked()
            return true
        }
        return false
    }

    override fun showAddingToPlayListError(errorCommand: ErrorCommand) {
        MessagesUtils.makeSnackbar(
            viewBinding.listContainer,
            getString(R.string.add_to_playlist_error_template, errorCommand.message),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun showAddingToPlayListComplete(playList: PlayList, compositions: List<Composition>) {
        val text = MessagesUtils.getAddToPlayListCompleteMessage(requireActivity(), playList, compositions)
        MessagesUtils.makeSnackbar(viewBinding.listContainer, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun showSelectPlayListForFolderDialog(folder: FolderFileSource) {
        val bundle = Bundle()
        bundle.putLong(ID_ARG, folder.id)
        val dialog = newChoosePlayListDialogFragment(bundle)
        choosePlaylistForFolderDialogRunner.show(dialog)
    }

    override fun showSelectOrderScreen(folderOrder: Order) {
        val fragment = SelectOrderDialogFragment.newInstance(
            folderOrder,
            true,
            OrderType.NAME,
            OrderType.FILE_NAME,
            OrderType.ADD_TIME,
            OrderType.DURATION,
            OrderType.SIZE
        )
        selectOrderDialogRunner.show(fragment)
    }

    override fun showSelectPlayListDialog() {
        choosePlayListDialogRunner.show(ChoosePlayListDialogFragment())
    }

    override fun showConfirmDeleteDialog(compositionsToDelete: List<Composition>) {
        showConfirmDeleteDialog(
            requireContext(),
            compositionsToDelete,
            presenter::onDeleteCompositionsDialogConfirmed
        )
    }

    override fun showConfirmDeleteDialog(folder: FolderFileSource) {
        showConfirmDeleteDialog(requireContext(), folder) {
            presenter.onDeleteFolderDialogConfirmed(folder)
        }
    }

    override fun showDeleteCompositionError(errorCommand: ErrorCommand) {
        deletingErrorHandler.handleError(errorCommand) {
            MessagesUtils.makeSnackbar(
                viewBinding.listContainer,
                getString(R.string.delete_composition_error_template, errorCommand.message),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun showDeleteCompositionMessage(compositionsToDelete: List<DeletedComposition>) {
        val text = MessagesUtils.getDeleteCompleteMessage(requireActivity(), compositionsToDelete)
        MessagesUtils.makeSnackbar(viewBinding.listContainer, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun sendCompositions(compositions: List<Composition>) {
        shareCompositions(this, compositions)
    }

    override fun showReceiveCompositionsForSendError(errorCommand: ErrorCommand) {
        MessagesUtils.makeSnackbar(
            viewBinding.listContainer,
            getString(R.string.can_not_receive_file_for_send, errorCommand.message),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun goToMusicStorageScreen(folderId: Long) {
        var lockedSearchMode = toolbar.isInSearchMode
        if (lockedSearchMode && toolbar.searchText.isNullOrEmpty()) {
            setSearchModeActive(false)
            lockedSearchMode = false
        }
        FragmentNavigation.from(parentFragmentManager)
            .addNewFragment(newFolderFragment(folderId, lockedSearchMode = lockedSearchMode))
    }

    override fun showErrorMessage(errorCommand: ErrorCommand) {
        editorErrorHandler.handleError(errorCommand) {
            MessagesUtils.makeSnackbar(
                viewBinding.listContainer, errorCommand.message, Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun setDisplayCoversEnabled(isCoversEnabled: Boolean) {
        adapter.setCoversEnabled(isCoversEnabled)
    }

    override fun showRandomMode(isRandomModeEnabled: Boolean) {
        FormatUtils.formatPlayAllButton(viewBinding.fab, isRandomModeEnabled)
    }

    override fun showInputFolderNameDialog(folder: FolderFileSource) {
        val extra = Bundle()
        extra.putLong(ID_ARG, folder.id)
        val fragment = newInputTextDialogFragment(
            R.string.rename_folder,
            R.string.change,
            R.string.cancel,
            R.string.folder_name,
            folder.name,
            canBeEmpty = false,
            extra = extra
        )
        filenameDialogFragmentRunner.show(fragment)
    }

    override fun showInputNewFolderNameDialog() {
        val fragment = newInputTextDialogFragment(
            R.string.new_folder,
            R.string.create,
            R.string.cancel,
            R.string.folder_name,
            null,
            canBeEmpty = false
        )
        newFolderDialogFragmentRunner.show(fragment)
    }

    override fun showSelectionMode(count: Int) {
        toolbar.showSelectionMode(count)
    }

    override fun onItemSelected(item: FileSource, position: Int) {
        adapter.setItemSelected(position)
    }

    override fun onItemUnselected(item: FileSource, position: Int) {
        adapter.setItemUnselected(position)
    }

    override fun setItemsSelected(selected: Boolean) {
        adapter.setItemsSelected(selected)
    }

    override fun updateMoveFilesList() {
        adapter.updateItemsToMove()
    }

    override fun showMoveFileMenu(show: Boolean) {
        com.github.anrimian.musicplayer.ui.utils.ViewUtils.animateVisibility(
            viewBinding.vgMoveFileMenu,
            if (show) View.VISIBLE else View.INVISIBLE
        )
    }

    override fun showCurrentComposition(currentComposition: CurrentComposition) {
        adapter.showCurrentComposition(currentComposition)
    }

    override fun restoreListPosition(listPosition: ListPosition) {
        ViewUtils.scrollToPosition(layoutManager, listPosition)
    }

    override fun showAddedIgnoredFolderMessage(folder: IgnoredFolder) {
        val message = getString(R.string.ignored_folder_added, folder.relativePath)
        MessagesUtils.makeSnackbar(viewBinding.listContainer, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.cancel, presenter::onRemoveIgnoredFolderClicked)
            .show()
    }

    override fun onCompositionsAddedToPlayNext(compositions: List<Composition>) {
        val message = MessagesUtils.getPlayNextMessage(requireContext(), compositions)
        MessagesUtils.makeSnackbar(viewBinding.listContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCompositionsAddedToQueue(compositions: List<Composition>) {
        val message = MessagesUtils.getAddedToQueueMessage(requireContext(), compositions)
        MessagesUtils.makeSnackbar(viewBinding.listContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun hideProgressDialog() {
        progressDialogRunner.cancel()
    }

    override fun showMoveProgress() {
        showProgressDialog(R.string.move_progress)
    }

    override fun showDeleteProgress() {
        showProgressDialog(R.string.delete_progress)
    }

    override fun showRenameProgress() {
        showProgressDialog(R.string.rename_progress)
    }

    override fun showFilesSyncState(states: Map<Long, FileSyncState>) {
        adapter.showFileSyncStates(states)
    }

    fun getFolderId(): Long? {
        val id = requireArguments().getLong(ID_ARG)
        return if (id == 0L) null else id
    }

    fun requestHighlightComposition(compositionId: Long) {
        requireArguments().putLong(HIGHLIGHT_COMPOSITION_ID, compositionId)
        highlightComposition(compositionId)
    }

    private fun highlightComposition(targetCompositionId: Long) {
        viewBinding.rvFileSources.post {
            val position = adapter.currentList.indexOfFirst { source ->
                source is CompositionFileSource && source.composition.id == targetCompositionId
            }
            if (position != -1) {
                viewBinding.rvFileSources.scrollToPosition(position)
                viewBinding.rvFileSources.post { adapter.highlightItem(position) }
                requireArguments().remove(HIGHLIGHT_COMPOSITION_ID)
            }
        }
    }

    private fun showProgressDialog(@StringRes resId: Int) {
        progressDialogRunner.show(newProgressDialogFragment(resId))
    }

    private fun onCompositionMenuClicked(view: View, position: Int, source: CompositionFileSource) {
        val composition = source.composition
        showCompositionPopupMenu(view, R.menu.composition_folder_actions_menu, composition) { item ->
            onCompositionActionSelected(position, composition, item.itemId)
        }
    }

    private fun onSelectionModeChanged(enabled: Boolean) {
        val show = enabled && !requireArguments().getBoolean(LAUNCHED_SEARCH_MODE)
        com.github.anrimian.musicplayer.ui.utils.ViewUtils.animateVisibility(
            viewBinding.vgFileMenu,
            if (show) View.VISIBLE else View.INVISIBLE
        )
    }

    private fun onCompositionActionSelected(
        position: Int,
        composition: Composition,
        @MenuRes menuItemId: Int
    ) {
        when (menuItemId) {
            R.id.menu_play -> presenter.onPlayCompositionActionSelected(position)
            R.id.menu_play_next -> presenter.onPlayNextCompositionClicked(composition)
            R.id.menu_add_to_queue -> presenter.onAddToQueueCompositionClicked(composition)
            R.id.menu_add_to_playlist -> presenter.onAddToPlayListButtonClicked(composition)
            R.id.menu_edit -> startActivity(newCompositionEditorIntent(requireContext(), composition.id))
            R.id.menu_share -> shareComposition(this, composition)
            R.id.menu_delete -> presenter.onDeleteCompositionButtonClicked(composition)
        }
    }

    private fun onActionModeItemClicked(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menu_play -> presenter.onPlayAllSelectedClicked()
            R.id.menu_select_all -> presenter.onSelectAllButtonClicked()
            R.id.menu_play_next -> presenter.onPlayNextSelectedSourcesClicked()
            R.id.menu_add_to_queue -> presenter.onAddToQueueSelectedSourcesClicked()
            R.id.menu_add_to_playlist -> presenter.onAddSelectedSourcesToPlayListClicked()
            R.id.menu_share -> presenter.onShareSelectedSourcesClicked()
            R.id.menu_delete -> presenter.onDeleteSelectedCompositionButtonClicked()
        }
    }

    private fun onFolderMenuClicked(view: View, folder: FolderFileSource) {
        PopupMenuWindow.showPopup(view, R.menu.folder_item_menu) { item ->
            when (item.itemId) {
                R.id.menu_play -> presenter.onPlayFolderClicked(folder)
                R.id.menu_play_next -> presenter.onPlayNextFolderClicked(folder)
                R.id.menu_add_to_queue -> presenter.onAddToQueueFolderClicked(folder)
                R.id.menu_add_to_playlist -> presenter.onAddFolderToPlayListButtonClicked(folder)
                R.id.menu_rename_folder -> presenter.onRenameFolderClicked(folder)
                R.id.menu_share -> presenter.onShareFolderClicked(folder)
                R.id.menu_hide -> presenter.onExcludeFolderClicked(folder)
                R.id.menu_delete -> presenter.onDeleteFolderButtonClicked(folder)
            }
        }
    }

    private fun onOptionsItemClicked(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_order -> presenter.onOrderMenuItemClicked()
            R.id.menu_excluded_folders -> {
                val parentFragment = parentFragment
                if (parentFragment != null) {
                    FragmentNavigation.from(parentFragment.parentFragmentManager)
                        .addNewFragment(ExcludedFoldersFragment())
                }
            }
            R.id.menu_sleep_timer -> SleepTimerDialogFragment().safeShow(childFragmentManager)
            R.id.menu_equalizer -> EqualizerDialogFragment().safeShow(childFragmentManager)
            R.id.menu_search -> setSearchModeActive(true)
        }
    }

    private fun setSearchModeActive(isActive: Boolean) {
        toolbar.setSearchModeEnabled(isActive)
        requireArguments().putBoolean(LAUNCHED_SEARCH_MODE, isActive)
        viewBinding.btnPaste.isEnabled = !isActive
        viewBinding.btnPasteInNewFolder.isEnabled = !isActive
        if (getFolderId() != null) {
            showSubToolbar(isActive, true)
        }
    }

    private fun showSubToolbar(isSearchActive: Boolean, animate: Boolean) {
        val show = getFolderId() != null && !isSearchActive

        val visibility = if (show) View.VISIBLE else View.GONE

        val expandedY = resources.getDimensionPixelSize(R.dimen.sub_toolbar_height)
        val collapsedY = 0
        val currentTranslationY = getSubtitleY()
        val targetTranslationY = if (show) expandedY else collapsedY
        subToolbarAnimator?.cancel()
        if (animate) {
            subToolbarAnimator = ValueAnimator.ofInt(currentTranslationY, targetTranslationY)
                .also { animator ->
                    animator.addUpdateListener { animation ->
                        setSubtitleY(animation.animatedValue as Int)
                    }
                    animator.addListener(
                        onStart = {
                            if (show) {
                                viewBinding.flHeader.visibility = View.VISIBLE
                            }
                        },
                        onEnd = {
                            if (!show) {
                                viewBinding.flHeader.visibility = visibility
                            }
                        },
                    )
                    animator.duration = 300
                    animator.interpolator = if (show) DecelerateInterpolator() else AccelerateInterpolator()

                    subToolbarAnimator?.cancel()
                    subToolbarAnimator = animator
                    animator.start()
                }
        } else {
            viewBinding.flHeader.visibility = visibility
            setSubtitleY(targetTranslationY)
        }
    }

    private fun getSubtitleY() =
        (viewBinding.guidelineSubtitle.layoutParams as ConstraintLayout.LayoutParams).guideBegin

    private fun setSubtitleY(translationY: Int) {
        viewBinding.guidelineSubtitle.updateLayoutParams<ConstraintLayout.LayoutParams> {
            guideBegin = translationY
        }
    }

    private fun showEditorRequestDeniedMessage() {
        MessagesUtils.makeSnackbar(
            viewBinding.listContainer,
            R.string.android_r_edit_file_permission_denied,
            Snackbar.LENGTH_LONG
        ).show()
    }
}