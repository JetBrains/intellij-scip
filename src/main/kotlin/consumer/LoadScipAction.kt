package com.jetbrains.scip.consumer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.sourcegraph.Scip

class LoadScipAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val fileChooser = FileChooserFactory.getInstance().createFileChooser(
      FileChooserDescriptorFactory.createSingleFileDescriptor(),
      project,
      e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    )
    val virtualFile = fileChooser.choose(project).singleOrNull() ?: return
    val index = Scip.Index.parseFrom(virtualFile.contentsToByteArray())
    ScipService.getInstance(project).run {
      activeIndex = index
      baseDir = virtualFile.parent
    }
  }
}
