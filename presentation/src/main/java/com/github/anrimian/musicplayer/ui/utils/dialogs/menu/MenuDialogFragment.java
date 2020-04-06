package com.github.anrimian.musicplayer.ui.utils.dialogs.menu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.domain.utils.java.BiCallback;
import com.github.anrimian.musicplayer.ui.utils.OnCompleteListener;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.github.anrimian.musicplayer.Constants.Arguments.EXTRA_DATA_ARG;
import static com.github.anrimian.musicplayer.ui.utils.AndroidUtils.createMenu;

public class MenuDialogFragment extends DialogFragment {

    private static final String MENU_ARG = "menu_arg";
    private static final String TITLE_ARG = "title_arg";

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @Nullable
    private OnCompleteListener<MenuItem> onCompleteListener;

    @Nullable
    private BiCallback<MenuItem, Bundle> complexCompleteListener;

    public static MenuDialogFragment newInstance(@MenuRes int menuRes, String title) {
        return newInstance(menuRes, title, null);
    }

    public static MenuDialogFragment newInstance(@MenuRes int menuRes,
                                                 String title,
                                                 Bundle extra) {
        Bundle args = new Bundle();
        args.putInt(MENU_ARG, menuRes);
        args.putString(TITLE_ARG, title);
        args.putBundle(EXTRA_DATA_ARG, extra);
        MenuDialogFragment fragment = new MenuDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.dialog_menu, null);
        ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        MenuAdapter menuAdapter = new MenuAdapter(getMenu(), R.layout.item_dialog_menu);
        menuAdapter.setOnItemClickListener(this::onMenuItemClicked);
        recyclerView.setAdapter(menuAdapter);

        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setView(view)
                .create();
    }

    public void setOnCompleteListener(@Nullable OnCompleteListener<MenuItem> onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setComplexCompleteListener(@Nullable BiCallback<MenuItem, Bundle> complexCompleteListener) {
        this.complexCompleteListener = complexCompleteListener;
    }

    private void onMenuItemClicked(MenuItem menuItem) {
        if (onCompleteListener != null) {
            onCompleteListener.onComplete(menuItem);
        }
        if (complexCompleteListener != null) {
            complexCompleteListener.call(menuItem, getArguments().getBundle(EXTRA_DATA_ARG));
        }
        dismissAllowingStateLoss();
    }

    private Menu getMenu() {
        return createMenu(requireContext(), getArguments().getInt(MENU_ARG));
    }

    private String getTitle() {
        return getArguments().getString(TITLE_ARG);
    }
}
