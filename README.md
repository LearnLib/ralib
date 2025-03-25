RALib
=========================

RALib is a library for active learning algorithms for register automata
(a form of extended finite state machines). RALib is licensed under
the [*Apache License, Version 2.0*][2].

RALib is developed as an extension to [*LearnLib*][1].
It currently implements the following algorithms for learning register automata:

1. The _SLλ algorithm_
   by Simon Dierl, Paul Fiterau-Brostean, Falk Howar, Bengt Jonsson,
   Konstantinos Sagonas, and Fredrik Tåquist,
   presented in the paper [Scalable Tree-based Register Automata Learning][4],
   Tools and Algorithms for the Construction and Analysis of Systems (TACAS 2024),
    pp 87-108.

2. The _SL* algorithm_
   by Sofia Cassel, Falk Howar, Bengt Jonsson, and Bernhard Steffen,
   presented in the paper [Learning Extended Finite State Machines][3],
   Software Engineering and Formal Methods (SEFM 2014), pp 250-264.


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
[3]: https://link.springer.com/chapter/10.1007/978-3-319-10431-7_18
[4]: https://doi.org/10.1007/978-3-031-57249-4_5
