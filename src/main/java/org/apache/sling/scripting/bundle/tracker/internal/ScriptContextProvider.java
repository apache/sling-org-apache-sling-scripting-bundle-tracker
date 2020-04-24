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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.LazyBindings;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.apache.sling.scripting.bundle.tracker.BundledRenderUnit;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = ScriptContextProvider.class
)
public class ScriptContextProvider {

    private static final Set<String> PROTECTED_BINDINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SlingBindings.REQUEST,
            SlingBindings.RESPONSE,
            SlingBindings.READER,
            SlingBindings.RESOURCE,
            SlingBindings.RESOLVER,
            SlingBindings.OUT,
            SlingBindings.LOG,
            SlingBindings.SLING,
            ScriptEngine.FILENAME,
            BundledRenderUnit.VARIABLE
    )));

    @Reference
    private BindingsValuesProvidersByContext bvpTracker;

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Reference
    private ScriptingResourceResolverProvider scriptingResourceResolverProvider;

    ExecutableContext prepareScriptContext(SlingHttpServletRequest request, SlingHttpServletResponse response, Executable executable)
            throws IOException {
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(executable.getScriptEngineName());
        if (scriptEngine == null) {
            throw new IllegalStateException(String.format("Cannot find a script engine with name %s for executable %s.",
                    executable.getScriptEngineName(), executable.getPath()));
        }
        // prepare the SlingBindings
        Bindings bindings = new LazyBindings();
        bindings.put("properties", request.getResource().getValueMap());
        bindings.put(SlingBindings.REQUEST, request);
        bindings.put(SlingBindings.RESPONSE, response);
        bindings.put(SlingBindings.READER, request.getReader());
        bindings.put(SlingBindings.RESOURCE, request.getResource());
        bindings.put(SlingBindings.RESOLVER, request.getResource().getResourceResolver());
        bindings.put(SlingBindings.OUT, response.getWriter());
        Logger scriptLogger = LoggerFactory.getLogger(executable.getName());
        bindings.put(SlingBindings.LOG, scriptLogger);
        bindings.put(SlingBindings.SLING, new ScriptHelper(executable.getBundle().getBundleContext(), null, request, response));
        bindings.put(BundledRenderUnit.VARIABLE, executable);
        bindings.put(ScriptEngine.FILENAME, executable.getName());
        bindings.put(ScriptEngine.FILENAME.replaceAll("\\.", "_"), executable.getName());

        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, PROTECTED_BINDINGS);
        for (BindingsValuesProvider bindingsValuesProvider : bvpTracker.getBindingsValuesProviders(scriptEngine.getFactory(),
                BindingsValuesProvider.DEFAULT_CONTEXT)) {
            bindingsValuesProvider.addBindings(protectedBindings);
        }
        ScriptContext scriptContext = new BundledScriptContext();
        Map<String, LazyBindings.Supplier> slingBindingsSuppliers = new HashMap<>();
        slingBindingsSuppliers.put(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER,
                () -> scriptingResourceResolverProvider.getRequestScopedResourceResolver());
        LazyBindings slingScopeBindings = new LazyBindings(slingBindingsSuppliers);
        scriptContext.setBindings(slingScopeBindings, SlingScriptConstants.SLING_SCOPE);
        scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setWriter(response.getWriter());
        scriptContext.setErrorWriter(new LogWriter(scriptLogger));
        scriptContext.setReader(request.getReader());
        return new ExecutableContext(scriptContext, executable, scriptEngine);
    }

    static class ExecutableContext {
        private final ScriptContext scriptContext;
        private final Executable executable;
        private final ScriptEngine scriptEngine;

        private ExecutableContext(ScriptContext scriptContext, Executable executable, ScriptEngine scriptEngine) {
            this.scriptContext = scriptContext;
            this.executable = executable;
            this.scriptEngine = scriptEngine;
        }

        void eval() throws ScriptException {
            executable.eval(scriptEngine, scriptContext);
        }

        void clean() {
            Bindings engineBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            if (engineBindings != null && engineBindings.containsKey(SlingBindings.SLING)) {
                Object scriptHelper = engineBindings.get(SlingBindings.SLING);
                if (scriptHelper instanceof ScriptHelper) {
                    ((ScriptHelper) scriptHelper).cleanup();
                }
            }
            executable.releaseDependencies();
        }
    }


}
