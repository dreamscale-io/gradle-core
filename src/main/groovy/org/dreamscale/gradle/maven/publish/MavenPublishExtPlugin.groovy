/**
 * Copyright 2013 BancVue, LTD
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
 */
package org.dreamscale.gradle.maven.publish

import org.dreamscale.gradle.GradlePluginMixin
import groovy.util.logging.Slf4j
import org.dreamscale.gradle.JavaExtPlugin
import org.dreamscale.gradle.maven.MavenRepositoryProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

@Slf4j
@Mixin(GradlePluginMixin)
class MavenPublishExtPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'org.dreamscale.maven-publish-ext'

	private Project project
	private MavenRepositoryProperties repositoryProperties

	void apply(Project project) {
		this.project = project
		this.repositoryProperties = new MavenRepositoryProperties(project)
		project.apply(plugin: JavaExtPlugin.PLUGIN_NAME)
		addArtifactDependencyAndPublishingSupport()
		addPublishingExtExtension()
		applyPomSettingsFromProperties()
	}

	private void addPublishingExtExtension() {
		project.extensions.create(MavenPublishExtExtension.NAME, MavenPublishExtExtension, project)
	}

	private void addArtifactDependencyAndPublishingSupport() {
		project.apply(plugin: 'maven-publish')
		createPublishLocalAlias()
		addMavenLocalAndOrganizationArtifactRepository()
		addOrganizationPublishingRepository()
	}

	private void createPublishLocalAlias() {
		Task publishToMavenLocal = project.tasks.findByName('publishToMavenLocal')
		Task publishLocal = project.tasks.create('publishLocal')
		publishLocal.dependsOn { publishToMavenLocal }
		publishLocal.group = publishToMavenLocal.group
		publishLocal.description = 'Publishes all Maven publications produced by this project to the local Maven cache.'
	}

	private void addMavenLocalAndOrganizationArtifactRepository() {
		project.repositories.mavenLocal()
		project.repositories {
			maven {
				if (repositoryProperties.hasReadCredentialsDefined()) {
					credentials {
						username repositoryProperties.readUsername
						password repositoryProperties.readPassword
					}
				}
				name repositoryProperties.name
				url repositoryProperties.publicUrl
			}
		}
	}

	private boolean isSnapshotProject() {
		project.version =~ /SNAPSHOT$/
	}

	private String acquireRepositoryPublishUrl() {
		if (isSnapshotProject()) {
			repositoryProperties.snapshotUrl
		} else {
			repositoryProperties.releaseUrl
		}
	}

	private void addOrganizationPublishingRepository() {
		project.publishing {
			repositories {
				maven {
					name repositoryProperties.name
					url acquireRepositoryPublishUrl()
					if (repositoryProperties.hasPublishCredentialsDefined()) {
						credentials {
							username repositoryProperties.username
							password repositoryProperties.password
						}
					}
				}
			}
		}
	}

	private void applyPomSettingsFromProperties() {
		String artifactId = getOptionalProjectProperty("artifactId")
		String orgRepoUrl = getOptionalProjectProperty("organization.repo.url")
		String orgRepoConnection = getOptionalProjectProperty("organization.repo.connection")
		String pomPackaging = getOptionalProjectProperty("maven.pom.packaging")
		String pomDeveloperId = getOptionalProjectProperty("maven.pom.developer.id")

		// the default packaging is jar so don't specify
		if (pomPackaging == "jar") {
			pomPackaging = null
		}

		project.publishing_ext {
			pom {
				if (pomPackaging != null) {
					packaging pomPackaging
				}
				if (orgRepoUrl != null && artifactId != null) {
					url "${orgRepoUrl}/${artifactId}"
				}
				if (pomDeveloperId != null) {
					developers {
						developer {
							id pomDeveloperId
							name getOptionalProjectProperty("maven.pom.developer.name")
							email getOptionalProjectProperty("maven.pom.developer.email")
						}
					}
				}
				if (orgRepoUrl != null && artifactId != null) {
					scm {
						url "${orgRepoUrl}/${artifactId}.git"
						if (orgRepoConnection != null) {
							connection "${orgRepoConnection}/${artifactId}.git"
							developerConnection "${orgRepoConnection}/${artifactId}.git"
						}
					}
				}
			}
		}
	}

	private String getOptionalProjectProperty(String propertyName) {
		project.hasProperty(propertyName) ? project.property(propertyName) : null
	}

}