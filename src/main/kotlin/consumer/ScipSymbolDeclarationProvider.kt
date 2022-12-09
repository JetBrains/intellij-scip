package com.jetbrains.scip.consumer

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.sourcegraph.Scip

class ScipSymbolDeclarationProvider : PsiSymbolDeclarationProvider {
  override fun getDeclarations(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
    val vFile = element.containingFile?.virtualFile ?: return emptyList()
    val occurrences = ScipService.getInstance(element.project).findOccurrencesAt(vFile, element.textRange).filter { it.isDeclaration() }
    return occurrences.map { ScipSymbolDeclaration(element, it) }
  }
}

class ScipSymbolDeclaration(private val element: PsiElement, private val occurrence: Scip.Occurrence) : PsiSymbolDeclaration {
  override fun getDeclaringElement(): PsiElement = element

  override fun getRangeInDeclaringElement(): TextRange = TextRange(0, element.textRange.length)

  override fun getSymbol(): Symbol = ScipSymbol(element.project, occurrence.symbol)
}
