package com.example.cryptofun.data;

import androidx.annotation.NonNull;

public class StrategyResultV2 {

    int passedMACD;
    int passedWT;
    int passedPPO;
    int passedADX;
    int passedAROON;
    int passedEMA;
    int passedRSI;
    int passedICHIMOKU;

    public StrategyResultV2(int passedMACD, int passedWT, int passedPPO, int passedADX, int passedAROON, int passedEMA, int passedRSI, int passedICHIMOKU) {
        this.passedMACD = passedMACD;
        this.passedWT = passedWT;
        this.passedPPO = passedPPO;
        this.passedADX = passedADX;
        this.passedAROON = passedAROON;
        this.passedEMA = passedEMA;
        this.passedRSI = passedRSI;
        this.passedICHIMOKU = passedICHIMOKU;
    }

    public int getPassedICHIMOKU() {
        return passedICHIMOKU;
    }

    public void setPassedICHIMOKU(int passedICHIMOKU) {
        this.passedICHIMOKU = passedICHIMOKU;
    }

    public int getPassedMACD() {
        return passedMACD;
    }

    public void setPassedMACD(int passedMACD) {
        this.passedMACD = passedMACD;
    }

    public int getPassedWT() {
        return passedWT;
    }

    public void setPassedWT(int passedWT) {
        this.passedWT = passedWT;
    }

    public int getPassedPPO() {
        return passedPPO;
    }

    public void setPassedPPO(int passedPPO) {
        this.passedPPO = passedPPO;
    }

    public int getPassedADX() {
        return passedADX;
    }

    public void setPassedADX(int passedADX) {
        this.passedADX = passedADX;
    }

    public int getPassedAROON() {
        return passedAROON;
    }

    public void setPassedAROON(int passedAROON) {
        this.passedAROON = passedAROON;
    }

    public int getPassedEMA() {
        return passedEMA;
    }

    public void setPassedEMA(int passedEMA) {
        this.passedEMA = passedEMA;
    }

    public int getPassedRSI() {
        return passedRSI;
    }

    public void setPassedRSI(int passedRSI) {
        this.passedRSI = passedRSI;
    }

    @NonNull
    @Override
    public String toString() {

        return "StrategyResultV2{passedMACD=" + passedMACD + " passedWT=" + passedWT + " passedPPO=" + passedPPO + " passedADX=" + passedADX + " passedAROON=" + passedAROON + " passedEMA=" + passedEMA
                + " passedRSI=" + passedRSI + '}';

    }
}
