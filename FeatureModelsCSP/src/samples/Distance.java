/**
 *
 */
package samples;

import static choco.Choco.*;
import choco.kernel.common.util.tools.StringUtils;
//import choco.kernel.common.util.tools.ChocoUtil;
//import choco.common.util.ChocoUtil;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;

/**
 *
 * compute euclidean distance between to points.
 * see https://sourceforge.net/forum/message.php?msg_id=5164540
 * @author Arnaud Malapert
 *
 */
public class Distance {


	public final IntegerVariable x1,x2,y1,y2,d;

	public Distance(IntegerVariable x1, IntegerVariable y1,
			IntegerVariable x2, IntegerVariable y2, IntegerVariable d) {
		super();
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.d = d;
	}

	public IntegerExpressionVariable power2(IntegerExpressionVariable v) {
		return mult(v,v);
	}

	public Model createModel(boolean ceil) {
		Model m =new CPModel();
		IntegerExpressionVariable a  = power2( minus(x1, x2) );
		IntegerExpressionVariable b = power2(minus(y1, y2));
		IntegerExpressionVariable ub = power2(d);
		//add constraint : (d-1)^2 +1 <= a + b <= d^2
		if(ceil) {
			// ceil(d) = sqrt( a +b )
			IntegerExpressionVariable lb = plus( power2(minus(d,1)), 1);
			m.addConstraint(geq( plus(a,b), lb));
			m.addConstraint(leq( plus(a,b), ub));
		}else {
			// d = sqrt( a +b )
			m.addConstraint(eq( plus(a,b), ub));
		}
		return m;
	}

	public void solve(boolean ceil) {
		Model m = createModel(ceil);
		Solver s = new CPSolver();
		s.read(m);
		s.maximize(s.getVar(d), false);
		System.out.println(s.solutionToString());
	}

	public static void main(String[] args) {
		IntegerVariable[] x = makeIntVarArray("x", 2, -4, 3, "cp:bound");
		IntegerVariable[] y = makeIntVarArray("y", 2, 2, 10, "cp:bound");
		IntegerVariable d = makeIntVar("dist", 0, 20, "cp:bound");
		Distance dist =new Distance(x[0], y[0], x[1], y[1],d);
		
		System.out.println("maximize distantce between "+StringUtils.pretty(x[0],y[0],x[1],y[1]));
		
		System.out.println("maximal distance = 10,63");
		System.out.println(" d = sqrt( (x0-x1)^2 + (y0-y1)^2 )");
		dist.solve(false);
		System.out.println(" d' =  ceil (d)");
		dist.solve(true);
	}
}
