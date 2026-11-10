package org.Azkerash.riftfall.mana;
/** Lazily allocated mob-owned state. MagicData is the actual pool, not a mirrored counter. */
public final class ManaState {
    public final ManaBudget budget=new ManaBudget();
    public boolean initialized;
    public double configuredBase=Double.NaN;
    public String lastExpense="none";
}
