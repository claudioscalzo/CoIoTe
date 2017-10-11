package ts_trim;

import org.coinor.opents.SimpleTabuList;
import org.coinor.opents.TabuSearchEvent;
import org.coinor.opents.TabuSearchListener;

@SuppressWarnings("serial")
public class CoIoTe_SearchListener implements TabuSearchListener {

    private int unimprovingCounter;
    private int improvingCounter;
    private int disimprovingCounter;
    private int max;

    public CoIoTe_SearchListener(int max) {
        this.unimprovingCounter = 0;
        this.improvingCounter = 0;
        this.disimprovingCounter = 0;
        this.max = max;
    }

    @Override
    public void tabuSearchStarted(TabuSearchEvent tabuSearchEvent) {

    }

    @Override
    public void tabuSearchStopped(TabuSearchEvent tabuSearchEvent) {

    }

    @Override
    public void newBestSolutionFound(TabuSearchEvent tabuSearchEvent) {

    }

    @Override
    public void newCurrentSolutionFound(TabuSearchEvent tabuSearchEvent) {

    }

    @Override
    public void unimprovingMoveMade(TabuSearchEvent tabuSearchEvent) {
    	improvingCounter = 0;
//    	unimprovingCounter = 0;
    	disimprovingCounter++;
    	
    	if (disimprovingCounter >= 4) {
        	SimpleTabuList st = (SimpleTabuList)tabuSearchEvent.getTabuSearch().getTabuList();
        	int actualTenure = st.getTenure();
        	
        	if(actualTenure <= 20) {
        		st.setTenure(actualTenure+actualTenure/2);
        	}
        }
    }

    @Override
    public void improvingMoveMade(TabuSearchEvent tabuSearchEvent) {
        unimprovingCounter = 0;
        disimprovingCounter = 0;
        improvingCounter++;
        
        if(improvingCounter >= 16) {
        	SimpleTabuList st = (SimpleTabuList)tabuSearchEvent.getTabuSearch().getTabuList();
        	int actualTenure = st.getTenure();
        	
        	if(actualTenure >= 2) {
        		st.setTenure(actualTenure-actualTenure/2);
        	}
        		
        }
        
    }

    @Override
    public void noChangeInValueMoveMade(TabuSearchEvent tabuSearchEvent) {
    	improvingCounter = 0;
//    	disimprovingCounter = 0;
        unimprovingCounter++;
        
        if (unimprovingCounter >= max) {
            tabuSearchEvent.getTabuSearch().stopSolving();
        }
    }
}
