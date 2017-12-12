RALib
=========================

RALib is a library for active learning algorithms for register automata
(a form of extended finite state machines). RALib is licensed under
the [*Apache License, Version 2.0*][4]. 

RALib is developed as an extension to [*LearnLib*][3]. It implements 
the SL* algorithm presented in 	Sofia Cassel, Falk Howar, Bengt Jonsson, 
Bernhard Steffen: Learning Extended Finite State Machines. SEFM 2014: 250-264.


Branch Information and Structure
-------------------------

This is an experimental branch of RaLib used to learn TCP stacks. Consequently, 
stability should not be expected. This adaptation supports theories of equality, 
inequality and sums over (one or two) constants. Handling these theories
allowed us to use RaLib to learn TCP client implementations.

The branch includes:
* the RaLib source code (src)
* models learned (results)
* a folder containing detailed experimental data, such as the input configuration, 
  the statistics, cache, logs and models generated following each experiment (experiments)
* various input configurations used mostly to learn TCP-like models (inputs) 

Installation
-------------------------

RaLib uses the [*jConstraints-z3*][1] library as an abstraction layer for 
interfacing (some) constraint solvers. While basic functionality of 
RaLib can be used without *jConstraints-z3*, the parent library
[*jConstraints*][5] is required for compilation. 
*jConstraints* and *jConstraints-z3* are open source software and are
licensed under the [*Apache License, Version 2.0*][4]. 


Using RALib
-------------------------


RALib can be used as a library from Java. The test cases that come with RALib
demonstrate how this can be done. RALib currently also provides two tools
that can be run from the shell. A 'simulator' for inferring RA models from 
simulated systems (automata) and a 'class analyzer' for inferring RA models
of Java classes. Running

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

Currently, *equality theories* can be used with the (default) integrated
constraint solver. *Inequality theories* only work in combination with 
[*jConstraints*][1]. (Configuration option ```solver=z3```


[1]: https://bitbucket.org/psycopaths/jConstraints-z3
[2]: https://z3.codeplex.com
[3]: http://www.learnlib.de
[4]: http://www.apache.org/licenses/LICENSE-2.0
[5]: https://bitbucket.org/psycopaths/jConstraints