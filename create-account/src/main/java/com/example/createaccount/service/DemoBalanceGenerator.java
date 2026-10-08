package com.example.createaccount.service;

import java.math.BigDecimal;

/**
 * Assigns the demo-only starting balance shown to a customer after sign-in. No real funding
 * happens in this POC - see {@link RandomDemoBalanceGenerator}.
 */
public interface DemoBalanceGenerator {

    BigDecimal generateInitialBalance();
}
