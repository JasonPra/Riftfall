package org.Azkerash.riftfall.confidence;

import java.util.*;

/** Bounded per-observer memory. Stores identifiers, never references to other entities. */
public final class WitnessedDeathMemory {
    public record Incident(UUID victim,String victimType,UUID killer,String killerType,long expiresAt) {}
    private final Deque<Incident> incidents=new ArrayDeque<>();
    public void remember(long now,WitnessSettings settings,UUID victim,String victimType,UUID killer,String killerType) {
        active(now,settings.maxStacks());
        if (settings.durationTicks()==0 || settings.maxStacks()==0 || incidents.stream().anyMatch(i->i.victim().equals(victim))) return;
        incidents.addLast(new Incident(victim,victimType,killer,killerType,now+settings.durationTicks()));
        active(now,settings.maxStacks());
    }
    public List<Incident> active(long now,int cap) {
        if (cap<0) throw new IllegalArgumentException("Negative stack cap");
        incidents.removeIf(i->i.expiresAt()<=now);
        while (incidents.size()>cap) incidents.removeFirst();
        return List.copyOf(incidents);
    }
}
