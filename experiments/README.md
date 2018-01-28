Here is stored data for some of the experiments run. Each experiment is assigned a folder.
The naming convention for the folders is the same as for the models in the 'results' folder. 
Namely  **_** acts as separator between the terms in the folder's name. 

The name of each folder identifies the operating system learned, the input alphabet used and a
dditional info about the experiment.

**BASE** stands for a baseline alphabet which comprises the **connect**, **close**, **ACK+RST**, **RST** and **SYN+ACK**. 
Any inputs appended to **BASE** identify additional inputs used in learning, or inputs from the baseline not used, 
in case the name of the input is prepended by **M**. 

**MT** suggests parameter typing was used. **OLD** suggests the experiment is superseeded by a new (not **OLD**)  experiment.

Each experiment folder may contain:
* config_.* - the input configuration used
* '.log' file - a log of learning
* 'model.dot' - the learned model (if learning was successful)
* '.ser' file - the cache obtained as a result of the learning experiment
