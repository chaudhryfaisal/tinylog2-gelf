tinylog2-gelf
============
[Initial clone from joschi/tinylog2-gelf](https://travis-ci.org/joschi/tinylog2-gelf)

`tinylog2-gelf` is a [Writer](https://tinylog.org/v2/extending/#custom-writer) implementation for
[tinylog2](https://www.tinylog.org/v2/) writing log messages to a [GELF](https://graylog2.org/gelf) compatible server like
[Graylog2](https://graylog2.org/) or [logstash](https://logstash.net/).


Configuration
-------------

The following configuration settings are supported by `tinylog2-gelf`:

* `server` (default: `localhost`)
  * The hostname of the GELF-compatible server.
* `port` (default: `12201`)
  * The port of the GELF-compatible server.
* `transport` (default: `UDP`)
  * The transport protocol to use, valid settings are `UDP` and `TCP`.
* `hostname` (default: local hostname or `localhost` as fallback)
  * The hostname of the application.
* `additionalLogEntryValues` (default: `DATE`, `LEVEL`, `RENDERED_LOG_ENTRY`)
  * Additional information for log messages, see [`LogEntryValue`](http://www.tinylog.org/v2/javadoc/org/pmw/tinylog/writers/LogEntryValue.html).
* `staticFields` (default: empty)
  * Additional static fields for the GELF messages. 

Additional configuration settings are supported by the `GelfWriter` class. Please consult the Javadoc for details.


Examples
--------

`tinylog2-gelf` can be configured using a [configuration-file](https://tinylog.org/v2/configuration/#configuration). 

Properties file example:

    writer=gelf
    writer.server=graylog2.example.com
    writer.port=12201
    writer.transport=TCP
    writer.hostname=myhostname
    writer.additionalLogEntryValues=EXCEPTION,FILE,LINE
    writer.staticFields=additionalfield1:foo,additionalfield2:bar



Maven Artifacts
---------------

This project is available on Maven Central. To add it to your project simply add the following dependencies to your
`pom.xml`:

    <dependency>
      <groupId>com.github.chaudhryfaisal</groupId>
      <artifactId>tinylog2-gelf</artifactId>
      <version>0.1.0</version>
    </dependency>


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/chaudhryfaisal/tinylog2-gelf/issues).


License
-------

Copyright (c) 2014 Jochen Schalanda

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.
