package com.example.cryptofun.ui.view;

import com.example.cryptofun.data.ApprovedToken;

import java.io.Serializable;

public class ListViewElement implements Serializable, Comparable<ListViewElement> {

    private String text;


    public ListViewElement(String text) {
        this.text = text;

    }


    public String getText() {
        return text;
    }

    public void setText(String symbol) {
        this.text = text;
    }


    @Override
    public int compareTo(ListViewElement element) {
        return this.getText().compareTo(element.getText());
    }
}
