

# https://stackoverflow.com/questions/26813545/how-to-structure-a-python-module-to-limit-exported-symbols
# https://stackoverflow.com/questions/44834/can-someone-explain-all-in-python

__all__ = ['InputAction','OutputAction','StateMachine']


import sys
import collections

# check if object is a string (works for both python 2 and 3)
# https://stackoverflow.com/questions/4843173/how-to-check-if-type-of-a-variable-is-string
def isstring(obj):
    return isinstance(obj, str if sys.version_info[0] >= 3 else basestring)

# check if object is sequence where we regard string objects not as sequence objects
# https://stackoverflow.com/questions/2937114/python-check-if-an-object-is-a-sequence
def issequence(obj):
    if isstring(obj):
        return False
    return isinstance(obj, collections.Sequence)

# immutable class
# src: https://stackoverflow.com/questions/22544610/how-to-make-class-immutable-in-python
#      => prefer readonly property above only getters, because immutable object should act as simple readonly data object
#
#     https://hynek.me/articles/hashes-and-equality/
#     https://stackoverflow.com/questions/2909106/whats-a-correct-and-good-way-to-implement-hash
class _Action(object):

    def __init__(self,methodName,  parameters):
        if self.__class__.__name__ == "_Action":
            raise NotImplementedError('_Action is abstract class : use subclasses of _Action!')
        if isstring(methodName):
            self._methodName=methodName
        else:
            raise AttributeError('name must be given as a string')
        if issequence(parameters):
            self._parameters=tuple(parameters)
        else:
            raise AttributeError('parameters must be given as a sequence')

    @property
    def methodName(self):
        return self._methodName

    @property
    def parameters(self):
        return self._parameters

    def __repr__(self):
        return self.__class__.__name__ + "(" + repr(self._methodName) + ',' + repr(self._parameters) + ')'

    def __key(self):
        return (self._methodName, self._parameters )

    def __eq__(self, other):
        return self.__class__ == other.__class__ and self.__key() == other.__key()

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.__key())

class InputAction(_Action):

    def __init__(self,methodName,  parameters):
        super(InputAction,self).__init__(methodName, parameters)


class OutputAction(_Action):

    def __init__(self,methodName,  parameters):
        super(OutputAction,self).__init__(methodName, parameters)



class _State(object):

    def __init__(self,id):
        self.id=id

    def handleInput(self,  inputAction ):
        pass

    def getStateId(self):
        return self.id

def enum(*args):
    enums = dict(zip(args, range(len(args))))
    return type('Enum', (), enums)





# https://www.pythoncentral.io/how-to-implement-an-enum-in-python/
# enum for StateID
_StateID = enum(
    'id1',
    'id3',
    )


# jinja template : http://jinja.pocoo.org/docs/2.10/templates/

# Initialize constants




class _State_id1(_State):

    def __init__(self):
        self.id=_StateID.id1

    def handleInput(self, statemachine, inputAction ):
        # copy statevars into locals
        
        a = statemachine.a
        b = statemachine.b


        # helpers for output
        outputMethodName = "Oquiescence"
        result = []

        methodName = inputAction.methodName
        
        if  methodName == "IIn":
        
            True # dummy code line in case no input transitions defined; to prevent empty if body!!
            tomte_p0= inputAction.parameters[0]
            
            # update action  (for input transition)
            b=tomte_p0

            # loop over output transitions for selected input transition
            
            if ( a==b ) :
            
                # update action  (for output transition)

                # change state
                statemachine._currentState= statemachine._state_id3


                # setup output name and params
                outputMethodName = "OOK"
            
            elif ( a!=b ) :
            
                # update action  (for output transition)

                # change state
                statemachine._currentState= statemachine._state_id3


                # setup output name and params
                outputMethodName = "OOut"
                result.append(b)
            
            
             

        

        # copy local updated statevars back into statemachine
        
        statemachine.a = a
        statemachine.b = b

        return OutputAction(outputMethodName, result)


class _State_id3(_State):

    def __init__(self):
        self.id=_StateID.id3

    def handleInput(self, statemachine, inputAction ):
        # copy statevars into locals
        
        a = statemachine.a
        b = statemachine.b


        # helpers for output
        outputMethodName = "Oquiescence"
        result = []

        methodName = inputAction.methodName
        
        if  methodName == "IIn":
        
            True # dummy code line in case no input transitions defined; to prevent empty if body!!
            tomte_p0= inputAction.parameters[0]
            
            # update action  (for input transition)
            a=tomte_p0

            # loop over output transitions for selected input transition
            
            # update action  (for output transition)

            # change state
            statemachine._currentState= statemachine._state_id1


            # setup output name and params
            outputMethodName = "OOut"
            result.append(a)
            
            
             

        

        # copy local updated statevars back into statemachine
        
        statemachine.a = a
        statemachine.b = b

        return OutputAction(outputMethodName, result)


class StateMachine(object):
    # Initialize states (state objects)
    # note: done once in class for multiple instance of statemachine
    _state_id1 = _State_id1()
    _state_id3 = _State_id3()
    

    #constructor: initializes by calling reset
    def __init__(self):
        self.reset()

    def handleInput(self, inputAction):
        # copy input
        ia =  InputAction(inputAction.methodName, inputAction.parameters )
        return self._currentState.handleInput(self,ia)


    def reset(self):
        # initial values statevars
        self.a = 0
        self.b = 0
        # start statee
        self._currentState = self._state_id3

    def getStateId(self):
        return self._currentState.getStateId()

    def getStateIds(self):
        return tuple(range(len(['id1', 'id3'])))

    def getStateVars(self):
        d={}
        d['a'] = self.a
        d['b'] = self.b
        return d

    def getInputAlphabet(self):
        return {'IIn': ['int']}

    def getOutputAlphabet(self):
        return {'OOut': ['int'], 'OOK': []}

    def getConstants(self):
        d={}
        return d
