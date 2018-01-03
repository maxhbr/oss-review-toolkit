/*
 * Copyright (c) 2017-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.downloader

import com.here.ort.downloader.vcs.Subversion

import io.kotlintest.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class SubversionTest : StringSpec() {
    private lateinit var zipContentDir: File

    override val oneInstancePerTest = false

    override fun interceptSpec(context: Spec, spec: () -> Unit) {
        val zipFile = Paths.get("src/test/assets/scannotation-1.0.3-svn.zip")

        zipContentDir = createTempDir()

        println("Extracting '$zipFile' to '$zipContentDir'...")

        FileSystems.newFileSystem(zipFile, null).use { zip ->
            zip.rootDirectories.forEach { root ->
                Files.walk(root).forEach { file ->
                    Files.copy(file, Paths.get(zipContentDir.toString(), file.toString()),
                            StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        try {
            spec()
        } finally {
            zipContentDir.deleteRecursively()
        }
    }

    init {
        "Detected Subversion version is not empty" {
            val version = Subversion.getVersion()
            println("Subversion version $version detected.")
            version shouldNotBe ""
        }

        "Subversion correctly detects URLs to remote repositories" {
            Subversion.isApplicableUrl("http://svn.code.sf.net/p/grepwin/code/") shouldBe true
            Subversion.isApplicableUrl("https://bitbucket.org/facebook/lz4revlog") shouldBe false
        }

        "Detected working tree information is correct" {
            val workingTree = Subversion.getWorkingTree(zipContentDir)

            workingTree.getProvider() shouldBe "Subversion"
            workingTree.isValid() shouldBe true
            workingTree.getRemoteUrl() shouldBe "https://svn.code.sf.net/p/scannotation/code"
            workingTree.getRevision() shouldBe "12"
            workingTree.getRootPath() shouldBe zipContentDir.path.replace(File.separatorChar, '/')
            workingTree.getPathToRoot(File(zipContentDir, "src")) shouldBe "src"
        }
    }
}