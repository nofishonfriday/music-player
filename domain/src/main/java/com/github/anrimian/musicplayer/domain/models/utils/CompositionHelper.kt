package com.github.anrimian.musicplayer.domain.models.utils

import com.github.anrimian.musicplayer.domain.models.composition.DeletedComposition
import com.github.anrimian.musicplayer.domain.models.composition.FullComposition
import com.github.anrimian.musicplayer.domain.models.sync.FileKey

fun FullComposition.isFileExists() = storageId != null

fun DeletedComposition.toFileKey() = FileKey(fileName, parentPath)

fun List<DeletedComposition>.toFileKeys() = map(DeletedComposition::toFileKey)