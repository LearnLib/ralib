RALib
=========================

RALib is a library for learning register automata.

Installation
-------------------------

RALib uses the [*jConstraints*][1] library as an abstraction layer for interfacing
the solver (*jConstraints* uses plugins for supporting multiple constraint solvers.
For licensing reasons no plugin is included in RALib. In order to run RALib you 
have to install a plugin for at least one constraint solver.

There exists a closed source plugin for Microsoft's [Z3][2]). For obtaining and 
installing *jConstraints-z3*, follow the instructions on the [BitBucket page][1].

Installing RALib is then as simple as just running `mvn clean install` and 
`mvn assembly:assembly` in the RALib directory. 


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
$ java -ea -jar target ralib-0.1-SNAPSHOT-jar-with-dependencies.jar \
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






[1]: https://bitbucket.org/psycopaths/jConstraints-z3
[2]: https://z3.codeplex.com