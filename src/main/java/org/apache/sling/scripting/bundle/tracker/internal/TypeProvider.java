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

import java.util.Objects;
import java.util.Set;

import org.apache.sling.scripting.bundle.tracker.ResourceType;
import org.osgi.framework.Bundle;

/**
 * A {@code TypeProvider} keeps an association between a versioned resource type and the bundle that provides it.
 */
public class TypeProvider {

    private final Set<ResourceType> resourceTypes;
    private final Bundle bundle;

    /**
     * Builds a {@code TypeProvider}.
     *
     * @param resourceTypes   the resource type
     * @param bundle the bundle that provides the resource type
     */
    TypeProvider(Set<ResourceType> resourceTypes, Bundle bundle) {
        this.resourceTypes = resourceTypes;
        this.bundle = bundle;
    }

    /**
     * Returns the resource type.
     *
     * @return the resource type
     */
    Set<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    /**
     * Returns the providing bundle.
     *
     * @return the providing bundle
     */
    Bundle getBundle() {
        return bundle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundle, resourceTypes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TypeProvider) {
            TypeProvider other = (TypeProvider) obj;
            return Objects.equals(bundle, other.bundle) && Objects.equals(resourceTypes, other.resourceTypes);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("TypeProvider{ resourceTypes=%s; bundle=%s }", resourceTypes, bundle.getSymbolicName());
    }
}
