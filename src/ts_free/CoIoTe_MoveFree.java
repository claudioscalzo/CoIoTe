package ts_free;

import org.coinor.opents.*;
import ts_trim.CoIoTe_Solution;

@SuppressWarnings("serial")
class CoIoTe_MoveFree implements Move {

    // ATTRIBUTES
    private int i, j, m, t;
    private int i1, m1, t1;
    private int quantity1;
    private int quantity2;

    //CONSTRUCTOR
    CoIoTe_MoveFree(int i, int j, int m, int t, int i1, int t1, int m1, int quantity1, int quantity2) {
        super();
        this.i = i;
        this.j = j;
        this.m = m;
        this.t = t;
        this.i1 = i1;
        this.t1 = t1;
        this.m1 = m1;

        this.quantity1 = quantity1;
        this.quantity2 = quantity2;
    }

    // METHOD SWAP FREE USERS
    @Override
    public void operateOn(Solution sol) {
        CoIoTe_Solution solution = (CoIoTe_Solution)sol;

        solution.decrementCell(i, j, m, t, quantity1);
        solution.incrementCell(i1, j, m1, t1, quantity2);

        solution.decrementDataUsersCell(i1, m1, t1, quantity2);
        solution.decrementDataUsersCell(i, m, t, -quantity1);
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
        hash += i1;
        hash *= 37;
        hash += t1;
        hash *= 37;
        hash += m1;

        return hash;
    }

    // GETTERS
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

    int getQuantity1() {
        return quantity1;
    }

    int getQuantity2() {
        return quantity2;
    }


}

