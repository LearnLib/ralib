This folder contains models obtained by using an RaLib based learning setup to infer TCP stacks.The name
of the model identifies the operating system learned and the input alphabet used. BASE stands for a baseline alphabet 
which comprises the *connect*, *close*, *ACK+RST* *RST* and *SYN+ACK*. "_" acts as separator between terms.  Any inputs appended to the "BASE"
identify additional inputs used in learning, or inputs from the baseline not used, in case the name of the input is prepended
by "M". "MT" suggests parameter typing was used.


Each model was tested a 15000 Random Walk style tests. For logs and caches obtained, as well as other experiments run 
check the ralib/experiments directory.