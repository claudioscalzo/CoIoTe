package ts_trim;

import org.coinor.opents.Move;
import org.coinor.opents.Solution;

@SuppressWarnings("serial")
class CoIoTe_Move implements Move {

	// ATTRIBUTES
	private int i, j, m, t;
	private int i1;
	private int t1;
	private int m1;
    private int q1;
    private int q2;
	
	//CONSTRUCTOR
	CoIoTe_Move(int i, int j, int m, int t, int i1, int t1, int m1, int q1, int q2) {
		super();
		this.i = i;
		this.j = j;
		this.m = m;
		this.t = t;
		this.i1 = i1;
		this.t1 = t1;
		this.m1 = m1;
        this.q1 = q1;
        this.q2 = q2;
	}

	@Override
	public void operateOn(Solution sol) {
		CoIoTe_Solution solution = (CoIoTe_Solution)sol;

        solution.incrementCell(i, j, m, t, q1);
        solution.decrementDataUsersCell(i, m, t, q1);

        solution.incrementCell(i1, j, m1, t1, q2);
        solution.decrementDataUsersCell(i1, m1, t1, q2);

        solution.setSurplusActivities(j, 0);
	}

    int getI() {
        return i;
    }

    int getJ() {
        return j;
    }

    int getM() {
        return m;
    }

    int getT() {
        return t;
    }

    int getI1() {
        return i1;
    }

    int getT1() {
        return t1;
    }

    int getM1() {
        return m1;
    }

    int getQ1() {
        return q1;
    }

    int getQ2() {
        return q2;
    }

    @Override
    public int hashCode() {
        int hash = i;

        hash *= 37;
        hash += j;
        hash *= 37;
        hash += m;
        hash *= 37;
        hash += t;
        hash *= 37;
        hash *= 37;
        hash += i1;
        hash *= 37;
        hash += m1;
        hash *= 37;
        hash += t1;
        hash *= 37;

        return hash;
    }

}
