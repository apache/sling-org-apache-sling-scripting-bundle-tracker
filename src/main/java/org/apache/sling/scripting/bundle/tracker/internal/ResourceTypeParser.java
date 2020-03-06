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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Version;

/**
 * The {@code ResourceTypeParser} provides methods for parsing resource type strings.
 *
 * <p>The following patterns are supported:</p>
 * <ol>
 * <li><tt>a/b/c</tt> - path-based</li>
 * <li><tt>a/b/c/1.0.0</tt> - path-based, versioned</li>
 * <li><tt>a.b.c</tt> - Java package name</li>
 * <li><tt>a.b.c/1.0.0</tt> - Java package name, versioned</li>
 * <li><tt>a</tt> - flat (sub-set of path-based)</li>
 * </ol>
 */
final class ResourceTypeParser {

    private ResourceTypeParser() {
    }

    /**
     * The {@code ResourceType} class encapsulates the details about a resource type.
     */
    static class ResourceType {
        private final String type;
        private final String version;
        private final String resourceLabel;

        private ResourceType(@NotNull String type, @Nullable String version) {
            this.type = type;
            this.version = version;
            if (type.lastIndexOf('/') != -1) {
                resourceLabel = type.substring(type.lastIndexOf('/') + 1);
            } else if (type.lastIndexOf('.') != -1) {
                resourceLabel = type.substring(type.lastIndexOf('.') + 1);
            } else {
                resourceLabel = type;
            }
        }

        /**
         * Returns a resource type's label. The label is important for script selection, since it will provide the name of the main script
         * for this resource type. For more details check the Apache Sling
         * <a href="https://sling.apache.org/documentation/the-sling-engine/url-to-script-resolution.html#scripts-for-get-requests">URL to
         * Script Resolution</a> page
         *
         * @return the resource type label
         */
        @NotNull
        public String getResourceLabel() {
            return resourceLabel;
        }

        /**
         * Returns the resource type string, without any version information.
         *
         * @return the resource type string
         */
        @NotNull
        String getType() {
            return type;
        }

        /**
         * Returns the version, if available.
         *
         * @return the version, if available; {@code null} otherwise
         */
        @Nullable
        String getVersion() {
            return version;
        }
    }

    /**
     * Given a {@code resourceTypeString}, this method will extract a {@link ResourceType} object.
     * <p>The accepted patterns are:</p>
     * <ol>
     * <li><tt>a/b/c</tt> - path-based</li>
     * <li><tt>a/b/c/1.0.0</tt> - path-based, versioned</li>
     * <li><tt>a.b.c</tt> - Java package name</li>
     * <li><tt>a.b.c/1.0.0</tt> - Java package name, versioned</li>
     * <li><tt>a</tt> - flat (sub-set of path-based)</li>
     * </ol>
     *
     * @param resourceTypeString the resource type string to parse
     * @return a {@link ResourceType} object
     * @throws IllegalArgumentException if the {@code resourceTypeString} cannot be parsed
     */
    @NotNull
    static ResourceType parseResourceType(@NotNull String resourceTypeString) {
        String type = StringUtils.EMPTY;
        String version = null;
        if (StringUtils.isNotEmpty(resourceTypeString)) {
            int lastSlash = resourceTypeString.lastIndexOf('/');
            if (lastSlash != -1 && !resourceTypeString.endsWith("/")) {
                try {
                    version = Version.parseVersion(resourceTypeString.substring(lastSlash + 1)).toString();
                    type = resourceTypeString.substring(0, lastSlash);
                } catch (IllegalArgumentException e) {
                    type = resourceTypeString;
                }
            } else {
                type = resourceTypeString;
            }
        }
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException(String.format("Cannot extract a type for the resourceTypeString %s.", resourceTypeString));
        }
        return new ResourceType(type, version);
    }
}
