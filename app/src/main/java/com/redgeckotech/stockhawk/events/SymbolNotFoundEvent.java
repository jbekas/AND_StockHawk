package com.redgeckotech.stockhawk.events;

public class SymbolNotFoundEvent {

    public final String symbol;

    public SymbolNotFoundEvent(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "SymbolNotFoundEvent{" +
                "symbol='" + symbol + '\'' +
                '}';
    }
}
