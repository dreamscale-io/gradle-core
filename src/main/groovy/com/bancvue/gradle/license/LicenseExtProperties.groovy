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

import com.bancvue.gradle.DefaultProjectPropertyContainer
import groovy.util.logging.Slf4j
import org.gradle.api.Project

@Slf4j
class LicenseExtProperties extends DefaultProjectPropertyContainer {

	private static final String NAME = "license"

	private static final class Props {
		String name
		String resourcePath = "licenses/apache_2.0"
	}

	@Delegate
	private Props props = new Props()

	LicenseExtProperties(Project project) {
		super(project, NAME)
	}

	LicenseModel acquireLicenseModel() {
		LicenseModel model = getLicenseModel()
		if (model == null) {
			throw new RuntimeException("Failed to resolve license from path=${resourcePath}")
		}
		model
	}

	LicenseModel getLicenseModel() {
		log.info("Resolving license from path=${resourcePath}")
		resourceResolver.resolveObjectFromMap(resourcePath, LicenseModel)
	}

}
