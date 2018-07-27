import random
import sys
import out
from taintedstr import tstr
import json
from collections import deque


def treeQuery(prefix, suffix):
    PIV = {}
    makePIV(prefix, PIV)
    com, d = taint_run(prefix, PIV)
    PIV = {}
    if d < len(prefix.values):
        sdt = SymbolicDecisionTree()
        sdt.set_accepting(False)
        return sdt, {}
    else:
        makePIV(prefix,PIV)
        sdt, comparisons, depth = SDT(prefix, suffix, PIV, len(prefix.values))
        finish(sdt, PIV)
        removePrefixValues(PIV)
    return sdt, PIV


def makePIV(prefix, PIV):
    for i in range(0, len(prefix.values)):
        if i % 2 == 0:
            PIV[prefix.suffix[i].parameters] = int(i / 2 + 1)

def removePrefixValues(dict):
    keys = []
    for key in dict.keys():
        if key < 0:
            keys.append(key)
    for key in keys:
        dict.pop(key, None)


def finish(sdt, PIV):
    for child in sdt.children:
        for eq in child.equalities:
            eq.set_register(PIV[eq.right])
        finish(child.tree, PIV)

def writeJson(sdt, PIV, output):
    data = {'sdt': sdt.get_json_format(), 'PIV': PIV}
    with open(output, 'w') as outfile:
        json.dump(data, outfile)


def SDT(prefix, suffix, PIV, depth):
    if suffix.empty():
        c, new_depth = taint_run(prefix, PIV)
        normalize(c)
        return SymbolicDecisionTree(), c, new_depth
    else:
        s = suffix.remove()
        prefix.add_prefix(s)
        if depth%2==0:
            PIV[s.parameters] = int(depth/2+1)
        tree, constraints, new_depth = SDT(prefix, suffix, PIV, depth+1)
        if new_depth >= depth:
            if new_depth == depth:
                tree.set_accepting(False)
            cons = get_constraints(constraints, s.parameters)
            tree2 = SymbolicDecisionTree()
            tree2.add_branch(TreeEdge(cons, s, tree))
            for c in cons:
                prefix.change_prefix_value(get_value(prefix, c.right))
                tree, constraints, new_depth = SDT(prefix, suffix, PIV, depth+1)
                tree2.add_branch(TreeEdge(get_constraints(constraints, s.parameters), s, tree))
            suffix.append(s)
            prefix.remove()
            return tree2, constraints, new_depth
        suffix.append(s)
        prefix.remove()
        return tree, constraints, new_depth

def taint_inputs(taints, actions):
    tainted_inputs = []
    for i in range(0, len(taints)):
        tainted_inputs.append(tstr(actions[i][1], [taints[i]]*len(actions[i][1])))
    return tainted_inputs

def get_inputs(prefix):
    values = prefix.values
    suffix = prefix.suffix
    input_actions = []
    output_actions = []
    in_taints = []
    out_taints = []
    for i in range(0, len(values)):
        if i%2==0:
            in_taints.append(suffix[i].parameters)
            input_actions.append((suffix[i].action, values[i]))
        else:
            out_taints.append(suffix[i].parameters)
            output_actions.append((suffix[i].action, values[i]))
    return in_taints, out_taints, input_actions, output_actions

