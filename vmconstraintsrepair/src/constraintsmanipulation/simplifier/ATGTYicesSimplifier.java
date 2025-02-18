package constraintsmanipulation.simplifier;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import constraintsmanipulation.model.Configuration;
import tgtlib.definitions.expression.Expression;
import tgtlib.definitions.expression.parser.ExpressionParser;
import tgtlib.definitions.expression.visitors.IDExprCollector;

/**
 * ATGT Fast with Yices
 *
 * @author Marco
 */
public class ATGTYicesSimplifier extends Simplifier {

	private static ATGTYicesSimplifier instance;
	
	private ATGTYicesSimplifier() {}
	
	public static ATGTYicesSimplifier getInstance() {return instance==null ? instance=new ATGTYicesSimplifier() : instance;}
	
	@Override
	public String getName() {
		return "ATGT";
	}
	

	@Override
	public tgtlib.definitions.expression.Expression simplify(tgtlib.definitions.expression.Expression e) {
		try {
			String s = e.toString();
			try {
				Process process = Runtime.getRuntime().exec("java -jar atgt_yices.jar "+timeout, null, new File("."));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
				writer.write(s, 0, s.length());
				writer.newLine();
				try {writer.close();} catch (Exception ex) {}
				
				// wait for completion
				if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
					process.destroyForcibly();
					hasTimedOut=true;
					return e;
				}
				s = readOutput(process, s);
				if (s.equals("")) {
					s = e.toString();
					hasTimedOut=true;
					System.err.println("ATGT error empty string");
				} else {			
					hasTimedOut = (s.charAt(0)=='1');
					s = s.substring(2);
				}
			} catch (Exception ex) {ex.printStackTrace();}
			Expression res = ExpressionParser.parseAsBooleanExpression(s, Configuration.idc);
			if (IDExprCollector.getIdsAsList(res).size()>IDExprCollector.getIdsAsList(e).size()) return e;
			return res;
		} catch (Exception ex) {ex.printStackTrace(); return null;}
	}
	
	static String readOutput(final Process process, String s) {
		try {
			BufferedInputStream in = new BufferedInputStream(process.getInputStream());
			byte[] bytes = new byte[4096];
			StringBuilder sb = new StringBuilder();
			while (in.read(bytes) != -1) {
				sb.append(new String(bytes, StandardCharsets.UTF_8));
			}
			s = sb.toString().replace("\0","");
			s = s.substring(s.lastIndexOf("\n")+1);
			s = s.replace("not ", "!").replace(" and ", " && ").replace(" or ", " || ");
		} catch (Exception e) {e.printStackTrace();}
		return s;
	}
}
