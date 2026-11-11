package org.Azkerash.riftfall.loot;
import java.util.*;
import org.junit.jupiter.api.Test;
import yesman.epicfight.world.capabilities.item.*;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import static org.junit.jupiter.api.Assertions.*;
class GoblinEquipmentPolicyTest {
    @Test void polearmsAndHeavyWeaponsStayForbiddenRegardlessOfStats(){
        for(var c:List.of(WeaponCategories.GREATSWORD,WeaponCategories.LONGSWORD,WeaponCategories.SPEAR,WeaponCategories.TRIDENT,WeaponCategories.TACHI))assertTrue(GoblinEquipmentPolicy.forbiddenCategory(c));
        assertFalse(GoblinEquipmentPolicy.forbiddenCategory(WeaponCategories.SWORD));assertFalse(GoblinEquipmentPolicy.forbiddenCategory(WeaponCategories.AXE));
    }
    @Test void addonCategoriesInheritRestrictionsAndCannotHideBehindANewName(){
        WeaponCategory addon=new WeaponCategory(){public int universalOrdinal(){return 10000;}public List<WeaponCategory> getParents(){return List.of(WeaponCategories.SPEAR);}public String toString(){return "fancy_stick";}};
        assertTrue(GoblinEquipmentPolicy.forbiddenCategory(addon));
        WeaponCategory balanced=new WeaponCategory(){public int universalOrdinal(){return 10001;}public String toString(){return "balanced_blade";}};
        assertFalse(GoblinEquipmentPolicy.forbiddenCategory(balanced));
    }
    @Test void equipmentRankingPrioritizesCombatValueThenEnchantmentsThenCondition(){
        assertTrue(new EquipmentRank(8,0,.2).compareTo(new EquipmentRank(5,10,1))>0);
        assertTrue(new EquipmentRank(5,2,.5).compareTo(new EquipmentRank(5,0,1))>0);
        assertTrue(new EquipmentRank(5,2,.8).compareTo(new EquipmentRank(5,2,.5))>0);
        assertEquals(0,new EquipmentRank(5,2,.8).compareTo(new EquipmentRank(5,2,.8)));
    }
}
