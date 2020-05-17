RALib
=========================

RALib is a library for active learning algorithms for register automata
(a form of extended finite state machines). RALib is licensed under
the [*Apache License, Version 2.0*][4]. 

RALib is developed as an extension to [*LearnLib*][3]. It implements 
the SL* algorithm presented in 	Sofia Cassel, Falk Howar, Bengt Jonsson, 
Bernhard Steffen: Learning Extended Finite State Machines. SEFM 2014: 250-264.


Using RALib
-------------------------

RALib can be used as a library from Java. 
The test cases that come with RALib demonstrate how this can be done. 
RALib provides four tools that can be run from the shell. 
These are:

 
* *ioimulator* for inferring RA models from  simulated systems (automata); 
* *class-analyzer* for inferring RA models of basic Java classes;
* *sul-analyzer* for inferring RA models of Java classes implementing a Java adaptor class;
* *socket-analyzer* for inferring RA models of systems over a TCP socket connection

Configurations for all of these tools can be 
Running:

```
#!bash
$ java -ea -jar target/ralib-0.1-SNAPSHOT-jar-with-dependencies.jar
```


will show some help and available options to the tools. Below we provide two
example configurations.

For learning a model of `java.util.LinkedList` with the class-analyzer call

```
#!bash
$ java -ea -jar target/ralib-0.1-SNAPSHOT-jar-with-dependencies.jar \
  class-analyzer -f config
```

with the following `config` file

```
#!bash
target=java.util.LinkedList
methods=push(java.lang.Object:int)boolean:boolean+\
  pop()java.lang.Object:int

logging.level=WARNING
max.time.millis=600000
use.ceopt=true
use.suffixopt=true
use.fresh=false
use.rwalk=true
export.model=true
rwalk.prob.fresh=0.8
rwalk.prob.reset=0.1
rwalk.max.depth=6
rwalk.max.runs=10000
rwalk.reset.count=false
rwalk.draw.uniform=false
teachers=int:de.learnlib.ralib.tools.theories.IntegerEqualityTheory
```

For learning a model of the SIP protocol with the simulator call

```
#!bash
$ java -ea -jar target ralib-0.1-SNAPSHOT-jar-with-dependencies.jar \
  iosimulator -f config
```

with the following `config` file

```
#!bash
target=src/test/resources/de/learnlib/ralib/automata/xml/sip.xml

logging.level=WARNING
max.time.millis=600000
use.eqtest=true
use.ceopt=true
use.suffixopt=true
use.fresh=false
use.rwalk=true
export.model=true
rwalk.prob.fresh=0.8
rwalk.prob.reset=0.1
rwalk.max.depth=100
rwalk.max.runs=10000
rwalk.reset.count=false
rwalk.draw.uniform=false
teachers=int:de.learnlib.ralib.tools.theories.IntegerEqualityTheory
```

Learning more advanced systems, or even more complex Java classes, may require *adapters*, which mediate communication between the system and RALib.
To that end, RALib provides two additional tools.

*sul-analyzer* is similar to *class-analyzer* but applied on a Java adaptor class implementing the abstract class de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL. 
This class is expected to translate RaLib concrete inputs into actual system calls, and generate concrete outputs from the results of these calls.


*socket-analyzer* assumes there is a TCP server adapter mediating communication to the system. 
It serializes RaLib's concrete inputs and sends them over TCP socket connection to a specified target address and port.
Data received is deserialized into concrete outputs.
The manner of serialization/de-serialization is inspired from Tomte[3].

Examples for using the tools are provided in the 'examples' directory.
Some examples require adding test sources to the classpath, which can be done as in the example below: 

```
#!bash
$ java -ea -cp target/ralib-0.1-SNAPSHOT-jar-with-dependencies.jar;target/ralib-0.1-SNAPSHOT-tests.jar \
de.learnlib.ralib.Main sul-analyzer -f examples\sul-analyzer\priority_queue

```


Branch Information and Structure
-------------------------

This is an experimental yet stable branch of RALib supporting the following theories:

* theories with equalities over (max 2) sum constants, with support for fresh values and free value suffix optimization
* theories with inequalities over (max 2) sum constants, with support for special fresh values, which always increase, aing to TCP's sequence numbers

Implementations of these theories are available for Integer and Double domains. 
Note that inequality theories over Integer domains are not well supported, due to complications arising from using discrete domains.
Also note that suffix optimization is deactivated for theories with inequalities since they are untested.     

The branch includes the folders:

* 'src' - the RALib source code 
* 'examples' - example configurations for RaLib's tools, some require that you include test sources in the class path



[1]: https://bitbucket.org/psycopaths/jConstraints-z3
[2]: https://z3.codeplex.com
[3]: http://www.learnlib.de
[4]: http://www.apache.org/licenses/LICENSE-2.0
[5]: https://bitbucket.org/psycopaths/jConstraints
[6]: https://gitlab.science.ru.nl/pfiteraubrostean/tcp-learner/tree/cav-aec
[7]: http://tomte.cs.ru.nl/