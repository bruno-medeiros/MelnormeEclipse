Developers Guide
================

See project page ( MMRNMHRM_PAGE ) for user information.

#### Setting up the development environment:
 * Clone the Git repository.
 * In Eclipse, use the "import existing projects" wizard, navigate to the Git repository, and add all the Eclipse projects that are present in the root directory of the Git repo. Java Compiler settings should automatically be configured, since each project has project-specific settings stored in source control.
 * Setup the target platform: Open the target platform file: `releng/target-platform/IDE.target` and set it as your target platform.

 
#### Running the tests in Eclipse:

#### Automated Building and Testing:
Using Maven (and Tycho), it is possible to automatically build the IDE, create an update site, and run all the tests. Download [Maven](http://maven.apache.org/) (minimum version 3.0), and run the following commands on the root folder of the repository:
 * Run `mvn package` to build the IDE feature into a p2 repository (which is a local update site). It will be placed at `bin-maven/features.repository/repository`
 * Run `mvn integration-test` to build the IDE as above and also run the test suites. You can do `mvn integration-test -P TestsLiteMode` to run the test suites in "Lite Mode" (skip certain long-running tests).

#### Deploying a new release:


## Project design info and notes

#### Old source history:

#### About `src-lang/` and `melnorme.lang` code:
The `melnorme.lang` code, or simply Lang code, is IDE functionality not specific to any language, designed to potentially be used by other language IDEs. To achieve this some constraints need to be observed:
 * Lang code can only depend on other `melnorme` code, or on Eclipse.org plugins (including DLTK). But not on IDE specific code.  The only exception to this are the `_Actual` classes, which contain bindings to IDE-specific code (such as ids or other IDE constants, or even methods)
 * Lang code should be place on its own source folder (`src-lang/` usually). This is to make it easier to compare and update the code with the `src-lang/` of another IDE. If the Lang code is identical, only the `_Actual` classes should show up as differences.

Why not re-use Lang code across IDEs by placing it in its own plugin? For two reasons. There are several points where Lang code needs to be connected/bound to certain IDE specific code. So, if Lang code is compiled into plugins shared by IDEs then this binding can only be done at runtime, as opposed to compile-time (This approach is similar to what DLTK does). Second, and perhaps more importantly, sharing at the source level allows unfettered freedom to customize the code. Sharing at a binary level requires that an API be exposed, and sometimes makes it difficult to extend/change functionality that the API didn't foresee changing. (From experience, this has happened a few times when using DLTK).

#### Unit tests double-method wrapper:
 
What is this code idiom seen so often in Junit tests? :
```java
@Test
public void testXXX() throws Exception { testXXX$(); }
public void testXXX$() throws Exception {
```
This is donely solely as an aid when debugging code, so that the "Drop to frame" functionality can be used on the unit-test method. It seems the Eclipse debugger cannot drop-to-frame to a method that is invoked dynamically (such as the unit-test method). So we wrap the unit-test method on another one. So while we now cannot drop-to-frame in `testXXX`, we can do it in `testXXX$`, which basically allows us to restart the unit-test.

TODO: investigate if there is an alternate way to achieve the same. I haven't actually checked that.