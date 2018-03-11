package com.mccorby.photolabeler.presentation

import com.mccorby.photolabeler.filemanager.FileManager
import com.mccorby.photolabeler.trainer.Trainer
import com.mccorby.photolabeler.trainer.TrainerImpl
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

internal class LabellingPresenterTest {
    @Test
    fun `Given a photo is taken when label is applied photo is saved in label directory`() {
        // Given
        val photoPath = "photoPath"
        val label = "photoLabel"
        val fileManager = mock<FileManager>()
        val view = mock<LabellingView>()
        val trainer = mock<Trainer>()

        // When
        val cut = LabellingPresenter(view, fileManager, trainer)
        cut.saveLabelledImage(photoPath, label)

        // Then
        verify(fileManager).saveLabelImage(photoPath, label)
    }

}