package com.bancvue.gradle.test

import org.junit.Test

class TestExtPluginIntegrationTest extends AbstractPluginIntegrationTest {

	@Test
	void shouldCompileMainTestSourceAndCreateJarFromSource() {
		projectFS.file("build.gradle") << """
apply plugin: 'java'
apply plugin: 'test-ext'

mainTestJar.archiveName='mainTest.jar'
        """
		projectFS.emptyClassFile("src/mainTest/java/Class.java")

		run("check", "mainTestJar")

		assert projectFS.file("build/classes/mainTest/Class.class").exists()
		assert projectFS.file("build/libs/mainTest.jar").exists()
	}

}
