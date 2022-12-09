package com.jetbrains.scip.consumer

import com.intellij.find.usages.api.PsiUsage
import com.intellij.find.usages.api.Usage
import com.intellij.find.usages.api.UsageSearchParameters
import com.intellij.find.usages.api.UsageSearcher
import com.intellij.psi.PsiManager

class ScipReferenceSearcher : UsageSearcher {
  override fun collectImmediateResults(parameters: UsageSearchParameters): Collection<Usage> {
    val target = parameters.target
    if (target is ScipSymbol) {
      val project = parameters.project
      val scipService = ScipService.getInstance(project)
      return scipService.findSymbolOccurrences(target.symbolString).mapNotNull {
        scipService.resolvePosition(it.document, it.occurrence.rangeList)?.let { (vFile, range) ->
          PsiManager.getInstance(project).findFile(vFile)?.let { psiFile ->
            PsiUsage.textUsage(psiFile, range)
          }
        }
      }
    }
    return emptyList()
  }
}
