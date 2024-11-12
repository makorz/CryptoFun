package app.makorz.cryptofun.data;

import java.util.ArrayList;
import java.util.List;

import app.makorz.cryptofun.ui.home.ListViewElement;

public class ApprovedGroupedTokens {

    private int strategy;
    private List<ListViewElement> tokens;
    private boolean isExpanded;

    public ApprovedGroupedTokens(int strategy, List<ListViewElement> tokens) {
        this.strategy = strategy;
        this.tokens = tokens;
        this.isExpanded = false;
    }

    public int getStrategy() {
        return strategy;
    }

    public void setStrategy(int strategy) {
        this.strategy = strategy;
    }

    public List<ListViewElement> getTokens() {
        return tokens;
    }

    public void setTokens(List<ListViewElement> tokens) {
        this.tokens = tokens;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

}
