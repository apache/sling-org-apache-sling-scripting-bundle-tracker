/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.bundle.tracker.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.compiler.source.JavaEscapeHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = BundledScriptFinder.class
)
public class BundledScriptFinder {

    private static final String NS_JAVAX_SCRIPT_CAPABILITY = "javax.script";
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final Set<String> DEFAULT_METHODS = new HashSet<>(Arrays.asList("GET", "HEAD"));

    @Reference
    private ScriptEngineManager scriptEngineManager;

    Executable getScript(SlingHttpServletRequest request, LinkedHashSet<TypeProvider> typeProviders, boolean precompiledScripts) {
        List<String> scriptMatches;
        for (TypeProvider provider : typeProviders) {
            scriptMatches = buildScriptMatches(request, provider.getType());
            for (String match : scriptMatches) {
                for (String extension : getScriptEngineExtensions()) {
                    URL bundledScriptURL;
                    if (precompiledScripts) {
                        String className = JavaEscapeHelper.makeJavaPackage(match);
                        try {
                            Class clazz = provider.getBundle().loadClass(className);
                            return new PrecompiledScript(provider.getBundle(), scriptEngineManager.getEngineByExtension(extension),
                                    clazz.getDeclaredConstructor().newInstance());
                        } catch (ClassNotFoundException e) {
                            // do nothing here
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot correctly instantiate class " + className + ".");
                        }
                    } else {
                        bundledScriptURL = provider.getBundle().getEntry(NS_JAVAX_SCRIPT_CAPABILITY + SLASH + match + DOT + extension);
                        if (bundledScriptURL != null) {
                            return new Script(provider.getBundle(), bundledScriptURL, scriptEngineManager.getEngineByExtension(extension));
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<String> buildScriptMatches(SlingHttpServletRequest request, String providerType) {
        List<String> matches = new ArrayList<>();
        String resourceType = providerType;
        String version = null;
        String method = request.getMethod();
        boolean defaultMethod = DEFAULT_METHODS.contains(method);
        if (resourceType.contains(SLASH) && StringUtils.countMatches(resourceType, SLASH) == 1) {
            version = resourceType.substring(resourceType.indexOf(SLASH) + 1);
            resourceType = resourceType.substring(0, resourceType.length() - version.length() - 1);
        }
        String extension = request.getRequestPathInfo().getExtension();
        String[] selectors = request.getRequestPathInfo().getSelectors();
        if (selectors.length > 0) {
            for (int i = selectors.length - 1; i >= 0; i--) {
                String scriptForMethod = resourceType + (StringUtils.isNotEmpty(version) ? SLASH + version + SLASH : SLASH) +
                        method + DOT + String.join(SLASH, Arrays.copyOf(selectors, i + 1));
                String scriptNoMethod = resourceType + (StringUtils.isNotEmpty(version) ? SLASH + version + SLASH : SLASH) + String.join
                        (SLASH, Arrays.copyOf(selectors, i + 1));
                if (StringUtils.isNotEmpty(extension)) {
                    if (defaultMethod) {
                        matches.add(scriptNoMethod + DOT + extension);
                    }
                    matches.add(scriptForMethod + DOT + extension);
                }
                if (defaultMethod) {
                    matches.add(scriptNoMethod);
                }
                matches.add(scriptForMethod);
            }
        }
        String scriptForMethod = resourceType + (StringUtils.isNotEmpty(version) ? SLASH + version + SLASH : SLASH) + method;
        String scriptNoMethod = resourceType + (StringUtils.isNotEmpty(version) ? SLASH + version + SLASH : SLASH) + resourceType
                .substring(resourceType.lastIndexOf(DOT) + 1);
        if (StringUtils.isNotEmpty(extension)) {
            if (defaultMethod) {
                matches.add(scriptNoMethod + DOT + extension);
            }
            matches.add(scriptForMethod + DOT + extension);

        }
        if (defaultMethod) {
            matches.add(scriptNoMethod);
        }
        matches.add(scriptForMethod);
        return Collections.unmodifiableList(matches);
    }

    private List<String> getScriptEngineExtensions() {
        List<String> _scriptEngineExtensions = new ArrayList<>();
        for (ScriptEngineFactory factory : scriptEngineManager.getEngineFactories()) {
            _scriptEngineExtensions.addAll(factory.getExtensions());
        }
        Collections.reverse(_scriptEngineExtensions);
        return Collections.unmodifiableList(_scriptEngineExtensions);
    }
}
