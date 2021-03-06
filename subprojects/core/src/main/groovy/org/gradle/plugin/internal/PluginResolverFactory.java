/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugin.internal;

import org.gradle.api.UnknownProjectException;
import org.gradle.api.internal.DocumentationRegistry;
import org.gradle.api.internal.DomainObjectContext;
import org.gradle.api.internal.artifacts.DependencyManagementServices;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.plugins.ClassloaderBackedPluginDescriptorLocator;
import org.gradle.api.internal.plugins.PluginDescriptorLocator;
import org.gradle.api.internal.plugins.PluginRegistry;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.cache.CacheRepository;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.plugin.resolve.internal.*;

import java.util.LinkedList;
import java.util.List;

public class PluginResolverFactory {

    private final PluginRegistry pluginRegistry;
    private final Instantiator instantiator;
    private final DependencyManagementServices dependencyManagementServices;
    private final FileResolver fileResolver;
    private final DependencyMetaDataProvider dependencyMetaDataProvider;
    private final DocumentationRegistry documentationRegistry;
    private final CacheRepository cacheRepository;

    private final ProjectFinder projectFinder = new ProjectFinder() {
        public ProjectInternal getProject(String path) {
            throw new UnknownProjectException("Cannot use project dependencies in a plugin resolution definition.");
        }
    };

    public PluginResolverFactory(PluginRegistry pluginRegistry, Instantiator instantiator, DependencyManagementServices dependencyManagementServices, FileResolver fileResolver, DependencyMetaDataProvider dependencyMetaDataProvider, DocumentationRegistry documentationRegistry, CacheRepository cacheRepository) {
        this.pluginRegistry = pluginRegistry;
        this.instantiator = instantiator;
        this.dependencyManagementServices = dependencyManagementServices;
        this.fileResolver = fileResolver;
        this.dependencyMetaDataProvider = dependencyMetaDataProvider;
        this.documentationRegistry = documentationRegistry;
        this.cacheRepository = cacheRepository;
    }

    public PluginResolver createPluginResolver(ClassLoader scriptClassLoader) {
        List<PluginResolver> resolvers = new LinkedList<PluginResolver>();
        addDefaultResolvers(resolvers);
        CompositePluginResolver compositePluginResolver = new CompositePluginResolver(resolvers);

        PluginDescriptorLocator scriptClasspathPluginDescriptorLocator = new ClassloaderBackedPluginDescriptorLocator(scriptClassLoader);
        PluginResolver notAlreadyOnClasspathCheck = new NotInPluginRegistryPluginResolverCheck(compositePluginResolver, pluginRegistry, scriptClasspathPluginDescriptorLocator);

        return notAlreadyOnClasspathCheck;
    }

    private void addDefaultResolvers(List<PluginResolver> resolvers) {
        resolvers.add(new NoopPluginResolver());
        resolvers.add(new CorePluginResolver(documentationRegistry, pluginRegistry));
        // resolvers.add(jcenterGradleOfficial(instantiator, createDependencyResolutionServices(), cacheRepository));
    }

    private DependencyResolutionServices createDependencyResolutionServices() {
        return dependencyManagementServices.create(fileResolver, dependencyMetaDataProvider, projectFinder, new BasicDomainObjectContext());
    }

    private static class BasicDomainObjectContext implements DomainObjectContext {
        public String absoluteProjectPath(String name) {
            return name;
        }
    }

}
