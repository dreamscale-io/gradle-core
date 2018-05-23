/*
 * Copyright 2018 DreamScale, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dreamscale.gradle.support

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.revwalk.RevCommit
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip


class ManifestAugmentor {

    private Project project

    ManifestAugmentor(Project project) {
        this.project = project
    }

    void addGitShaToArtifactManifest() {
        String commit = getLatestCommit()

        if (commit != null) {
            project.tasks.withType(Jar) { Jar jar ->
                manifest {
                    attributes 'Git-Sha': commit
                }
            }

            project.tasks.withType(Zip) { Zip zip ->
                if ((zip instanceof Jar) == false) {
                    dependsOn getOrCreateWriteGitShaToManifestTask(commit)
                    from("${project.buildDir}/MANIFEST.properties", {
                        into new File('META-INF')
                    })
                }
            }
        }
    }

    private Task getOrCreateWriteGitShaToManifestTask(String gitSha) {
        String taskName = "writeGitShaToManifest"
        Task writeManifestTask = project.tasks.findByName(taskName)
        if (writeManifestTask == null) {
            writeManifestTask = project.tasks.create(taskName) {
                doLast {
                    project.buildDir.mkdir()
                    def manifest = new File("${project.buildDir}/MANIFEST.properties")
                    writeGitShaToManifest(gitSha, manifest)
                }
            }
        }
        writeManifestTask
    }

    private void writeGitShaToManifest(String gitSha, File manifest) {
        Properties manifestProperties = new Properties()
        manifestProperties.setProperty("Git-Sha", gitSha)
        OutputStream outputStream = new FileOutputStream(manifest)
        manifestProperties.store(outputStream, "This is the UI manifest for meta information")
    }


    private String getLatestCommit() {
        Git repo
        try {
            repo = Git.open(project.projectDir)
        } catch (RepositoryNotFoundException ex) {
            return null
        }

        Iterator<RevCommit> commits = repo
                .log()
                .setMaxCount(1)
                .call()
                .iterator()
        commits.hasNext() ? commits.next().name : null
    }

}
