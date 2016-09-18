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
package com.bancvue.gradle.license

import nl.javadude.gradle.plugins.license.License
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class LicenseExtPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = 'com.bancvue.license-ext'
	private static final String GROUP_NAME = 'License'

	private Project project
	private LicenseExtProperties licenseProperties

	@Override
	void apply(Project project) {
		init(project)
		applyLicensePlugin()
		configureLicenseHeader()
		addCheckAllLicenseTask()
		setGroupOnLicenseTasks()
	}

	private void init(Project project) {
		this.project = project
		licenseProperties = new LicenseExtProperties(project)
	}

	private void applyLicensePlugin() {
		project.apply(plugin: 'license')
	}

	private void configureLicenseHeader() {
		writeConfiguredLicenseHeaderToBuildDirPriorToLicenseExecution()
		project.license {
			header = getHeaderFile()
			ext.year = Calendar.getInstance().get(Calendar.YEAR)
			if (licenseProperties.name) {
				ext.name = licenseProperties.name
			}
		}
	}

	private void writeConfiguredLicenseHeaderToBuildDirPriorToLicenseExecution() {
		Task writeLicenseHeaderToBuildDirTask = project.tasks.create('writeLicenseHeaderToBuildDir')
		writeLicenseHeaderToBuildDirTask.doLast {
			writeLicenseHeaderToBuildDir()
		}

		project.tasks.withType(License) { License task ->
			task.dependsOn(writeLicenseHeaderToBuildDirTask)
		}
	}

	private void writeLicenseHeaderToBuildDir() {
		LicenseModel license = licenseProperties.acquireLicenseModel()
		File headerFile = getHeaderFile()

		headerFile.parentFile.mkdirs()
		headerFile.write(license.header)
	}

	private File getHeaderFile() {
		new File(project.buildDir, "license/HEADER")
	}

	private void setGroupOnLicenseTasks() {
		project.tasks.findByName("licenseFormat")?.group = GROUP_NAME
		project.tasks.findByName("downloadLicenses")?.group = GROUP_NAME
	}

	private void addCheckAllLicenseTask() {
		Task licenseCheck = project.tasks.create('licenseCheck')
		licenseCheck.group = GROUP_NAME
		licenseCheck.description = 'Check license on files from all available source sets'

		// this must be done after evaluation, otherwise - if a License task is added after this plugin
		// is applied - the closure will be called when the task is added to the container, which will probably
		// be before the check value is set
		project.afterEvaluate {
			project.tasks.withType(License) { License task ->
				if (task.check) {
					licenseCheck.dependsOn(task)
				}
			}
		}
	}

}
