[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-org-apache-sling-scripting-bundle-tracker/master)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-scripting-bundle-tracker/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-org-apache-sling-scripting-bundle-tracker/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-org-apache-sling-scripting-bundle-tracker/job/master/test_results_analyzer/) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![scripting](https://sling.apache.org/badges/group-scripting.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/scripting.md)

Apache Sling Scripting Bundle Tracker
====

## What
The `org.apache.sling.scripting.bundle.tracker` bundle represents an add-on module that can be deployed on a Sling 10 instance or newer to
enhance the scripting resolving capabilities. Being an add-on, the bundle hooks into Sling's current mechanisms for
content-to-servlet resolution and essentially acts as a service registration broker for bundles providing scripting
capabilities (more details in the ["How"](#how) section).

## Why
Although traditionally scripts are deployed as content stored in the search paths of a Sling instance, this leaves very little
room for script evolution in a backwards compatible way. Furthermore, versioning scripts is a difficult process if the only
mechanism to do this is the `sling:resourceType` property, since consumers (content nodes or other resource types) have then to
explicitly mention the version expected to be executed.

Scripts should not be considered content, since their only purpose is to actually generate the rendering for a certain content
structure. They are not consumed by users, but rather by the Sling Engine itself and have very little meaning outside this
context. As such, scripts should be handled like code:

  1. they _provide an HTTP API_;
  2. they can evolve in a _semantical_ [1] way;
  3. they have a _developer audience_.

## How
Being built around a `BundleTrackerCustomizer` [2], the Scripting Bundle Tracker monitors the instance's bundles wired to itself and
scans the ones providing a `sling.resourceType` capability [3]. The wiring is created by placing a `Require-Capability` header in
the bundles that provide the `sling.resourceType` capability:

```
osgi.extender;filter:="(&(osgi.extender=sling.scripting)(version>=1.0.0)(!(version>=2.0.0)))"
```


A `sling.resourceType` capability has the following attributes:

  1. `sling.resourceType:String` - mandatory; defines the provided resource type; its value is a valid Java package identifier,
  though it does not need to correspond to one;
  2. `sling.resourceType.selectors:List` - optional; defines the list of selectors that this resource type can handle;
  3. `sling.resourceType.extensions:List` - optional; defines the list of extensions that this resource type can handle;
  4. `version:Version` - mandatory; defines the version of the provided `resourceType`;
  5. `extends:String` - optional; defines which resource type it extends; the version range of the extended resource type is defined in a
    `Require-Capability`.

The `BundleTrackerCustomizer` will register a Sling Servlet with the appropriate `sling.servlet` properties for each `sling.resourceType`
capability. The servlets will be registered using the bundle context of the bundle providing the `sling.resourceType` capability, making
sure to expose the different versions of a resource type as part of the registered servlet's properties. On top of this, a plain resource
type bound servlet will also be registered, which will be automatically wired to the highest version of `resourceType`. All the mentioned
service registrations are managed automatically by the `BundleTrackerCustomizer`.

### So how do I deploy my scripts?
Short answer: exactly like you deploy your code, preferably right next to it. Pack your scripts using the following conventions:

  1. create a `javax.script` folder in your bundle;
  2. each folder under `javax.script` will identify a `resourceType`; the folder name should preferably be a valid Java package identifier,
  but it does not need to refer to an existing one;
  3. inside each `resourceType` folder create a `Version` folder; this has to follow the Semantic Versioning constraints described at [1];
  4. add your scripts, using the same naming conventions that you were used to from before [4];
  5. manually define your provide and require capabilities; just kidding; add the
  [`scriptingbundle-maven-plugin`](https://github.com/apache/sling-scriptingbundle-maven-plugin) to your build section and add its required
  properties in the `maven-bundle-plugin`'s instructions (check [this](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it/tree/master/examples/org-apache-sling-scripting-examplebundle/pom.xml)
  example);
  6. `mvn clean sling:install`.

### Integration Tests

The integration tests are provided by the [`org.apache.sling.scripting.bundle.tracker.it`](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it) project. To run the integration tests execute the following command:

```
mvn clean verify -Pit
```

### Example

To play around with a Sling instance on localhost port 8080 (override with -Dhttp.port=<port>) that has the [examples](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it/tree/master/examples/) installed run:

```
mvn clean verify -Pexample
``` 

from the [`org.apache.sling.scripting.bundle.tracker.it`](https://github.com/apache/sling-org-apache-sling-scripting-bundle-tracker-it) project.

## Resources
[1] - https://semver.org/  
[2] - https://osgi.org/javadoc/r6/core/org/osgi/util/tracker/BundleTrackerCustomizer.html  
[3] - https://osgi.org/download/r6/osgi.core-6.0.0.pdf, Page 41, section 3.3.3 "Bundle Capabilities"  
[4] - https://sling.apache.org/documentation/the-sling-engine/url-to-script-resolution.html
