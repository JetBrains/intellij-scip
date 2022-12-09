package com.jetbrains.scip.consumer

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.sourcegraph.Scip

data class SymbolInDocument(
  val document: Scip.Document,
  val symbol: Scip.SymbolInformation,
  val definitionRange: List<Int>?
)

data class OccurrenceInDocument(
  val document: Scip.Document,
  val occurrence: Scip.Occurrence
)

class ScipService {
  val symbols = mutableMapOf<String, SymbolInDocument>()

  var baseDir: VirtualFile? = null
  var activeIndex: Scip.Index? = null
    get() = field
    set(value) {
      field = value
      updateSymbolMap(value)
    }

  private fun updateSymbolMap(index: Scip.Index?) {
    symbols.clear()
    if (index != null) {
      for (document in index.documentsList) {
        val definitionRanges = mutableMapOf<String, List<Int>>()
        for (occurrence in document.occurrencesList) {
          if (occurrence.isDeclaration()) {
            definitionRanges[occurrence.symbol] = occurrence.rangeList
          }
        }
        for (symbolInformation in document.symbolsList) {
          symbols[symbolInformation.symbol] = SymbolInDocument(document, symbolInformation, definitionRanges[symbolInformation.symbol])
        }
      }
    }
  }

  fun findIndexDocument(file: VirtualFile): Scip.Document? {
    return activeIndex?.documentsList?.find { file.path.endsWith(it.relativePath) }
  }

  fun findOccurrencesAt(file: VirtualFile, range: TextRange): List<Scip.Occurrence> {
    val indexDoc = findIndexDocument(file) ?: return emptyList()
    val doc = FileDocumentManager.getInstance().getDocument(file) ?: return emptyList()
    val scipRange = rangeToScipFormat(doc, range)
    return indexDoc.occurrencesList.filter { it.rangeList == scipRange }
  }

  private fun rangeToScipFormat(doc: Document, range: TextRange): List<Int> {
    val startLine = doc.getLineNumber(range.startOffset)
    val startChar = range.startOffset - doc.getLineStartOffset(startLine)
    val endLine = doc.getLineNumber(range.endOffset)
    val endChar = range.endOffset - doc.getLineStartOffset(endLine)
    return if (startLine == endLine) listOf(startLine, startChar, endChar) else listOf(startLine, startChar, endLine, endChar)
  }

  fun findSymbolOccurrences(symbolString: String): List<OccurrenceInDocument> {
    return activeIndex?.documentsList?.flatMap {
      doc -> doc.occurrencesList.filter { it.symbol == symbolString }.map { OccurrenceInDocument(doc, it) }
    } ?: emptyList()
  }

  fun resolvePosition(indexDoc: Scip.Document, scipRange: List<Int>?): Pair<VirtualFile, TextRange>? {
    val vFile = baseDir?.findFileByRelativePath(indexDoc.relativePath) ?: return null
    var startOffset = 0
    var endOffset = 0
    if (scipRange != null) {
      val doc = FileDocumentManager.getInstance().getDocument(vFile)
      if (doc != null) {
        startOffset = doc.getLineStartOffset(scipRange[0]) + scipRange[1]
        val endLine = if (scipRange.size == 3) scipRange[0] else scipRange[2]
        endOffset = doc.getLineEndOffset(endLine) + scipRange.last()
      }
    }
    return vFile to TextRange(startOffset, endOffset)
  }

  companion object {
    fun getInstance(project: Project): ScipService = project.getService(ScipService::class.java)
  }
}

fun Scip.Occurrence.isDeclaration() = symbolRoles and Scip.SymbolRole.Definition.number != 0
