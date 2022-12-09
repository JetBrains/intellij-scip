package com.jetbrains.scip.consumer

import com.intellij.find.usages.api.SearchTarget
import com.intellij.find.usages.api.UsageHandler
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.navigation.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.scip.ScipSymbolDescriptor
import com.sourcegraph.Scip

class ScipImplicitReferenceProvider : ImplicitReferenceProvider {
  override fun resolveAsReference(element: PsiElement): Collection<Symbol> {
    val vFile = element.containingFile?.virtualFile ?: return emptyList()
    val occurrences = ScipService.getInstance(element.project)
      .findOccurrencesAt(vFile, element.textRange)
      .filter { !it.isDeclaration() }
    return occurrences.map { ScipSymbol(element.project, it.symbol) }
  }
}

class ScipSymbol(private val project: Project, val symbolString: String) : Symbol, NavigatableSymbol, SearchTarget {
  override fun createPointer(): Pointer<out ScipSymbol> {
    return Pointer.hardPointer(this)
  }

  override fun presentation(): TargetPresentation {
    val symbolInDocument = ScipService.getInstance(project).symbols[symbolString] ?: return TargetPresentation.builder("???").presentation()
    return symbolInDocument.symbol.toPresentation()
  }

  override val usageHandler: UsageHandler
    get() {
      val presentation = ScipService.getInstance(project).symbols[symbolString]?.symbol?.toPresentation()
      val presentableString = presentation?.presentableText ?: symbolString
      return UsageHandler.createEmptyUsageHandler(presentableString)
    }

  override fun getNavigationTargets(project: Project): Collection<NavigationTarget> {
    val symbolInDocument = ScipService.getInstance(project).symbols[symbolString] ?: return emptyList()
    return listOf(ScipNavigationTarget(project, symbolInDocument))
  }

  override fun equals(other: Any?): Boolean {
    return other is ScipSymbol && other.symbolString == symbolString
  }

  override fun hashCode(): Int {
    return symbolString.hashCode()
  }
}

class ScipNavigationTarget(
  private val project: Project,
  private val symbolInDocument: SymbolInDocument
) : NavigationTarget {
  override fun createPointer(): Pointer<out NavigationTarget> {
    return Pointer.hardPointer(this)
  }

  override fun presentation(): TargetPresentation {
    return symbolInDocument.symbol.toPresentation()
  }

  override fun navigationRequest(): NavigationRequest? {
    val position = ScipService.getInstance(project).resolvePosition(symbolInDocument.document, symbolInDocument.definitionRange)
                   ?: return null
    return NavigationService.instance().sourceNavigationRequest(position.first, position.second.startOffset)
  }
}

fun Scip.SymbolInformation.toPresentation(): TargetPresentation {
  val descriptor = ScipSymbolDescriptor.parse(symbol)
  return TargetPresentation.builder(descriptor.toPresentableString()).presentation()
}
