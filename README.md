# aegis4j

Avoid the NEXT Log4Shell vulnerability!

The Java platform has accrued a number of features over the years. Some of these features are no longer commonly used,
but their existence remains a security liability, providing attackers with a diverse toolkit to leverage against
Java-based systems.

It is possible to eliminate some of this attack surface area by creating custom JVM images with
[jlink](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jlink.html), but this is not always feasible or desired.
Another option is to use the [--limit-modules](https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html) command
line parameter when running your application, but this is a relatively coarse tool that cannot be used to disable
individual features like serialization or native process execution.

A third option is aegis4j, a Java agent which patches key system classes to completely disable a number of standard
Java features:

- `jndi`: all JNDI functionality (`javax.naming.*`)
- `rmi`: all RMI functionality (`java.rmi.*`)
- `process`: all process execution functionality (`Runtime.exec()`, `ProcessBuilder`)
- `httpserver`: all use of the JDK HTTP server (`com.sun.net.httpserver.*`)
- `serialization`: all Java serialization (`ObjectInputStream`, `ObjectOutputStream`)
- `unsafe`: all use of `sun.misc.Unsafe`
- `scripting`: all JSR 223 scripting (`javax.script.*`)
- `jshell`: all use of the Java Shell API (`jdk.jshell.*`)

### Download

The aegis4j JAR is available in the [Maven Central](https://repo1.maven.org/maven2/net/gredler/aegis4j/1.0/) repository.

### Usage: Attach at Application Startup

To attach at application startup, blocking all features listed above, add the agent to your java command line:

`java -cp <classpath> -javaagent:aegis4j-1.0.jar <main-class> <arguments>`

Or, if you want to configure the specific features to block:

`java -cp <classpath> -javaagent:aegis4j-1.0.jar=block=<features> <main-class> <arguments>`

Or, if you want to use the default block list, but unblock specific features:

`java -cp <classpath> -javaagent:aegis4j-1.0.jar=unblock=<features> <main-class> <arguments>`

Feature lists should be comma-delimited (e.g. `jndi,rmi,unsafe`).

### Usage: Attach to a Running Application

To attach to a running application, blocking all features listed above, run the following command:

`java -jar aegis4j-1.0.jar <application-pid>`

Or, if you want to configure the specific features to block:

`java -jar aegis4j-1.0.jar <application-pid> block=<features>`

Or, if you want to use the default block list, but unblock specific features:

`java -jar aegis4j-1.0.jar <application-pid> unblock=<features>`

Feature lists should be comma-delimited (e.g. `jndi,rmi,unsafe`).

The application process ID, or PID, can usually be determined by running the `jps` command.

### Compatibility

The aegis4j Java agent is compatible with JDK 11 and newer.

### Building

To build aegis4j, run `gradlew build`.

### Digging Deeper

Class modifications are performed using [Javassist](https://www.javassist.org/). The specific class modifications performed are
configured in the [mods.properties](src/main/resources/net/gredler/aegis4j/mods.properties) file.

Some of the tests validate the agent against actual vulnerabilities (e.g.
[CVE-2015-7501](src/test/java/net/gredler/aegis4j/CVE_2015_7501.java),
[CVE-2019-17531](src/test/java/net/gredler/aegis4j/CVE_2019_17531.java),
[CVE-2021-44228](src/test/java/net/gredler/aegis4j/CVE_2021_44228.java)).
The tests are run with the `jdk.attach.allowAttachSelf=true` system property, so that the agent can be attached and tested
locally. Tests are also run in individual VM instances, so that the class modifications performed in one test do not affect other
tests.

Ideally aegis4j could block all reflection as well, since it's often used in exploit chains. However, reflection is used *everywhere*,
including the JDK lambda internals, Spring Boot, JUnit, and many other libraries and frameworks.

### Related Work

[log4j-jndi-be-gone](https://github.com/nccgroup/log4j-jndi-be-gone): A Java agent which patches the Log4Shell vulnerability (CVE-2021-44228).

[Log4jHotPatch](https://github.com/corretto/hotpatch-for-apache-log4j2/): A similar Java agent from the Amazon Corretto team.

[Logout4Shell](https://github.com/Cybereason/Logout4Shell): Vaccine exploit which leverages the Log4Shell vulnerability to patch the Log4Shell vulnerability.

[Logpresso log4j2-scan](https://github.com/logpresso/CVE-2021-44228-Scanner): Command line tool for scanning (and patching) JAR files for Log4Shell vulnerabilities.

[ysoserial](https://github.com/frohoff/ysoserial): A proof-of-concept tool for generating Java serialization vulnerability payloads.

[NotSoSerial](https://github.com/kantega/notsoserial): A Java agent which attempts to mitigate serialization vulnerabilities by selectively blocking serialization attempts.
