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
package org.dreamscale.gradle.ide

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger

class IdeaProject {

    private Project project

    IdeaProject(Project project) {
        this.project = project
    }

    private IdeaExtExtension getIdeaExtension() {
        IdeaExtExtension.getInstance(project)
    }

    void updateIdeaSourcePathsAndScopesPriorToModuleTaskExecution() {
        Task ideaModule = project.tasks.getByName('ideaModule')

        ideaModule.doFirst {
            updateIdeaSourcePathsAndScopes()
        }
    }

    private void updateIdeaSourcePathsAndScopes() {
        ProjectInfo info = new ProjectInfo(project)

        project.idea {
            module {
                sourceDirs += info.sourceDirs
                testSourceDirs += info.testSourceDirs
                scopes.COMPILE.plus += info.compileConfigurations
                scopes.RUNTIME.plus += info.runtimeConfigurations - info.compileConfigurations
                scopes.TEST.plus += info.testConfigurations

                iml {
                    withXml { provider ->
                        provider.node.component.content.sourceFolder.each { Node sourceFolder ->
                            if (sourceFolder.@url =~ /resources$/) {
                                sourceFolder.@type = getSourceFolderTypeString(info, sourceFolder.@url)
                            }
                        }
                    }
                }
            }
        }
    }

    private String getSourceFolderTypeString(ProjectInfo info, String sourceFolderUrl) {
        String partialSrcFolderUrl = sourceFolderUrl - "file://\$MODULE_DIR\$"
        boolean isTestSourceFolder = info.testSourceDirs.find { File testSourceDir ->
            testSourceDir.absolutePath.endsWith(partialSrcFolderUrl)
        }
        isTestSourceFolder ? "java-test-resource" : "java-resource"
    }

    void enableIdeaAnnotationProcessing() {
        project.idea.project.ipr {
            withXml { provider ->
                provider.node.component.find {
                    it.@name == 'CompilerConfiguration'
                }.annotationProcessing.replaceNode { node ->
                    annotationProcessing {
                        profile(default: "true", name: "Default", enabled: "true") {
                            processorPath(useClasspath: "true")
                        }
                    }
                }
            }
        }
    }

    void enableECMAScript6() {
        addComponentOption("JavaScriptSettings", "languageLevel", "ES6")
    }

    void addIdeaGitMappingNodeIfGitRepo() {
        if (new File(project.projectDir, ".git").exists()) {
            project.idea.project.ipr {
                withXml { provider ->
                    Node vcsDirectoryMappingNode = provider.node.component.find {
                        it.@name == 'VcsDirectoryMappings'
                    }

                    if (!declaresGitMappingNode(vcsDirectoryMappingNode)) {
                        vcsDirectoryMappingNode.appendNode('mapping', [directory: '$PROJECT_DIR$', vcs: 'Git'])
                    }
                }
            }
        }
    }

    private boolean declaresGitMappingNode(Node vcsDirectoryMappingNode) {
        vcsDirectoryMappingNode?.mapping?.find {
            it.@vcs == 'Git'
        }
    }

    private void addComponentOption(String componentName, String optionName, String optionValue) {
        project.idea.project.ipr {
            withXml { provider ->
                Node componentNode = provider.node.component.find {
                    it.@name == componentName
                }

                if (componentNode == null) {
                    provider.node.appendNode('component', [name: componentName]).appendNode('option', [name: optionName, value: optionValue])
                } else {
                    componentNode.option.find {
                        it.@name == optionName
                    }.replaceNode {
                        option(name: optionName, value: optionValue)
                    }
                }
            }
        }
    }

    void configureDefaultConfigurationVmParameters() {
        project.afterEvaluate {
            for (Map.Entry<String, List> entry : ideaExtension.defaultConfigurationVmParameters.entrySet()) {
                setDefaultConfigurationVmParameters(entry.key, entry.value)
            }
        }
    }

    private void setDefaultConfigurationVmParameters(String configurationName, List vmParameters) {
        String vmParameterString = createVmParameterString(vmParameters)

        project.idea {
            workspace {
                iws.withXml { xmlFile ->
                    def runManager = xmlFile.asNode().component.find { it.@name == 'RunManager' }
                    def configurationNode = runManager.configuration.find {
                        it.@default == 'true' && it.@factoryName == configurationName
                    }

                    if (configurationNode == null) {
                        String type = getConfigurationType(configurationName)
                        configurationNode = runManager.appendNode('configuration', [default: 'true', type: type])
                        configurationNode.appendNode('option', [name: 'VM_PARAMETERS'])
                    }

                    configurationNode.option.find { it.@name == 'VM_PARAMETERS' }.replaceNode {
                        option(name: 'VM_PARAMETERS', value: vmParameterString)
                    }
                }
            }
        }
    }

    private String getConfigurationType(String factoryName) {
        if (factoryName == "Spring Boot") {
            return "SpringBootApplicationConfigurationType"
        }
        factoryName
    }

    private String createVmParameterString(List<String> vmParameters) {
        StringBuilder builder = new StringBuilder()
        vmParameters.each { def vmParameter ->
            String vmParameterString = vmParameter instanceof Closure ? vmParameter.call() : vmParameter.toString()
            builder.append("${vmParameterString} ")
        }
        builder.size() > 0 ? builder.toString() : null
    }

}
