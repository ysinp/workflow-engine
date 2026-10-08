package com.example.createaccount.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Picks a random demo balance between {@value #MIN} and {@value #MAX}, rounded to whole
 * dollars. Purely cosmetic - makes the post-sign-in "account details" screen feel real without
 * modelling actual funding/deposits in this POC.
 */
@Component
public class RandomDemoBalanceGenerator implements DemoBalanceGenerator {

    private static final long MIN = 1_000;
    private static final long MAX = 25_000;

    @Override
    public BigDecimal generateInitialBalance() {
        long dollars = ThreadLocalRandom.current().nextLong(MIN, MAX + 1);
        return BigDecimal.valueOf(dollars).setScale(2, RoundingMode.UNNECESSARY);
    }
}
