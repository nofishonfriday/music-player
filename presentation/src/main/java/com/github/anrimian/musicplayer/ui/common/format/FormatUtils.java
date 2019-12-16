package com.github.anrimian.musicplayer.ui.common.format;

import android.content.Context;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.composition.order.OrderType;
import com.github.anrimian.musicplayer.domain.models.player.modes.RepeatMode;
import com.github.anrimian.musicplayer.domain.utils.java.Callback;
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.touch_helper.drag_and_swipe.DragAndSwipeTouchHelperCallback;

import java.util.Locale;

import static android.text.TextUtils.isEmpty;
import static com.github.anrimian.musicplayer.ui.utils.ViewUtils.run;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created on 15.11.2017.
 */

public class FormatUtils {

    public static String formatCompositionsCount(Context context, int compositionsCount) {
        return context.getResources().getQuantityString(
                R.plurals.compositions_count,
                compositionsCount,
                compositionsCount);
    }

    public static StringBuilder formatCompositionAuthor(Composition composition, Context context) {
        String author = composition.getArtist();
        return formatAuthor(author, context);
    }

    public static StringBuilder formatAuthor(String author, Context context) {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(author)) {
            sb.append(author);
        } else {
            sb.append(context.getString(R.string.unknown_author));
        }
        return sb;
    }

    public static String formatMilliseconds(long millis) {
        StringBuilder sb = new StringBuilder();

        long hours = MILLISECONDS.toHours(millis);
        if (hours != 0) {
            sb.append(format(Locale.getDefault(), "%02d", hours));
            sb.append(":");
        }

        long minutes = MILLISECONDS.toMinutes(millis) - HOURS.toMinutes(hours);
        if (minutes != 0 || hours != 0) {
            sb.append(format(Locale.getDefault(), "%02d", minutes));

        } else {
            sb.append("00");
        }
        sb.append(":");

        long seconds = MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis));
        sb.append(format(Locale.getDefault(), "%02d", seconds));
        return sb.toString();
    }

    public static int getOrderTitle(OrderType orderType) {
        switch (orderType) {
            case ALPHABETICAL: return R.string.alphabetical_order;
            case ADD_TIME: return R.string.add_date_order;
            case COMPOSITION_COUNT: return R.string.by_composition_count;
            case DURATION: return R.string.by_duration;
            default: throw new IllegalStateException("can not find title for order: " + orderType);
        }
    }

    public static int getReversedOrderText(OrderType orderType) {
        switch (orderType) {
            case ALPHABETICAL: return R.string.alphabetical_order_desc_title;
            case ADD_TIME: return R.string.add_date_order_desc_title;
            case COMPOSITION_COUNT: return R.string.more_first;
            case DURATION: return R.string.longest_first;
            default: throw new IllegalStateException("can not find title for order: " + orderType);
        }
    }

    @DrawableRes
    public static int getRepeatModeIcon(int repeatMode) {
        switch (repeatMode) {
            case RepeatMode.NONE: {
                return R.drawable.ic_repeat_off;
            }
            case RepeatMode.REPEAT_COMPOSITION: {
                return R.drawable.ic_repeat_once;
            }
            case RepeatMode.REPEAT_PLAY_LIST: {
                return R.drawable.ic_repeat;
            }
            default: return R.drawable.ic_repeat_off;
        }
    }

    public static void formatLinkedFabView(View view, View fab) {
        run(fab, () -> {
            int width = fab.getWidth();
            int margin = view.getResources().getDimensionPixelSize(R.dimen.content_horizontal_margin);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
            params.setMarginEnd(width + margin * 2);
            params.height = fab.getHeight();
            view.setLayoutParams(params);
        });
    }

    public static DragAndSwipeTouchHelperCallback withSwipeToDelete(RecyclerView recyclerView,
                                                                    @ColorInt int backgroundColor,
                                                                    Callback<Integer> swipeCallback,
                                                                    int swipeFlags,
                                                                    @DrawableRes int iconRes,
                                                                    @StringRes int textResId) {
        return DragAndSwipeTouchHelperCallback.withSwipeToDelete(recyclerView,
                backgroundColor,
                swipeCallback,
                swipeFlags,
                iconRes,
                textResId,
                R.dimen.swipe_panel_width,
                R.dimen.swipe_panel_padding_end,
                R.dimen.swipe_panel_text_top_padding,
                R.dimen.swipe_panel_icon_size,
                R.dimen.swipe_panel_text_size);
    }
}