def taint_run(prefix, PIV):
    depth = len(prefix.values)
    equalities = []
    m = out.StateMachine()
    in_taints, out_taints, input_actions, output_actions = get_inputs(prefix)
    tainted_inputs = taint_inputs(in_taints, input_actions)
    for i in range(0, len(input_actions)):
        in1 = out.InputAction(input_actions[i][0], [tainted_inputs[i]])
        out1 = m.handleInput(in1)
        if i < len(output_actions):
            if out1.methodName == output_actions[i][0]:
                if out1.parameters:
                    PIV[out_taints[i]] = PIV[out1.parameters[0]._taint[0]]
                    eq = Equality(out1.parameters[0]._taint[0], out_taints[i], str(out1.parameters[0]) == output_actions[i][1])
                    if eq not in equalities:
                        equalities.append(eq)
                    if not (str(out1.parameters[0]) == output_actions[i][1]):
                        depth = i*2+1
                        break
            else:
                depth = i*2+1
                break
        # print("output:")
        # print(out1)
        # for i in out1.parameters:
        #     print("output", '\'' + i + '\'', "comes from input ", i._taint)
    # print("comparisons:")
    for j in tainted_inputs:
        for i in j.comparisons:
            taint_a = i.op_A._taint[0]
            taint_b = i.op_B._taint[0]

            # print('\'' + i.op_A + '\'', "with taint", taint_a, "is compared with", '\'' + i.op_B + '\'', "with taint", taint_b, "using", i.op_name, "as comparison method")
            eq = Equality(taint_a, taint_b, str(i.op_A) == str(i.op_B))
            if eq not in equalities:
                equalities.append(eq)
    # print("current inputs:")
    # for j in tainted_inputs:
    #         print("input", '\'' + j + '\'', "with taint", j._taint[0])
    # for eq in equalities:
    #     print(eq.left, eq.right, eq.equality)
    return equalities, depth

def normalize(constraints):
    switch_positions(constraints)
    constraints.sort(key=lambda constraint: constraint.left, reverse=True)
    to_normal(constraints)


def to_normal(constraints):
    i = 0
    while i < len(constraints):
        c1 = constraints[i]
        if c1.equality:
            if (c1.left == c1.right):
                del constraints[i]
                continue
            j = i + 1
            while j < len(constraints):
                c2 = constraints[j]
                if not c2.equality:
                    pass
                elif c1.right == c2.right and c1.left == c2.left:
                    del constraints[j]
                    continue
                elif c1.left == c2.left:
                    if (push_lower(constraints, i, j, c1.right, c2.right)):
                        break
                    continue
                elif c1.right == c2.right:
                    if (push_lower(constraints, i, j, c1.left, c2.left)):
                        break
                    continue
                j += 1
        i += 1

def push_lower(constraints, i, j, first, second):
    if first > second:
        reverse_insort(constraints, Equality(first, second, True))
        del constraints[i]
        i -= 1
        return True
    else:
        reverse_insort(constraints, Equality(second, first, True))
        del constraints[j]
        return False

def switch_positions(constraints):
    for i in range (0, len(constraints)):
        c = constraints[i]
        if c.right > c.left:
            constraints[i] = Equality(c.right, c.left, c.equality)

def get_value(prefix, parameter):
    for i in range (0, len(prefix.suffix)):
        if prefix.suffix[i].parameters == parameter:
            return prefix.values[i]

def get_constraints(constraints, value):
    sub_constraints = []
    for cons in constraints:
        if(cons.left == value):
            sub_constraints.append(cons)
    return sub_constraints

class ConcretePrefix:
    def __init__(self):
        self.values = []  # list of concrete prefix values
        self.suffix = []  # list of corresponding suffixes

    def add_prefix(self, suffix, value = None):
        if not value:
            self.values.append(self.new_value())
        else:
            self.values.append(str(value))
        self.suffix.append(suffix)

    def new_value(self):
        i = str(random.randint(0, sys.maxsize))
        while i in self.values:
            i = str(random.randint(0, sys.maxsize))
        return i

    def change_prefix_value(self, value):
        self.values[-1] = value

    def get_last(self):
        return self.suffix[-1]

    def get_value(self):
        return self.values[-1]

    def remove(self):
        del self.values[-1]
        del self.suffix[-1]


class SymbolicSuffix:
    def __init__(self, suffix):
        self.suffix = suffix  # list of symbolic suffixes.

    def remove(self):
        return self.suffix.popleft()

    def append(self, s):
        self.suffix.appendleft(s)

    def empty(self):
        return not self.suffix

    def get_suffix(self):
        return self.suffix

class SingleSuffix:
    def __init__(self, action, parameters):
        self.action = action  # The action that can be taken from a particular state.
        self.parameters = parameters  # The symbolic parameters of the action.

    def get_json_format(self):
        return {'action': self.action, 'parameters': self.parameters}


