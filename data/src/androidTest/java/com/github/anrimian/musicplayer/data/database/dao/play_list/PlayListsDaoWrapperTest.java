package com.github.anrimian.musicplayer.data.database.dao.play_list;

import static com.github.anrimian.musicplayer.data.utils.TestDataProvider.composition;
import static com.github.anrimian.musicplayer.domain.utils.ListUtils.asList;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.anrimian.musicplayer.data.database.AppDatabase;
import com.github.anrimian.musicplayer.data.database.dao.compositions.CompositionsDao;
import com.github.anrimian.musicplayer.data.database.entities.playlist.PlayListEntryEntity;
import com.github.anrimian.musicplayer.data.storage.providers.playlists.StoragePlayList;
import com.github.anrimian.musicplayer.domain.models.playlist.PlayListItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

public class PlayListsDaoWrapperTest {

    private PlayListDao playListDao;
    private CompositionsDao compositionsDao;
    private AppDatabase db;

    private PlayListsDaoWrapper daoWrapper;

    @BeforeEach
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        compositionsDao = db.compositionsDao();
        playListDao = db.playListDao();

        daoWrapper = new PlayListsDaoWrapper(playListDao, compositionsDao, db);
    }

    @AfterEach
    public void tearDown() {
        db.close();
    }

    @Test
    public void testMoveItems() {
        long playlistId = daoWrapper.insertPlayList("playlist", new Date(), new Date(), () -> null);

        for (int i = 0; i < 10; i++) {
            long id = compositionsDao.insert(composition(null, null, String.valueOf(i)));
            playListDao.insertPlayListEntries(asList(new PlayListEntryEntity(
                    null,
                    id,
                    playlistId,
                    i
            )));
        }

        List<PlayListItem> items = daoWrapper.getPlayListItemsObservable(playlistId, false, null)
                .blockingFirst();
        displayItems("testMoveItems, items: ", items);

        daoWrapper.moveItems(playlistId, 0, 7);

        items = daoWrapper.getPlayListItemsObservable(playlistId, false, null)
                .blockingFirst();
        displayItems("testMoveItems, moved items: ", items);

    }

    @Test
    public void testUpdatePlaylistNameThatAlreadyExists() {
        Date date = new Date();
        daoWrapper.insertPlayList("test", date, date, () -> 1L);
        daoWrapper.insertPlayList("test1", date, date, () -> 2L);
        daoWrapper.insertPlayList("test2", date, date, () -> 3L);

        StoragePlayList duplicatePlayList2 = new StoragePlayList(2L, "test", date, date);
        StoragePlayList duplicatePlayList3 = new StoragePlayList(3L, "test", date, date);
//        daoWrapper.applyChanges(emptyList(), asList(
//                new Change<>(playList2, duplicatePlayList2),
//                new Change<>(playList3, duplicatePlayList3)
//        ));

        System.out.println("KEKAS" + daoWrapper.getPlayListsObservable().blockingFirst());
    }

    private void displayItems(String message, List<PlayListItem> items) {
        StringBuilder sb = new StringBuilder();
        for (PlayListItem item : items) {
            sb.append("\n");
            sb.append("itemId = ");
            sb.append(item.getItemId());
            sb.append("; title = ");
            sb.append(item.getComposition().getTitle());
            sb.append(";");
        }
        String text = sb.toString();
        Log.d("KEK", message + text);
        System.out.println(message + text);
    }
}