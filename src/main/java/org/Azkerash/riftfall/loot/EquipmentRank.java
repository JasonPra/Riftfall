package org.Azkerash.riftfall.loot;
/** Comparable equipment utility; prefer combat value, then enchantments, then remaining durability. */
public record EquipmentRank(double value,double enchantments,double durability) implements Comparable<EquipmentRank> {
    @Override public int compareTo(EquipmentRank other){
        int v=Double.compare(value,other.value);if(v!=0)return v;
        v=Double.compare(enchantments,other.enchantments);return v!=0?v:Double.compare(durability,other.durability);
    }
}