class Equality:

    def __init__(self, left, right, equality):
        self.left = left  # Left side of the equality
        self.right = right  # Right side of the equality
        self.equality = equality # Whether the the comparision is equals or non-equal

    def __eq__(self, other):
        return (self.left == other.left and self.right == other.right or self.left == other.right and self.right == other.left) and self.equality == other.equality

    def __lt__(self, other):
        return self.left < other.left

    def __gt__(self, other):
        return self.left > other.left

    def set_register(self, reg):
        self.right = reg


    def get_json_format(self):
        return {'parameter': self.left, 'register': self.right, 'equality': self.equality}


class SymbolicDecisionTree:
    def __init__(self):
        self.children = []
        self.accepting = True

    def set_accepting(self, accepting):
        self.accepting = accepting

    def add_branch(self, edge):
        self.children.append(edge)

    def get_json_format(self):
        data = []
        for child in self.children:
            data.append(child.get_json_format())
        return {'children': data, 'accepting': self.accepting}



class TreeEdge:
    def __init__(self, equalities, suffix, tree):
        self.equalities = equalities
        self.suffix = suffix
        self.tree = tree

    def get_json_format(self):
        eq = []
        for equality in self.equalities:
            eq.append(equality.get_json_format())
        return {'guards': eq, 'suffix': self.suffix.get_json_format(),
                'tree': self.tree.get_json_format()}

def reverse_insort(a, x, lo=0, hi=None):
    """Insert item x in list a, and keep it reverse-sorted assuming a
    is reverse-sorted.

    If x is already in a, insert it to the right of the rightmost x.

    Optional args lo (default 0) and hi (default len(a)) bound the
    slice of a to be searched.
    """
    if lo < 0:
        raise ValueError('lo must be non-negative')
    if hi is None:
        hi = len(a)
    while lo < hi:
        mid = (lo+hi)//2
        if x > a[mid]: hi = mid
        else: lo = mid+1
    a.insert(lo, x)


def readJson(input):
    with open(input) as json_file:
        i = -1000
        data = json.load(json_file)
        prefix = ConcretePrefix()
        suffix = []
        if 'prefix' in data.keys():
            for p in data['prefix']:
                prefix.add_prefix(SingleSuffix(p['symbol'], i), p['concreteParameter'])
                i += 1
        if 'suffix' in data.keys():
            for s in data['suffix']:
                suffix.append(SingleSuffix(s['symbol'], s['symbolicParameter']))
    return prefix, suffix



if __name__ in ['__main__']:
#taintrun test
    # u = ConcretePrefix()
    # for i in range (0, 10):
    #     u.add_prefix(SingleSuffix("IIN", i))
    # taint_run(u)
# normalize test
#     equalities = []
#     equalities.append(Equality(6, 7, True))
#     equalities.append(Equality(7, 8, False))
#     equalities.append(Equality(0, 1, True))
#     equalities.append(Equality(3, 4, True))
#     # equalities.append(Equality(7, 3, True))
#     # equalities.append(Equality(4, 4, True))
#     # equalities.append(Equality(5, 3, True))
#     print("space")
#     for eq in equalities:
#         print(eq.left, eq.right, eq.equality)
#     normalize(equalities)
#     print("space")
#     for eq in equalities:
#         print(eq.left, eq.right, eq.equality)

#SDT test
    # s = []
    # s.append(SingleSuffix("OOK", -1))
    # s.append(SingleSuffix("IIN", 4))
    # prefix = ConcretePrefix()
    # prefix.add_prefix(SingleSuffix("IIN", 0))
    # prefix.add_prefix(SingleSuffix("OOK", -1))
    # prefix.add_prefix(SingleSuffix("IIN", 1))
    # prefix.add_prefix(SingleSuffix("OOK", -1))
    # prefix.add_prefix(SingleSuffix("IIN", 2))
    # prefix.add_prefix(SingleSuffix("OOK", -1))
    # prefix.add_prefix(SingleSuffix("IIN", 3))
    # prefix.add_prefix(SingleSuffix("OOK", -1))
    # suffix = SymbolicSuffix(s)
    input = sys.argv[1]
    output = sys.argv[2]
    prefix, suffix = readJson(input)
    sdt, PIV = treeQuery(prefix, SymbolicSuffix(deque(suffix)))
    writeJson(sdt, PIV, output)
    print("done")


