# TEST CODE FOR PYTHON {{{
import sys, time , math

def tc_equal(expected, received):
    try:
        _t = type(expected)
        received = _t(received)
        if _t == list or _t == tuple:
            if len(expected) != len(received): return False
            return all(tc_equal(e, r) for (e, r) in zip(expected, received))
        elif _t == float:
            eps = 1e-9
            d = abs(received - expected)
            return not math.isnan(received) and not math.isnan(expected) and d <= eps * max(1.0, abs(expected))
        else:
            return expected == received
    except:
        return False

def pretty_str(x):
    if type(x) == str:
        return '"%s"' % x
    elif type(x) == tuple:
        return '(%s)' % (','.join( (pretty_str(y) for y in x) ) )
    else:
        return str(x)
        
def do_test(${Method.Params}, __expected, caseNo):
    sys.stdout.write("  Testcase #%d ... " % caseNo)

${<if RecordRuntime}
    startTime = time.time()
${<end}
    instance = ${ClassName}()
    exception = None
    try:
        __result = instance.${Method.Name}(${foreach Method.Params par , }${par.Name}${end});
    except:
        import traceback
        exception = traceback.format_exc()
${<if RecordRuntime}
    elapsed = time.time() - startTime   # in sec
${<end}

    if exception is not None:
        sys.stdout.write("RUNTIME ERROR: \\n")
        sys.stdout.write(exception + "\\n")
        return 0

    if tc_equal(__expected, __result):
        sys.stdout.write("PASSED! " ${if RecordRuntime}+ ("(%.3f seconds)" % elapsed)${end} + "\\n")
        return 1
    else:
        sys.stdout.write("FAILED! " ${if RecordRuntime}+ ("(%.3f seconds)" % elapsed)${end} + "\\n")
        sys.stdout.write("           Expected: " + pretty_str(__expected) + "\\n")
        sys.stdout.write("           Received: " + pretty_str(__result) + "\\n")
        return 0

def run_testcase(__no):
${<foreach Examples e}
    if __no == ${e.Num}:
${<foreach e.Input in}
${<if !in.Param.Type.Array}
        ${in.Param.Name} = ${in}
${<else}
        ${in.Param.Name} = (${foreach in.ValueList v ,}
            ${v}${end}
        )
${<end}
${<end}
${<if !e.Output.Param.Type.Array}
        __expected = ${e.Output}
${<else}
        __expected = (${foreach e.Output.ValueList v ,}
            ${v}${end}
        )
${<end}

        # run the code
        return do_test(${foreach e.Input in , }${in.Param.Name}${end}, __expected, __no)

${<end}
    # Your custom testcase goes here
    # if __no == ${NumOfExamples} ...
    #     pass

def run_tests():
    sys.stdout.write("${Problem.Name} (${Problem.Score} Points)\\n\\n")

    nPassed = nAll = 0
    if len(sys.argv) <= 1:
        for i in range(${NumOfExamples}):
            nAll += 1
            nPassed += run_testcase(i)
    else:
        for arg in sys.argv[1:]:
            nAll += 1
            nPassed += run_testcase(int(arg))

    sys.stdout.write("\\nPassed : %d / %d cases\\n" % (nPassed, nAll))

${<if RecordScore}
    T = time.time() - ${CreateTime}
    PT, TT = (T / 60.0, 75.0)
    points = ${Problem.Score} * (0.3 + (0.7 * TT * TT) / (10.0 * PT * PT + TT * TT))
    sys.stdout.write("Time   : %d minutes %d secs\\n" % (int(T/60), T%60))
    sys.stdout.write("Score  : %.2f points\\n" % points)
${<end}

if __name__ == '__main__':
    run_tests()

# }}}

