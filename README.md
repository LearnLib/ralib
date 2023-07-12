RALib
=========================

RALib is a library for active learning algorithms for register automata
(a form of extended finite state machines). RALib is licensed under
the [*Apache License, Version 2.0*][2]. 

RALib is developed as an extension to [*LearnLib*][1]. It implements 
the SL* algorithm presented in 	Sofia Cassel, Falk Howar, Bengt Jonsson, 
Bernhard Steffen: Learning Extended Finite State Machines. SEFM 2014: 250-264.


Installation
-------------------------

RaLib uses maven as a build system. You can simply run

```sh
mvn package assembly:single
``` 
 
to build RaLib.


Using RALib
-------------------------

RALib can be used as a library from Java. The test cases that come with RALib
demonstrate how this can be done. RALib currently also provides two tools
that can be run from the shell. A 'simulator' for inferring RA models from 
simulated systems (automata) and a 'class analyzer' for inferring RA models
of Java classes. Running
```sh
java -ea -jar target/ralib-0.1-SNAPSHOT-jar-with-dependencies.jar
```
will show some help and available options to the tools.

Below we provide two example configurations.

1. For learning a model of `java.util.LinkedList` with the `class-analyzer` call:

   ```sh
   java -ea -jar target/ralib-0.1-SNAPSHOT-jar-with-dependencies.jar  class-analyzer -f config
   ```
   with the following `config` file:
   ```
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

2. For learning a model of the SIP protocol with the `iosimulator` call:
   ```sh
   java -ea -jar target ralib-0.1-SNAPSHOT-jar-with-dependencies.jar iosimulator -f config
   ```
   with the following `config` file
   ```
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


[1]: http://www.learnlib.de
[2]: http://www.apache.org/licenses/LICENSE-2.0
