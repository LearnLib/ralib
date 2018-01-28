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
allowed us to use RaLib to learn TCP client implementations. For a more formal description
of the extensions, we refer to the publication: Learning-Based Testing the Sliding Window 
Behavior of TCP Implementations. FMICS 2017: 185-200. 

The branch includes the folders:

* 'src' - the RaLib source code 
* 'dist' - contains "ralib.jar", an archive generated from this project
* 'results' - the .dot register automata models learned plus some scripts 
* 'experiments - a folder containing detailed experimental data, such as the input configuration, 
  the statistics, cache, logs and models generated following each experiment
* 'inputs' - contains various input configurations including those for learning TCP-like models 
  * 'tcp' - contains input files for the actual TCP case study

Connecting to TCP stacks was done using the TCP Adapter and Entity components from 
the [*tcp-learner*][6] project.

Quick Run
-------------------------
Download the [*tcp-learner*][6] tool in a Linux VM (or your Linux Host). On the machine 
whose TCP stack you want to learn, set up the SutAdapter of the tcp-learner project. Then
follow the instructions on the tcp-learner page, other than the one starting the learner.
We copied them here:

Get the TCP Adapter (SutAdapter/socketAdapter.c) and deploy it on the system you want 
to learn (for example your host). Compile it (with any Linux/Windows 
compiler) and use:

`./socketAdapter -a addressOfNetworkAdapter -l portOfNetworkAdapter -p portUsedByTCPEntity`

This should get your TCP Adapter listening for network adapter connections, over which all
socket strings are sent along with system resets. 

The Learner side requires more tweaking. 
1. first, don't allow the OS to interfere to the communication with the TCP Entity by running from a terminal:
`sudo iptables -A OUTPUT -p --tcp-flags RST RST -j DROP`

Then copy the files from the Example directory to the tcp-learner dir. 

2. edit sutinfo.yaml with the alphabet used for learner. All possible inputs are included,
comment out those you don't need. Start off small. (e.g. with only "ACCEPT", "LISTEN" 
and "SYN(V,V,0)"). Don't combine server socket calls with client socket calls!

3. edit 'config.cfg' by setting: 
   * serverIP, serverPort to the IP/port of _TCP Entity_
   * cmdIP, cmdPort to the IP/port of  _TCP Adapter_ 
   * networkInterface (the interface over which communication with the TCP Entity is done)
   * waitime to 0.2 or 0.3 (depending on the network)

4. run the network adapter:
`sudo python Adapter/main.py --configFile config.cfg`
(the adapter should display all the parameters used and start running)

Now comes the interesting part, instead of running LearnLib, we configure and run the RALib setup. 

5. In the project root, create a new folder (say 'run') in which you copy 'ralib.jar' and the contents of 'inputs/tcp'. 'cd' to that directory.

6. Configure the parameters of 'tcp.properties' (the configuration file for the TCP adapter integrated into RALib) so that it can connect to the network adapter. 
 * localCommunicationPort 'config.cfg' of network adapter should match senderPort in 'tcp.properties'

7. Configure the input configuration file config_realtcp according to preference. You can use it as is.

8. Run the command 
` java -cp ralib.jar -jar ralib.jar sul-analyzer -f config_realtcp `

If everything was set up correctly, learning should commence.


The remainder of this file comprises content copied from the README.md of a stable version
of RaLib. 

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


[1]: https://bitbucket.org/psycopaths/jConstraints-z3
[2]: https://z3.codeplex.com
[3]: http://www.learnlib.de
[4]: http://www.apache.org/licenses/LICENSE-2.0
[5]: https://bitbucket.org/psycopaths/jConstraints
[6]: https://gitlab.science.ru.nl/pfiteraubrostean/tcp-learner/tree/cav-aec