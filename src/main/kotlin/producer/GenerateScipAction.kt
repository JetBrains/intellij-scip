package com.jetbrains.scip.producer

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.jetbrains.scip.ScipBundle

class GenerateScipAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(
      FileSaverDescriptor(ScipBundle.message("dialog.title.generate.skip.file"),
                          ScipBundle.message("label.enter.skip.file.name")),
      e.project
    )
    val file = dialog.save(null) ?: return

  }

  override fun update(e: AnActionEvent) {
    val files = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY)
    e.presentation.isEnabledAndVisible = !files.isNullOrEmpty()
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }
}
