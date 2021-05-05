/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.config.Configuration
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.kotlin.api.KotlinCheck
import org.sonarsource.kotlin.converter.KotlinCodeVerifier
import org.sonarsource.kotlin.converter.KotlinConverter
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import org.sonarsource.slang.checks.CommentedCodeCheck
import org.sonarsource.slang.checks.api.SlangCheck
import org.sonarsource.slang.plugin.SlangSensor

class KotlinSensor(
    checkFactory: CheckFactory,
    fileLinesContextFactory: FileLinesContextFactory,
    noSonarFilter: NoSonarFilter,
    language: KotlinLanguage,
) : SlangSensor(noSonarFilter, fileLinesContextFactory, language) {

    @Deprecated("Use Kotlin-native checks instead", replaceWith = ReplaceWith("checks"))
    val legacyChecks: Checks<SlangCheck> = checkFactory.create<SlangCheck>(KotlinPlugin.KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(KotlinCheckList.legacyChecks() as Iterable<*>)
        addAnnotatedChecks(CommentedCodeCheck(KotlinCodeVerifier()))
    }

    val checks: Checks<KotlinCheck<PsiElement>> = checkFactory.create<KotlinCheck<PsiElement>>(KotlinPlugin.KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(KotlinCheckList.checks() as Iterable<*>)
        all().forEach { it.initialize(ruleKey(it)!!) }
    }

    override fun astConverter(sensorContext: SensorContext) =
        KotlinConverter(getFilesFromProperty(sensorContext.config(), SONAR_JAVA_BINARIES)
            + getFilesFromProperty(sensorContext.config(), SONAR_JAVA_LIBRARIES))

    override fun languageSpecificVisitors() = listOf(KtChecksVisitor(checks))

    @Deprecated("Use native Kotlin API instead", replaceWith = ReplaceWith("legacyChecks"))
    override fun checks() = legacyChecks

    override fun repositoryKey() = KotlinPlugin.KOTLIN_REPOSITORY_KEY
}

fun getFilesFromProperty(settings: Configuration, property: String): List<String> =
    settings.get(property).map {
        if (it.isNotBlank()) it.split(",").toList() else emptyList()
    }.orElse(emptyList())
