	static void runTestcase(int __case) {
		switch (__case) {
			// Your custom testcase goes here
			case -1:
				//doTest(${foreach Method.Params p , }${p.Name}${end}, expected, __case);
				break;

${<foreach Examples e}
			case ${e.Num}: {
${<foreach e.Input in}
${<if !in.Param.Type.Array}
				${in.Param.Type.Primitive} ${in.Param.Name} = ${in};
${<else}
				${in.Param.Type.Primitive} ${in.Param.Name}[] = new ${in.Param.Type} {${foreach in.ValueList v ,}
					${v}${end}
				};
${<end}
${<end}
${<if !e.Output.Param.Type.Array}
				${e.Output.Param.Type.Primitive} __excepted = ${e.Output};
${<else}
				${e.Output.Param.Type.Primitive} __excepted[] = new ${e.Output.Param.Type} {${foreach e.Output.ValueList v ,}
					${v}${end}
				};
${<end}
				doTest(${foreach e.Input in , }${in.Param.Name}${end}, __excepted, __case);
				break;
			}
${<end}
			default: break;
		}
	}

	static void doTest(${Method.Params}, ${Method.ReturnType} __expected, int caseNo) {
${<foreach Method.Params p}
${<if p.Type.String}
${<if p.Type.Array}
		for (int i = 0; i < ${p.Name}.length; i++) {
			${p.Name}[i] = new String(${p.Name}[i]);
		}
${<else}
        ${p.Name} = new String(${p.Name});
${<end}
${<end}
${<end}
${<if RecordRuntime}
		long startTime = System.currentTimeMillis();
${<end}
		Throwable exception = null;
		${ClassName} instance = new ${ClassName}();
		${Method.ReturnType} __result = ${Method.ReturnType;ZeroValue};
		try {
			__result = instance.${Method.Name}(${foreach Method.Params par , }${par.Name}${end});
		}
		catch (Throwable e) { exception = e; }
${<if RecordRuntime}
		double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
${<end}

		nAll++;
		System.err.print(String.format("  Testcase #%d ... ", caseNo));

		if (exception != null) {
			System.err.println("RUNTIME ERROR!");
			exception.printStackTrace();
		}
		else if (${if Method.ReturnType.Array}equals(__result, __expected)${else}${if Method.ReturnType.String}__expected.equals(__result)${else}${if Method.ReturnType.RealNumber}doubleEquals(__expected, __result)${else}__result == __expected${end}${end}${end}) {
			System.err.println("PASSED! "${if RecordRuntime} + String.format("(%.2f seconds)", elapsed)${end});
			nPassed++;
		}
		else {
			System.err.println("FAILED! "${if RecordRuntime} + String.format("(%.2f seconds)", elapsed)${end});
			System.err.println("           Expected: " + ${if Method.ReturnType.Array}toString(__expected)${else}__expected${end});
			System.err.println("           Received: " + ${if Method.ReturnType.Array}toString(__result)${else}__result${end});
		}
	}

	static int nExample = ${NumOfExamples};
	static int nAll = 0, nPassed = 0;

${<if Method.ReturnType.RealNumber}
	static boolean doubleEquals(double a, double b) {
	    return !Double.isNaN(a) && !Double.isNaN(b) && Math.abs(b - a) <= 1e-9 * Math.max(1.0, Math.abs(a) );
	}

${<end}
${<if ReturnsArray}
	static boolean equals(${Method.ReturnType} a, ${Method.ReturnType} b) {
		if (a.length != b.length) return false;
		for (int i = 0; i < a.length; ++i) if (${if Method.ReturnType.String}a[i] == null || b[i] == null || !a[i].equals(b[i])${else}${if Method.ReturnType.RealNumber}!doubleEquals(a[i], b[i])${else}a[i] != b[i]${end}${end}) return false;
		return true;
	}

	static String toString(${Method.ReturnType} arr) {
		StringBuffer sb = new StringBuffer();
		sb.append("[ ");
		for (int i = 0; i < arr.length; ++i) {
			if (i > 0) sb.append(", ");
			sb.append(arr[i]);
		}
		return sb.toString() + " ]";
	}
${<end}
	public static void main(String[] args){
		System.err.println("${Problem.Name} (${Problem.Score} Points)");
		System.err.println();
		if (args.length == 0)
			for (int i = 0; i < nExample; ++i) runTestcase(i);
		else
			for (int i = 0; i < args.length; ++i) runTestcase(Integer.parseInt(args[i]));
		System.err.println(String.format("%nPassed : %d/%d cases", nPassed, nAll));

${<if RecordScore}
		int T = (int)(System.currentTimeMillis() / 1000) - ${CreateTime};
		double PT = T / 60.0, TT = 75.0;
		System.err.println(String.format("Time   : %d minutes %d secs%nScore  : %.2f points", T / 60, T % 60, ${Problem.Score} * (0.3 + (0.7 * TT * TT) / (10.0 * PT * PT + TT * TT))));
${<end}
	}
