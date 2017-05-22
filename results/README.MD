This folder contains models obtained by using an RaLib based learning setup to infer TCP stacks. **_** acts as separator between the terms
in the model's name. The name of each model identifies the operating system learned, the input alphabet used and additional info about the model. **BASE** stands for a baseline alphabet 
which comprises the **connect**, **close**, **ACK+RST**, **RST** and **SYN+ACK**. 


Any inputs appended to **BASE** identify additional inputs used in learning, or inputs from the baseline not used, 
in case the name of the input is prepended by **M**. **MT** suggests parameter typing was used. **OLD** suggests the model is superseeded by a new (not **OLD**)  model.


Each model was tested a 15000 Random Walk style tests. For logs and caches obtained, as well as other experiments run 
check the ralib/experiments directory here [link](https://bitbucket.org/learnlib/ralib/src/14976849ecbc1dec75538afcc6948e057a1467a1/experiments/?at=succ-expr). 


For each model we include the .dot file and we hope to make available a more readable .pdf version.
