package com.github.anrimian.musicplayer.data.controllers.music.equalizer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.widget.Toast;

import androidx.annotation.NonNull;

class ExternalEqualizer {

    public static void attachExternalEqualizer(Context context, int audioSessionId) {
        Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
        context.sendBroadcast(intent);
    }

    public static void detachExternalEqualizer(Context context, int audioSessionId) {
        Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
        context.sendBroadcast(intent);
    }

    public static void launchExternalEqualizerSetup(Activity activity, int audioSessionId) {
        Toast.makeText(activity, "sessionId: " + audioSessionId, Toast.LENGTH_LONG).show();
        if (audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(activity, "No Session Id", Toast.LENGTH_LONG).show();
        } else {
            try {
                Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, activity.getPackageName());
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
                intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                activity.startActivityForResult(intent, 0);
            } catch (@NonNull final ActivityNotFoundException notFound) {
                //check on dialog start and show message
                Toast.makeText(activity, "There is no equalizer", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
