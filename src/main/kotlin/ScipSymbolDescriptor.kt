package com.jetbrains.scip

import com.intellij.openapi.util.NlsSafe
import com.sourcegraph.Scip

sealed class ScipSymbolDescriptor {
  abstract fun toPresentableString(): @NlsSafe String

  companion object {
    fun parse(symbol: String): ScipSymbolDescriptor {
      val (scheme, rest) = symbol.split(' ', limit = 2)
      if (scheme == "local") {
        return ScipLocalSymbolDescriptor(rest)
      }
      val (manager, packageName, version, descriptor) = rest.split(' ')
      return ScipGlobalSymbolDescriptor(scheme, manager, packageName, version, descriptor)
    }
  }
}

class ScipGlobalSymbolDescriptor(
  val scheme: String,
  val manager: String,
  val packageName: String,
  val version: String,
  descriptor: String) : ScipSymbolDescriptor() {

  val descriptorElements = parseDescriptorElements(descriptor)

  override fun toPresentableString(): String {
    return descriptorElements.joinToString(".") { it.name }
  }

  companion object {
    fun parseDescriptorElements(descriptor: String): List<ScipSymbolDescriptorElement> {
      val result = mutableListOf<ScipSymbolDescriptorElement>()

      var index = 0

      fun parseDescriptorElementUntil(endChar: Char, suffix: Scip.Descriptor.Suffix): ScipSymbolDescriptorElement {
        val lastIndex = descriptor.indexOf(endChar, index + 1)
        val name = descriptor.substring(index + 1, lastIndex)
        index = lastIndex + 1
        return ScipSymbolDescriptorElement(name, null, suffix)
      }

      fun parseDescriptorElement(): ScipSymbolDescriptorElement {
        if (descriptor[index] == '(') {
          return parseDescriptorElementUntil(')', Scip.Descriptor.Suffix.Parameter)
        }
        if (descriptor[index] == '[') {
          return parseDescriptorElementUntil(']', Scip.Descriptor.Suffix.TypeParameter)
        }
        val lastIndex = descriptor.indexOfAny("/#.:(".toCharArray(), index)
        if (lastIndex == -1) {
          index = descriptor.length
          return ScipSymbolDescriptorElement(descriptor.substring(index),null, Scip.Descriptor.Suffix.UnspecifiedSuffix)
        }
        val name = descriptor.substring(index, lastIndex)
        if (descriptor[lastIndex] == '(') {
          val nameEndIndex = descriptor.indexOf(").", lastIndex + 1)
          index = nameEndIndex + 1
          return ScipSymbolDescriptorElement(
            name,
            descriptor.substring(lastIndex+1, nameEndIndex),
            Scip.Descriptor.Suffix.Method
          )
        }
        val suffix = when (descriptor[lastIndex]) {
          '/' -> Scip.Descriptor.Suffix.Package
          '#' -> Scip.Descriptor.Suffix.Type
          '.' -> Scip.Descriptor.Suffix.Term
          ':' -> Scip.Descriptor.Suffix.Meta
          else -> throw IllegalStateException()
        }
        index = lastIndex + 1
        return ScipSymbolDescriptorElement(name, null, suffix)
      }

      while (index < descriptor.length) {
        result.add(parseDescriptorElement())
      }
      return result
    }
  }
}

class ScipSymbolDescriptorElement(
  val name: String,
  val disambiguator: String?,
  val suffix: Scip.Descriptor.Suffix
) {
}

class ScipLocalSymbolDescriptor(val localId: String) : ScipSymbolDescriptor() {
  override fun toPresentableString(): String {
    return localId
  }
}
