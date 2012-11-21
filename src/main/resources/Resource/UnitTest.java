import org.junit.Assert;
import org.junit.Test;

public class ${ClassName}Test {
${<foreach Examples e}
	@Test
	public void Example${e.Num}()
	{
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
		${e.Output.Param.Type.Primitive} expected = ${e.Output};
${<else}
		${e.Output.Param.Type.Primitive} expected[] = new ${e.Output.Param.Type} {${foreach e.Output.ValueList v ,}
			${v}${end}
		};
${<end}
	
		${Method.ReturnType} result = new ${ClassName}().${Method.Name}(${foreach e.Input in , }${in.Param.Name}${end});
		
${<if Method.ReturnType.RealNumber}
		Assert.assertEquals(expected, result, 1e-9);
${<else}
${<if ReturnsArray}
		Assert.assertArrayEquals(expected, result);
${<else}
		Assert.assertEquals(expected, result);
${<end}
${<end}
	}

${<end}
}
