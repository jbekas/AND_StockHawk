package com.redgeckotech.stockhawk.rest;

public class SymbolNotFoundException extends Exception {
    /**
     * Constructs a new {@code Exception} that includes the current stack trace.
     */
    public SymbolNotFoundException() {
        super();
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace and the
     * specified detail message.
     *
     * @param symbol
     *            the stock symbol for this exception.
     */
    public SymbolNotFoundException(String symbol) {
        super(symbol);
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace, the
     * specified detail message and the specified cause.
     *
     * @param symbol
     *            the stock symbol for this exception.
     * @param throwable
     *            the cause of this exception.
     */
    public SymbolNotFoundException(String symbol, Throwable throwable) {
        super(symbol, throwable);
    }

    /**
     * Constructs a new {@code Exception} with the current stack trace and the
     * specified cause.
     *
     * @param throwable
     *            the cause of this exception.
     */
    public SymbolNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
