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

import com.bancvue.gradle.ResourceResolver
import com.bancvue.gradle.test.AbstractPluginSpecification
import com.bancvue.gradle.test.TestFile
import nl.javadude.gradle.plugins.license.License

class LicenseExtPluginSpecification extends AbstractPluginSpecification {

	private ResourceResolver resourceResolver
	private LicenseExtPlugin plugin
	private LicenseExtProperties licenseProperties

	String getPluginName() {
		LicenseExtPlugin.PLUGIN_NAME
	}

	void setup() {
		resourceResolver = Mock()
		plugin = new LicenseExtPlugin()
		plugin.init(project)
		licenseProperties = plugin.licenseProperties
		licenseProperties.resourceResolver = resourceResolver
	}

	def "writeDefaultHeaderFile should overwrite any existing content"() {
		given:
		TestFile headerFile = new TestFile(plugin.getHeaderFile())
		headerFile << "existing content"

		when:
		plugin.writeLicenseHeaderToBuildDir()

		then:
		1 * resourceResolver.resolveObjectFromMap(licenseProperties.resourcePath, LicenseModel) >> new LicenseModel(header: "new content")
		headerFile.text =~ /new content/
		!(headerFile.text =~ /existing content/)
	}

}
