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
package org.dreamscale.gradle.test

import org.dreamscale.gradle.GradlePluginMixin
import org.dreamscale.gradle.support.CommonTaskFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

@Mixin(GradlePluginMixin)
class TestExtPlugin implements Plugin<Project> {

	static final String PLUGIN_NAME = "org.dreamscale.test-ext"
	static final String VERIFICATION_GROUP_NAME = "Verification"


	private Project project

	@Override
	void apply(Project project) {
		this.project = project
		project.apply(plugin: "java")
		configureMainTestAndSharedTest()
		updateTestLoggersToWriteStackTracesOnTestFailure()
		udpateTestLoggersToWriteSkippedTestEvents()
		addStyledTestOutputTask()
	}

	private void configureMainTestAndSharedTest() {
		addMainTestAndSharedTestConfigurations()
		addMainTestAndSharedTestSourceSets()
		updateSourceSetTestToIncludeConfigurationSharedTest()

		if (project.file("src/mainTest").exists()) {
			addMainTestJarTasks()
		}
	}

	private void addMainTestAndSharedTestConfigurations() {
		createNamedConfigurationExtendingFrom("mainTest", ["compile"], ["compileOnly"], [])
		createNamedConfigurationExtendingFrom("sharedTest", ["compile", "mainTestCompile"], ["compileOnly", "mainTestCompileOnly"], ["runtime"])
	}

	private void addMainTestAndSharedTestSourceSets() {
		project.sourceSets {
			mainTest {
				compileClasspath = main.output + compileClasspath
				runtimeClasspath = mainTest.output + main.output + runtimeClasspath
			}
		}

		project.sourceSets {
            sharedTest {
                compileClasspath = mainTest.output + main.output + compileClasspath
                runtimeClasspath = sharedTest.output + mainTest.output + main.output + runtimeClasspath
            }
        }
	}

	private void addMainTestJarTasks() {
		SourceSet mainTest = project.sourceSets.mainTest
		CommonTaskFactory taskFactory = new CommonTaskFactory(project, mainTest)

		taskFactory.createJarTask()
		taskFactory.createSourcesJarTask()
		taskFactory.createJavadocJarTask()
	}

	private void updateSourceSetTestToIncludeConfigurationSharedTest() {
		project.sourceSets {
			test {
				compileClasspath = sharedTest.output + sharedTest.compileClasspath + compileClasspath
				runtimeClasspath = test.output + sharedTest.runtimeClasspath + runtimeClasspath
			}
		}
	}

	private void updateTestLoggersToWriteStackTracesOnTestFailure() {
		project.tasks.withType(Test) { Test test ->
			test.testLogging.exceptionFormat = "full"
			test.testLogging.stackTraceFilters("groovy")
		}
	}

	private void udpateTestLoggersToWriteSkippedTestEvents() {
		project.tasks.withType(Test) { Test test ->
			test.testLogging.events("skipped")
		}
	}

	private void addStyledTestOutputTask() {
		StyledTestOutput stoTask = project.tasks.create("styledTestOutput", StyledTestOutput)
		stoTask.configure {
			group = VERIFICATION_GROUP_NAME
			description = "Modifies build to output test results incrementally"
		}

		project.tasks.withType(Test) { Test test ->
			test.mustRunAfter stoTask
		}
	}

}
